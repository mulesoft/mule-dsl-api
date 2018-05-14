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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.artifact.ast.ArtifactAst;
import org.mule.runtime.api.artifact.ast.ChainAst;
import org.mule.runtime.api.artifact.ast.ComplexParameterValueAst;
import org.mule.runtime.api.artifact.ast.ComponentAst;
import org.mule.runtime.api.artifact.ast.ConfigurationAst;
import org.mule.runtime.api.artifact.ast.ConnectionProviderAst;
import org.mule.runtime.api.artifact.ast.ConstructAst;
import org.mule.runtime.api.artifact.ast.InternalDslOperationAst;
import org.mule.runtime.api.artifact.ast.ObjectAst;
import org.mule.runtime.api.artifact.ast.OperationAst;
import org.mule.runtime.api.artifact.ast.ParameterAst;
import org.mule.runtime.api.artifact.ast.ParameterComponentAst;
import org.mule.runtime.api.artifact.ast.ParameterIdentifierAst;
import org.mule.runtime.api.artifact.ast.RouteAst;
import org.mule.runtime.api.artifact.ast.SimpleParameterValueAst;
import org.mule.runtime.api.artifact.ast.SourceAst;
import org.mule.runtime.api.artifact.ast.SourceResponseAst;
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
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.dsl.api.config.ConfigResource;
import org.mule.runtime.dsl.internal.parser.xml.ExtensionsHelper;
import org.mule.runtime.extension.api.dsl.syntax.DslSyntaxUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

//TODO review if this factory exists per DSL or can be the same one for all of the DSLs.
public class XmlArtifactModelFactory {

  private final Set<ExtensionModel> extensionsModels;
  private final ExtensionsHelper extensionsHelper;

  public XmlArtifactModelFactory(Set<ExtensionModel> extensionModels) {
    this.extensionsModels = extensionModels;
    this.extensionsHelper = new ExtensionsHelper(extensionModels);
  }

  // TODO remove configFiles and disableXmlValidations
  public ArtifactAst createFrom(ArtifactDefinition artifactDefinition, Set<URL> configFiles, boolean disableXmlValidations,
                                ConfigResource[] configResources) {
    return ArtifactAst.builder()
        .withConfigFiles(configFiles)
        .withDisableXmlValidations(disableXmlValidations)
        .withConfigResources(configResources)
        .withArtifactType(artifactDefinition.getRootDefinitions().get(0).getIdentifier().getName())
        .withParameters(createRootParameters(artifactDefinition))
        .withGlobalComponents(createGlobalComponents(artifactDefinition))
        .build();
  }

  private List<ParameterAst> createRootParameters(ArtifactDefinition artifactDefinition) {
    List<ParameterAst> parameters = new ArrayList<>();
    artifactDefinition.getRootDefinitions().stream()
        .forEach(componentDefinition -> {
          componentDefinition.getParameterDefinitions().stream()
              .forEach(parameterDefinition -> {
                parameters.add(ParameterAst.builder()
                    .withParameterIdentifier(ParameterIdentifierAst.builder()
                        .withSourceCodeLocation(parameterDefinition.getParameterIdentifierDefinition().getSourceCodeLocation())
                        .withIdentifier(parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier())
                        .build())
                    .withSourceCodeLocation(parameterDefinition.getSourceCodeLocation())
                    .withValue(SimpleParameterValueAst.builder()
                        .withRawValue(parameterDefinition.getParameterValueDefinition().getRawValue())
                        .withSourceCodeLocation(parameterDefinition.getParameterValueDefinition().getSourceCodeLocation())
                        .build())
                    .build());
              });
        });
    return parameters;
  }

  private List<ComponentAst> createGlobalComponents(ArtifactDefinition artifactDefinition) {
    List<ComponentAst> globalComponents = new ArrayList<>();
    artifactDefinition.getRootDefinitions().stream()
        .map(ComponentDefinition::getChildComponentDefinitions)
        .forEach(componentDefinitions -> componentDefinitions.stream()
            .forEach(componentDefinition -> globalComponents.add(createComponent(componentDefinition, empty()))));
    return globalComponents;
  }

