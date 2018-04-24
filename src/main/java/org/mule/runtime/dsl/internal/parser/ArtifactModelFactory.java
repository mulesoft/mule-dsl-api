/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.internal.parser;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.dsl.internal.parser.ParameterModelsProvider.fromParameterGroupModel;
import static org.mule.runtime.dsl.internal.parser.ParameterModelsProvider.fromParameterizedModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.artifact.semantic.Artifact;
import org.mule.runtime.api.artifact.semantic.Chain;
import org.mule.runtime.api.artifact.semantic.ComplexParameterValue;
import org.mule.runtime.api.artifact.semantic.Component;
import org.mule.runtime.api.artifact.semantic.Configuration;
import org.mule.runtime.api.artifact.semantic.ConnectionProvider;
import org.mule.runtime.api.artifact.semantic.Construct;
import org.mule.runtime.api.artifact.semantic.Object;
import org.mule.runtime.api.artifact.semantic.Operation;
import org.mule.runtime.api.artifact.semantic.Parameter;
import org.mule.runtime.api.artifact.semantic.Route;
import org.mule.runtime.api.artifact.semantic.SimpleParameterValue;
import org.mule.runtime.api.artifact.semantic.Source;
import org.mule.runtime.api.artifact.semantic.SourceResponse;
import org.mule.runtime.api.artifact.semantic.Value;
import org.mule.runtime.api.artifact.sintax.ArtifactDefinition;
import org.mule.runtime.api.artifact.sintax.ComponentDefinition;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.dsl.internal.parser.xml.ExtensionsHelper;
import org.mule.runtime.extension.api.dsl.syntax.DslSyntaxUtils;
import org.mule.runtime.internal.dsl.DslConstants;

import com.google.common.collect.Streams;

public class ArtifactModelFactory {

  private final Set<ExtensionModel> extensionsModels;
  private final ExtensionsHelper extensionsHelper;

  public ArtifactModelFactory(Set<ExtensionModel> extensionModels) {
    this.extensionsModels = extensionModels;
    this.extensionsHelper = new ExtensionsHelper(extensionModels);
  }

  public Artifact createFrom(ArtifactDefinition artifactDefinition) {
    Artifact.ArtifactBuilder artifactBuilder = Artifact.builder().withArtifactDefinition(Optional.of(artifactDefinition));
    return artifactBuilder.withGlobalComponents(createGlobalComponents(artifactDefinition)).build();
  }

  private List<Component> createGlobalComponents(ArtifactDefinition artifactDefinition) {
    return artifactDefinition.getGlobalDefinitions().stream()
        .map(componentDefinition -> createComponent(componentDefinition, empty())).collect(Collectors.toList());
  }

  private Component createComponent(ComponentDefinition componentDefinition, Optional<ConstructModel> constructModel) {
    java.lang.Object model = extensionsHelper.findModel(componentDefinition.getIdentifier());
    if (model == null && constructModel.isPresent()) {
      model = extensionsHelper.findNestedComponentWithinModel(componentDefinition.getIdentifier(), constructModel.get());
    }
    if (model instanceof OperationModel) {
      return createOperation(componentDefinition, (OperationModel) model);
    } else if (model instanceof SourceModel) {
      return createSource(componentDefinition, (SourceModel) model);
    } else if (model instanceof ConstructModel) {
      return createConstruct(componentDefinition, (ConstructModel) model);
    } else if (model instanceof ConfigurationModel) {
      return createConfiguration(componentDefinition, (ConfigurationModel) model);
    } else if (model instanceof ObjectType) {
      return createObject(componentDefinition, (ObjectType) model);
    } else if (model instanceof NestedRouteModel) {
      return createRoute(componentDefinition, (NestedRouteModel) model);
    } else if (model instanceof NestedChainModel) {
      return createChain(componentDefinition, (NestedChainModel) model);
    } else if (model instanceof NestedComponentModel) {
      // TODO proper error handling
      throw new RuntimeException("this should never happen I guess");
    }
    // TODO improve
    throw new RuntimeException();
  }

  private Component createChain(ComponentDefinition componentDefinition, NestedChainModel chainModel) {
    return Chain.builder()
        .withModel(chainModel)
        .withComponentDefinition(componentDefinition)
        .withProcessorComponents(extractProcessorComponents(componentDefinition))
        .build();
  }

  private Component createRoute(ComponentDefinition componentDefinition, NestedRouteModel model) {
    return Route.builder()
        .withComponentDefinition(componentDefinition)
        .withParameters(extractParameters(componentDefinition, fromParameterizedModel(model)))
        .withModel(model)
        .withProcessorComponents(extractProcessorComponents(componentDefinition))
        .build();
  }

