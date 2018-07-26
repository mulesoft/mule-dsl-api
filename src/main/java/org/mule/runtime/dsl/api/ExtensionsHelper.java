/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;

import java.util.Map;
import java.util.Optional;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.dsl.internal.parser.ParameterGroupModelsProvider;
import org.mule.runtime.dsl.internal.parser.ParameterModelsProvider;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

public class ExtensionsHelper {

  private final DslResolvingContext resolvingContext;
  private final Map<ExtensionModel, DslSyntaxResolver> resolvers;
  private ExtensionModelProvider extensionModelProvider;

  public ExtensionsHelper(ExtensionModelProvider extensionModelProvider) {
    this.extensionModelProvider = extensionModelProvider;
    this.resolvingContext = DslResolvingContext.getDefault(extensionModelProvider.getExtensionModels());
    this.resolvers = resolvingContext.getExtensions().stream()
        .collect(toMap(e -> e, e -> DslSyntaxResolver.getDefault(e, resolvingContext)));
  }

  public Object findModel(ComponentIdentifier identifier) {

    // TODO this code is duplicated from ConfigurationBasedElementModelFactory

    Optional<Map.Entry<ExtensionModel, DslSyntaxResolver>> entry = findExtensionEntry(identifier);

    if (!entry.isPresent()) {
      return null;
    }

    ExtensionModel currentExtension = entry.get().getKey();
    DslSyntaxResolver dsl = entry.get().getValue();

    Reference<Object> elementModel = new Reference<>();
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            elementModel.set(model);
            stop();
          }
        });
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            elementModel.set(model);
            stop();
          }
        });
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            elementModel.set(model);
            stop();
          }
        });
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            elementModel.set(model);
            stop();
          }
        });
      }

    }.walk(currentExtension);

    if (elementModel.get() == null) {
      resolveBasedOnTypes(currentExtension, identifier, dsl)
          .ifPresent(elementModel::set);
    }

    return elementModel.get();

  }

  private Optional<Map.Entry<ExtensionModel, DslSyntaxResolver>> findExtensionEntry(ComponentIdentifier identifier) {
    return resolvers.entrySet().stream()
        .filter(e -> e.getKey().getXmlDslModel().getPrefix().equals(identifier.getNamespace()))
        .findFirst();
  }

  private Optional<ComponentIdentifier> getIdentifier(DslElementSyntax dsl) {
    if (isNotBlank(dsl.getElementName()) && isNotBlank(dsl.getPrefix())) {
      return Optional.of(builder()
          .name(dsl.getElementName())
          .namespace(dsl.getPrefix())
          .build());
    }

    return empty();
  }

  private Optional<ObjectType> resolveBasedOnTypes(ExtensionModel extensionModel, ComponentIdentifier identifier,
                                                   DslSyntaxResolver dsl) {
    return extensionModel.getTypes().stream()
        .map(type -> resolveBasedOnType(dsl, type, identifier))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }


  private Optional<ObjectType> resolveBasedOnType(DslSyntaxResolver dsl,
                                                  ObjectType type,
                                                  ComponentIdentifier componentIdentifier) {
    Optional<DslElementSyntax> typeDsl = dsl.resolve(type);
    if (typeDsl.isPresent()) {
      Optional<ComponentIdentifier> elementIdentifier = getIdentifier(typeDsl.get());
      if (elementIdentifier.isPresent() && elementIdentifier.get().equals(componentIdentifier)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }

  public Optional<ParameterModel> findParameterModel(ParameterModelsProvider parameterModelsProvider,
                                                     ComponentIdentifier parameterIdentifier) {

    // Optional<ExtensionModel> parameterIdentifierExtensionModel = extensionModels.stream().filter(em ->
    // em.getName().toLowerCase().equals(parameterIdentifier.getNamespace())).findAny();
    // if (parameterIdentifierExtensionModel.isPresent()) {
    // ExtensionModel extensionModel = parameterIdentifierExtensionModel.get();
    // extensionModel.getSubTypes().stream()
    // .filter(type -> type.getBaseType().)
    // }

    Optional<Map.Entry<ExtensionModel, DslSyntaxResolver>> entry = findExtensionEntry(parameterIdentifier);

    if (!entry.isPresent()) {
      return empty(); // TODO what should we do?
    }

    DslSyntaxResolver dsl = entry.get().getValue();

    return parameterModelsProvider.getAllParameterModels()
        .stream()
        .filter(parameterModel -> dsl.resolve(parameterModel).getAttributeName().equals(parameterIdentifier.getName())
            || dsl.resolve(parameterModel).getElementName().equals(parameterIdentifier.getName()))
        .findAny();
  }

  public Optional<ParameterGroupModel> findParameterGroup(ParameterGroupModelsProvider parameterGroupModelsProvider,
                                                          ComponentIdentifier parameterIdentifier) {
    return parameterGroupModelsProvider
        .getParameterGroupModels()
        .stream()
        .filter(parameterGroup -> parameterGroup.isShowInDsl()
            && parameterGroup.getName().equals(parameterIdentifier.getName())) // TODO this is not taking into account namespaces
        // nor converting Dsl text to
        .findAny();
  }


  public Optional<ParameterModel> findParameterModel(ComponentIdentifier parameterOwnerComponentIdentifier,
                                                     ComponentIdentifier parameterIdentifier) {

    Object model = findModel(parameterOwnerComponentIdentifier);
    if (model instanceof ComponentModel) {
      // TODO have in mind the namespace of the attribute
      // TODO have in mind complex child elements that are parameter models
      return findParameterModel(ParameterModelsProvider.fromParameterizedModel((ParameterizedModel) model), parameterIdentifier);
    } else {
      throw new RuntimeException(parameterOwnerComponentIdentifier + " " + parameterIdentifier);
    }
  }

  public Object findNestedComponentWithinModel(ComponentIdentifier identifier, ConstructModel constructModel) {

    Optional<Map.Entry<ExtensionModel, DslSyntaxResolver>> entry = findExtensionEntry(identifier);

    if (!entry.isPresent()) {
      return null;
    }

    DslSyntaxResolver dsl = entry.get().getValue();

    Optional<? extends NestableElementModel> nestedElementModelOptional = constructModel.getNestedComponents().stream()
        .filter(elementModel -> getIdentifier(dsl.resolve(elementModel))
            .map(foundIdentifier -> foundIdentifier.equals(identifier))
            .orElse(false))
        .findFirst();

    // TODO change to not return null
    return nestedElementModelOptional.orElse(null);
  }

  public Optional<ConnectionProviderModel> findConnectionProvider(ComponentIdentifier componentIdentifier,
                                                                  ConfigurationModel configurationModel) {
    Optional<Map.Entry<ExtensionModel, DslSyntaxResolver>> entry = findExtensionEntry(componentIdentifier);

    if (!entry.isPresent()) {
      return null;
    }

    DslSyntaxResolver dsl = entry.get().getValue();

    return configurationModel.getConnectionProviders()
        .stream()
        .filter(connectionProviderModel -> getIdentifier(dsl.resolve(connectionProviderModel)).get().equals(componentIdentifier))
        .findAny();
  }

  public ExtensionModel findExtensionModelOwning(ComponentIdentifier identifier) {
    return extensionModelProvider.getExtensionModels().stream()
        .filter(extensionModel -> extensionModel.getName().equalsIgnoreCase(identifier.getNamespace()))
        .findAny().get();
  }
}