  private ComponentAst createComponent(ComponentDefinition componentDefinition, Optional<ConstructModel> constructModel) {
    // TODO change to double dispatcher
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
    } else if (componentDefinition.getIdentifier().getNamespace().equalsIgnoreCase("tns")) {
      return createDslModuleInternalOperation(componentDefinition);
    }
    // TODO improve
    throw new RuntimeException("Could not createComponent from " + componentDefinition.getIdentifier());
  }

  private InternalDslOperationAst createDslModuleInternalOperation(ComponentDefinition componentDefinition) {
    return InternalDslOperationAst.builder()
        .withComponentIdentifier(componentDefinition.getIdentifier())
        .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
        .withParameters(createInternalDslOperationParameters(componentDefinition))
        .build();
  }

  private List<ParameterAst> createInternalDslOperationParameters(ComponentDefinition componentDefinition) {
    return Streams.concat(componentDefinition
        .getParameterDefinitions()
        .stream()
        .map(parameterDefinition -> ParameterAst.builder()
            .withSourceCodeLocation(parameterDefinition.getSourceCodeLocation())
            .withParameterIdentifier(ParameterIdentifierAst.builder()
                .withIdentifier(parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier())
                .withSourceCodeLocation(parameterDefinition.getParameterIdentifierDefinition().getSourceCodeLocation())
                .build())
            .withValue(SimpleParameterValueAst.builder()
                .withSourceCodeLocation(parameterDefinition.getParameterValueDefinition().getSourceCodeLocation())
                .withRawValue(parameterDefinition.getParameterValueDefinition().getRawValue())
                .build())
            .build()),
                          componentDefinition.getChildComponentDefinitions().stream()
                              .map(nestedComponentDefinition -> ParameterAst.builder()
                                  .withValue(SimpleParameterValueAst.builder()
                                      .withRawValue(nestedComponentDefinition.getParameterValueDefinition().get().getRawValue())
                                      .withSourceCodeLocation(nestedComponentDefinition.getParameterValueDefinition().get()
                                          .getSourceCodeLocation())
                                      .build())
                                  .withParameterIdentifier(ParameterIdentifierAst.builder()
                                      .withIdentifier(nestedComponentDefinition.getIdentifier())
                                      .withSourceCodeLocation(nestedComponentDefinition.getSourceCodeLocation()).build())
                                  .withSourceCodeLocation(nestedComponentDefinition.getSourceCodeLocation())
                                  .build()))
        .collect(Collectors.toList());
  }

  private ComponentAst createChain(ComponentDefinition componentDefinition, NestedChainModel chainModel) {
    return ChainAst.builder()
        .withModel(chainModel)
        .withComponentIdentifier(componentDefinition.getIdentifier()) // TODO for now we use the same but we need to normalize
        .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
        .withProcessorComponents(extractProcessorComponents(componentDefinition))
        .build();
  }

  private ComponentAst createRoute(ComponentDefinition componentDefinition, NestedRouteModel model) {
    return RouteAst.builder()
        .withComponentIdentifier(componentDefinition.getIdentifier()) // TODO for now we use the same but we need to normalize
        .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
        .withParameters(createParameters(componentDefinition, model))
        .withModel(model)
        .withProcessorComponents(extractProcessorComponents(componentDefinition))
        .build();
  }

  private List<ComponentAst> extractProcessorComponents(ComponentDefinition componentDefinition) {
    // TODO filter those that are parameters
    return componentDefinition.getChildComponentDefinitions().stream()
        .map(childComponentDefinition -> createComponent(childComponentDefinition, empty()))
        .collect(Collectors.toList());
  }

  private ObjectAst createObject(ComponentDefinition componentDefinition, ObjectType model) {
    return ObjectAst.builder()
        .withComponentIdentifier(componentDefinition.getIdentifier()) // TODO for now we use the same but we need to normalize
        .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
        .withObjectType(model)
        .build();
  }

  private ConnectionProviderAst createConnectionProvider(ComponentDefinition componentDefinition,
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

  private ConnectionProviderAst createConnectionProvider(ComponentDefinition componentDefinition, ConnectionProviderModel model) {
    return ConnectionProviderAst.builder()
        .withModel(model)
        .withComponentIdentifier(componentDefinition.getIdentifier()) // TODO for now we use the same but we need to normalize
        .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
        .withParameters(createParameters(componentDefinition, model))
        .build();
  }

  private ComponentAst createConfiguration(ComponentDefinition componentDefinition, ConfigurationModel model) {
    return ConfigurationAst.builder()
        .withModel(model)
        .withComponentIdentifier(componentDefinition.getIdentifier()) // TODO for now we use the same but we need to normalize
        .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
        .withParameters(createParameters(componentDefinition, model))
        .withConnectionProvider(createConnectionProvider(componentDefinition, model))
        .build();
  }

  private ConstructAst createConstruct(ComponentDefinition componentDefinition, ConstructModel model) {
    ConstructAst.ConstructAstBuilder constructBuilder = ConstructAst.builder()
        .withParameters(createParameters(componentDefinition, model))
        .withComponentIdentifier(componentDefinition.getIdentifier()) // TODO for now we use the same but we need to normalize
        .withModel(model)
        .withSourceCodeLocation(componentDefinition.getSourceCodeLocation());

    constructBuilder.withProcessorComponents(componentDefinition.getChildComponentDefinitions()
        .stream()
        .filter(childComponentDefinition -> !isParameterElement(childComponentDefinition.getIdentifier(), model))
        .map(childComponentDefinition -> createComponent(childComponentDefinition, of(model)))
        .collect(Collectors.toList()));

    return constructBuilder
        .build();
  }

  private boolean isParameterElement(ComponentIdentifier parameterIdentifier, ParameterizedModel parameterizedModel) {
    return extensionsHelper.findParameterModel(fromParameterizedModel(parameterizedModel), parameterIdentifier).isPresent()
        || extensionsHelper
            .findParameterGroup(ParameterGroupModelsProvider.fromParameterizedModel(parameterizedModel), parameterIdentifier)
            .isPresent();
  }

  private SourceAst createSource(ComponentDefinition componentDefinition, SourceModel model) {
    return SourceAst.builder()
        .withModel(model)
        .withComponentIdentifier(componentDefinition.getIdentifier()) // TODO for now we use the same but we need to normalize
        .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
        .withParameters(createParameters(componentDefinition, model))
        .withSuccessResponse(extractSuccessResponse(componentDefinition, model))
        .withErrorResponse(extractErrorResponse(componentDefinition, model))
        .build();
  }

  private SourceResponseAst extractSuccessResponse(ComponentDefinition componentDefinition, SourceModel model) {
    Optional<SourceCallbackModel> successCallbackOptional = model.getSuccessCallback();
    return extractResponse(componentDefinition, successCallbackOptional);
  }

  private SourceResponseAst extractErrorResponse(ComponentDefinition componentDefinition, SourceModel model) {
    Optional<SourceCallbackModel> errorCallbackOptional = model.getErrorCallback();
    return extractResponse(componentDefinition, errorCallbackOptional);
  }

  private SourceResponseAst extractResponse(ComponentDefinition componentDefinition,
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
            List<ParameterAst> parameters =
                createParameters(childComponentDefinition, responseCallbackOptional.get());
            return SourceResponseAst.builder()
                .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
                .withModel(sourceCallbackModel)
                .withParameters(parameters)
                .build();
          })
          .findAny()
          .orElse(null);

    }
    return null;
  }

  private OperationAst createOperation(ComponentDefinition componentDefinition, OperationModel model) {
    return OperationAst.builder()
        .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
        .withOperationModel(model)
        .withComponentIdentifier(componentDefinition.getIdentifier()) // TODO for now we use the same but we need to normalize
        .withParameters(createParameters(componentDefinition, model))
        .build();
  }

  private List<ParameterAst> createParameters(ComponentDefinition componentDefinition,
                                              ParameterizedModel parameterizedModel) {
    // TODO work with an indexed extension model
    final Map<String, String> processedParameters = new HashMap<>();

    // TODO missing parameters that do not exists in ext. model as the name parameter
    // TODO remove try, just for troubleshooting.
    ParameterModelsProvider parameterModelsProvider = ParameterModelsProvider.fromParameterizedModel(parameterizedModel);
    Stream<ParameterAst> simpleParametersStream;
    try {
      simpleParametersStream = componentDefinition.getParameterDefinitions().stream()
          .map(parameterDefinition -> {
            return extensionsHelper
                .findParameterModel(parameterModelsProvider,
                                    parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier())
                .map(parameterModel -> {
                  processedParameters.put(parameterModel.getName(), parameterModel.getName());
                  return ParameterAst.builder()
                      .withModel(parameterModel)
                      .withSourceCodeLocation(componentDefinition.getSourceCodeLocation())
                      .withParameterIdentifier(ParameterIdentifierAst.builder()
                          .withSourceCodeLocation(parameterDefinition.getParameterIdentifierDefinition().getSourceCodeLocation())
                          .withIdentifier(parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier())
                          .build())
                      .withValue(SimpleParameterValueAst.builder()
                          .withRawValue(parameterDefinition.getParameterValueDefinition().getRawValue())
                          .withSourceCodeLocation(parameterDefinition.getParameterValueDefinition().getSourceCodeLocation())
                          .build())
                      .build();
                });
          })
          .filter(optional -> optional.isPresent())
          .map(optional -> optional.get());
    } catch (Exception e) {
      System.out.println(componentDefinition);
      e.printStackTrace();
      throw e;
    }

    Stream<ParameterAst> complexParametersStream = componentDefinition.getChildComponentDefinitions().stream()
        .filter(childComponentDefinition -> isParameterElement(childComponentDefinition.getIdentifier(), parameterizedModel))
        .map(childComponentDefinition -> {
          Optional<ParameterGroupModel> parameterGroupOptional =
              extensionsHelper.findParameterGroup(ParameterGroupModelsProvider.fromParameterizedModel(parameterizedModel),
                                                  childComponentDefinition.getIdentifier());
          if (parameterGroupOptional.isPresent()) {
            ParameterGroupModel parameterGroupModel = parameterGroupOptional.get();
            // specific case for operations output since it's not complaint with extension models
            if (parameterGroupModel.getName().equals("output") || parameterGroupModel.getName().equals("outputAttributes")) {

              ParameterAst parameterAst = ParameterAst.builder()
                  .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                  .withParameterIdentifier(ParameterIdentifierAst.builder()
                      .withIdentifier(childComponentDefinition.getIdentifier())
                      .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                      .build())
                  .withValue(ComplexParameterValueAst.builder()
                      .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                      .withComponent(ParameterComponentAst.builder()
                          .withComponentIdentifier(childComponentDefinition.getIdentifier()) // TODO this does not make sense
                          .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                          .withParameters(childComponentDefinition.getParameterDefinitions().stream()
                              .map(childParameterDefinition -> ParameterAst.builder()
                                  .withSourceCodeLocation(childParameterDefinition.getSourceCodeLocation())
                                  .withModel(extensionsHelper.findParameterModel(fromParameterGroupModel(parameterGroupModel),
                                                                                 childParameterDefinition
                                                                                     .getParameterIdentifierDefinition()
                                                                                     .getComponentIdentifier())
                                      .orElse(null))
                                  .withParameterIdentifier(ParameterIdentifierAst.builder()
                                      .withSourceCodeLocation(childParameterDefinition.getParameterIdentifierDefinition()
                                          .getSourceCodeLocation())
                                      .withIdentifier(childParameterDefinition.getParameterIdentifierDefinition()
                                          .getComponentIdentifier())
                                      .build())
                                  .withValue(SimpleParameterValueAst.builder()
                                      .withRawValue(childParameterDefinition.getParameterValueDefinition().getRawValue())
                                      .withSourceCodeLocation(childParameterDefinition.getParameterValueDefinition()
                                          .getSourceCodeLocation())
                                      .build())
                                  .build())
                              .collect(Collectors.toList()))
                          .build())
                      .build())
                  .build();

              return ImmutableList.of(parameterAst);
            } else if (parameterGroupModel.getName().equals("parameters")) {
              return ImmutableList.of(ParameterAst.builder()
                  .withParameterIdentifier(ParameterIdentifierAst.builder()
                      .withIdentifier(childComponentDefinition.getIdentifier())
                      .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                      .build())
                  .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                  .withValue(ComplexParameterValueAst.builder()
                      .withComponent(ParameterComponentAst.builder()
                          .withComponentIdentifier(childComponentDefinition.getIdentifier())
                          .withParameters(extractOperationParameters(childComponentDefinition))
                          .build())
                      .build())
                  .build());
            }
            return parameterGroupModel.getParameterModels()
                .stream()
                .map(parameterModel -> ParameterAst.builder()
                    .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                    .withParameterIdentifier(ParameterIdentifierAst.builder()
                        .withIdentifier(childComponentDefinition.getIdentifier())
                        .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                        .build())
                    .withModel(parameterModel)
                    .withValue(SimpleParameterValueAst.builder()
                        .withSourceCodeLocation(childComponentDefinition.getParameterValueDefinition().get()
                            .getSourceCodeLocation())
                        .withRawValue(childComponentDefinition.getParameterValueDefinition().get().getRawValue())
                        .build())
                    .build())
                .collect(Collectors.toList());
          } else {
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

            return Stream.of(parameterModelOptional.map(parameterModel -> {
              Preconditions.checkState(childComponentDefinition.getChildComponentDefinitions().size() <= 1,
                                       "Only one child maximum should be available at this point");
              if (isContentParameter(parameterModel)) {
                return ParameterAst.builder()
                    .withModel(parameterModel)
                    .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                    .withParameterIdentifier(ParameterIdentifierAst.builder()
                        .withIdentifier(childComponentDefinition.getIdentifier())
                        .build())
                    .withValue(SimpleParameterValueAst.builder()
                        .withSourceCodeLocation(childComponentDefinition.getParameterValueDefinition().get()
                            .getSourceCodeLocation())
                        .withRawValue(childComponentDefinition.getParameterValueDefinition().get().getRawValue()) // TODO I'm
                        // assuming the
                        // value exists.
                        .build())
                    .build();

              } else if (!childComponentDefinition.getChildComponentDefinitions().isEmpty()) {
                // TODO extended type. This syntax is particular of XML. This must be solved by DSL.
                ComponentDefinition specificImplementionChildDefinition =
                    childComponentDefinition.getChildComponentDefinitions().get(0);
                ComponentAst component = createComponent(specificImplementionChildDefinition, empty());
                return ParameterAst.builder()
                    .withModel(parameterModel)
                    .withSourceCodeLocation(childComponentDefinition.getSourceCodeLocation())
                    .withParameterIdentifier(ParameterIdentifierAst.builder()
                        .withIdentifier(childComponentDefinition.getIdentifier())
                        .build())
                    .withValue(ComplexParameterValueAst.builder()
                        .withComponent(component) // TODO review if the value should be over the parameter wrapper or the
                        // paramerter
                        // itself.
                        .withSourceCodeLocation(specificImplementionChildDefinition.getSourceCodeLocation())
                        .build())
                    .build();
              } else {
                throw new RuntimeException();
              }

            }).orElse(null)).collect(Collectors.toList());
          }
        })
        .flatMap(list -> list.stream())
        .filter(value -> value != null);// TODO this seems weird. probably I can get rid of it.

    List<ParameterAst> parameters =
        Streams.concat(simpleParametersStream, complexParametersStream).collect(Collectors.toList());

    // TODO do not add if already exists
    componentDefinition.getParameterDefinitions().stream()
        .filter(parameterDefinition -> parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier().getName()
            .equals("name")
            || parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier().getName()
                .equals("config-ref"))
        .findAny()
        .ifPresent(parameterDefinition -> {
          parameters.add(ParameterAst.builder()
              .withSourceCodeLocation(parameterDefinition.getSourceCodeLocation())
              .withParameterIdentifier(ParameterIdentifierAst.builder()
                  .withIdentifier(parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier())
                  .withSourceCodeLocation(parameterDefinition.getParameterIdentifierDefinition().getSourceCodeLocation())
                  .build())
              .withValue(SimpleParameterValueAst.builder()
                  .withRawValue(parameterDefinition.getParameterValueDefinition().getRawValue())
                  .withSourceCodeLocation(parameterDefinition.getParameterValueDefinition().getSourceCodeLocation())
                  .build())
              .build());
        });

    return parameters;
  }

  private List<ParameterAst> extractOperationParameters(ComponentDefinition parametersDefinition) {
    return parametersDefinition.getChildComponentDefinitions()
        .stream()
        .map(parameterDefinition -> ParameterAst.builder()
            .withParameterIdentifier(ParameterIdentifierAst.builder()
                .withIdentifier(parameterDefinition.getIdentifier())
                .withSourceCodeLocation(parameterDefinition.getSourceCodeLocation())
                .build())
            .withValue(ComplexParameterValueAst.builder()
                .withComponent(ParameterComponentAst.builder()
                    .withComponentIdentifier(parameterDefinition.getIdentifier())
                    .withParameters(createParameterForOperationParameter(parameterDefinition))
                    .build())
                .build())
            .build())
        .collect(Collectors.toList());
  }

  private List<ParameterAst> createParameterForOperationParameter(ComponentDefinition parameterComponentDefinition) {
    List<ParameterAst> parameterAsts = new ArrayList<>();

    parameterComponentDefinition.getParameterDefinitions().stream()
        .forEach(parameterDefinition -> parameterAsts.add(ParameterAst.builder()
            .withParameterIdentifier(ParameterIdentifierAst.builder()
                .withSourceCodeLocation(parameterDefinition.getParameterIdentifierDefinition().getSourceCodeLocation())
                .withIdentifier(parameterDefinition.getParameterIdentifierDefinition().getComponentIdentifier())
                .build())
            .withValue(SimpleParameterValueAst.builder()
                .withRawValue(parameterDefinition.getParameterValueDefinition().getRawValue())
                .withSourceCodeLocation(parameterDefinition.getParameterValueDefinition().getSourceCodeLocation())
                .build())
            .build()));

    return parameterAsts;
  }

  private boolean isContentParameter(ParameterModel parameterModel) {
    return parameterModel.getRole().equals(ParameterRole.CONTENT)
        || parameterModel.getRole().equals(ParameterRole.PRIMARY_CONTENT);
  }


}