  private List<Component> extractProcessorComponents(ComponentDefinition componentDefinition) {
    // TODO filter those that are parameters
    return componentDefinition.getChildComponentDefinitions().stream()
        .map(childComponentDefinition -> createComponent(childComponentDefinition, empty()))
        .collect(Collectors.toList());
  }

  private Object createObject(ComponentDefinition componentDefinition, ObjectType model) {
    return Object.builder()
        .withComponentDefinition(componentDefinition)
        .withObjectType(model)
        .build();
  }

  private ConnectionProvider createConnectionProvider(ComponentDefinition componentDefinition,
                                                      ConfigurationModel configurationModel) {
    final Reference<ConnectionProviderModel> foundModel = new Reference<>();
    return componentDefinition.getChildComponentDefinitions()
        .stream()
        .map(childComponentDefinition -> {
          Optional<ConnectionProviderModel> connectionProviderOptional =
              extensionsHelper.findConnectionProvider(childComponentDefinition.getIdentifier(), configurationModel);
          if (connectionProviderOptional.isPresent()) {
            foundModel.set(connectionProviderOptional.get());
            return childComponentDefinition;
          }
          return null;
        })
        .filter(value -> value != null)
        .findAny()
        .map(connectionProviderDefinition -> createConnectionProvider(connectionProviderDefinition, foundModel.get()))
        .orElse(null);
  }

  private ConnectionProvider createConnectionProvider(ComponentDefinition componentDefinition, ConnectionProviderModel model) {
    return ConnectionProvider.builder()
        .withModel(model)
        .withComponentDefinition(componentDefinition)
        .withParameters(extractParameters(componentDefinition, fromParameterizedModel(model)))
        .build();
  }

  private Component createConfiguration(ComponentDefinition componentDefinition, ConfigurationModel model) {
    return Configuration.builder()
        .withModel(model)
        .withComponentDefinition(componentDefinition)
        .withParameters(extractParameters(componentDefinition, fromParameterizedModel(model)))
        .withConnectionProvider(createConnectionProvider(componentDefinition, model))
        .build();
  }

  private Construct createConstruct(ComponentDefinition componentDefinition, ConstructModel model) {
    Construct.ConstructBuilder constructBuilder = Construct.builder()
        .withParameters(extractParameters(componentDefinition, fromParameterizedModel(model)))
        .withModel(model)
        .withComponentDefinition(componentDefinition);

    constructBuilder.withProcessorComponents(componentDefinition.getChildComponentDefinitions()
        .stream() // add predicate to filter childs that are parameters
        .map(childComponentDefinition -> createComponent(childComponentDefinition, of(model)))
        .collect(Collectors.toList()));

    return constructBuilder
        .build();
  }

  private Source createSource(ComponentDefinition componentDefinition, SourceModel model) {
    return Source.builder()
        .withModel(model)
        .withComponentDefinition(componentDefinition)
        .withParameters(extractParameters(componentDefinition, fromParameterizedModel(model)))
        .withSuccessResponse(extractSuccessResponse(componentDefinition, model))
        .withErrorResponse(extractErrorResponse(componentDefinition, model))
        .build();
  }

  private SourceResponse extractSuccessResponse(ComponentDefinition componentDefinition, SourceModel model) {
    Optional<SourceCallbackModel> successCallbackOptional = model.getSuccessCallback();
    return extractResponse(componentDefinition, successCallbackOptional);
  }

  private SourceResponse extractErrorResponse(ComponentDefinition componentDefinition, SourceModel model) {
    Optional<SourceCallbackModel> errorCallbackOptional = model.getErrorCallback();
    return extractResponse(componentDefinition, errorCallbackOptional);
  }

  private SourceResponse extractResponse(ComponentDefinition componentDefinition,
                                         Optional<SourceCallbackModel> responseCallbackOptional) {
    if (responseCallbackOptional.isPresent()) {
      SourceCallbackModel sourceCallbackModel = responseCallbackOptional.get();
      Preconditions.checkState(sourceCallbackModel.getParameterGroupModels().size() == 1,
                               "All parameters should be under the same group.");
      ParameterGroupModel parameterGroupModel = sourceCallbackModel.getParameterGroupModels().get(0);
      // TODO fix to use proper comparison
      String asInConfigName = DslSyntaxUtils.getSanitizedElementName(parameterGroupModel);
      return componentDefinition.getChildComponentDefinitions().stream()
          .filter(childComponentDefinition -> childComponentDefinition.getIdentifier().getName().equalsIgnoreCase(asInConfigName))
          .map(childComponentDefinition -> {
            List<Parameter> parameters =
                extractParameters(childComponentDefinition, fromParameterGroupModel(parameterGroupModel));
            return SourceResponse.builder()
                .withComponentDefinition(componentDefinition)
                .withModel(sourceCallbackModel)
                .withParameters(parameters)
                .build();
          })
          .findAny()
          .orElse(null);

    }
    return null;
  }

  private Operation createOperation(ComponentDefinition componentDefinition, OperationModel model) {
    return Operation.builder()
        .withComponentDefinition(componentDefinition)
        .withOperationModel(model)
        .withParameters(extractParameters(componentDefinition, fromParameterizedModel(model)))
        .build();
  }

  private List<Parameter> extractParameters(ComponentDefinition componentDefinition,
                                            ParameterModelsProvider parameterModelsProvider) {
    // TODO work with an indexed extension model
    final Map<String, String> processedParameters = new HashMap<>();

    // TODO missing parameters that do not exists in ext. model as the name parameter
    Stream<Parameter> simpleParametersStream = componentDefinition.getParameterDefinitions().stream()
        .map(parameterDefinition -> extensionsHelper
            .findParameterModel(parameterModelsProvider,
                                parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier())
            .map(parameterModel -> {
              processedParameters.put(parameterModel.getName(), parameterModel.getName());
              return Parameter.builder()
                  .withModel(parameterModel)
                  .withParameterDefinition(parameterDefinition)
                  .withValue(SimpleParameterValue.builder()
                      .withParameterValueDefinition(parameterDefinition.getParameterValueDefinition())
                      .build())
                  .build();
            }))
        .filter(optional -> optional.isPresent())
        .map(optional -> optional.get());

    Stream<Parameter> complexParametersStream = componentDefinition.getChildComponentDefinitions().stream()
        .map(childComponentDefinition -> {
          Optional<ParameterModel> parameterModelOptional = parameterModelsProvider.getAllParameterModels().stream()
              .filter(parameterModel -> {
                if (isContentParameter(parameterModel)) {
                  return parameterModel.getName().equals(childComponentDefinition.getIdentifier().getName());
                } else {
                  IsObjectTypeMetadataTypeVisitor objectTypeMetadataTypeVisitor = new IsObjectTypeMetadataTypeVisitor();
                  parameterModel.getType().accept(objectTypeMetadataTypeVisitor);
                  return objectTypeMetadataTypeVisitor.isObjectType()
                      && parameterModel.getName().equals(childComponentDefinition.getIdentifier().getName());
                }
              })
              .findAny();

          return parameterModelOptional.map(parameterModel -> {
            Preconditions.checkState(childComponentDefinition.getChildComponentDefinitions().size() <= 1,
                                     "Only one child maximum should be available at this point");
            if (isContentParameter(parameterModel)) {
              return Parameter.builder()
                  .withModel(parameterModel)
                  .withValue(ComplexParameterValue.builder()
                      .withComponentDefinition(childComponentDefinition)
                      .withComponent(Value.builder() // TODO this is weird.
                          .withParameterValueDefinition(childComponentDefinition.getParameterValueDefinition().get())
                          .build())
                      .build())
                  .build();

            } else if (!childComponentDefinition.getChildComponentDefinitions().isEmpty()) {
              // TODO extended type. This syntax is particular of XML. This must be solved by DSL.
              ComponentDefinition specificImplementionChildDefinition =
                  childComponentDefinition.getChildComponentDefinitions().get(0);
              Component component = createComponent(specificImplementionChildDefinition, empty());
              return Parameter.builder()
                  .withModel(parameterModel)
                  .withParameterDefinition(null) // TODO see what to put here in this case
                  .withValue(ComplexParameterValue.builder()
                      .withComponent(component) // TODO review if the value should be over the parameter wrapper or the paramerter
                      // itself.
                      .withComponentDefinition(childComponentDefinition)
                      .build())
                  .build();
            } else {
              throw new RuntimeException();
            }

          }).orElse(null);
        })
        .filter(value -> value != null);// TODO this seems weird. probably I can get rid of it.

    List<Parameter> parameters =
        Streams.concat(simpleParametersStream, complexParametersStream).collect(Collectors.toList());

    // TODO do not add if already exists
    componentDefinition.getParameterDefinitions().stream()
        .filter(parameterDefinition -> parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier()
            .equals(ComponentIdentifier.builder().namespace(DslConstants.CORE_PREFIX).name("name").build()))
        .findAny()
        .ifPresent(parameterDefinition -> {
          parameters.add(Parameter.builder()
              .withParameterDefinition(parameterDefinition)
              .withValue(SimpleParameterValue.builder()
                  .withParameterValueDefinition(parameterDefinition.getParameterValueDefinition())
                  .build())
              .build());
        });

    return parameters;
  }

  private boolean isContentParameter(ParameterModel parameterModel) {
    return parameterModel.getRole().equals(ParameterRole.CONTENT)
        || parameterModel.getRole().equals(ParameterRole.PRIMARY_CONTENT);
  }


}
