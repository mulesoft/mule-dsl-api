/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component.config;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.LocationPart;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import com.google.common.collect.ImmutableList;

/**
 * A component location describes where the component is defined in the configuration of the artifact.
 *
 * For instance:
 * <ul>
 * <li>COMPONENT_NAME - global component defined with name COMPONENT_NAME</li>
 * <li>FLOW_NAME/source - a source defined within a flow</li>
 * <li>FLOW_NAME/processors/0 - the first processor defined within a flow with name FLOW_NAME</li>
 * <li>FLOW_NAME/processors/4/1 - the first processors defined inside another processor which is positioned fifth within a flow
 * with name FLOW_NAME</li>
 * <li>FLOW_NAME/errorHandler/0 - the first on-error within the error handler</li>
 * <li>FLOW_NAME/0/errorHandler/3 - the third on-error within the error handler of the first element of the flow with name
 * FLOW_NAME</li>
 * </ul>
 *
 * The different {@link DefaultLocationPart}s in FLOW_NAME/processors/1 are:
 * <ul>
 * <li>'processors' as partPath and no component identifier since this part is synthetic to indicate the part of the flow
 * referenced by the next index</li>
 * <li>'1' as partPath and 'mule:payload' as component identifier assuming that the second processor of the flow was a set-payload
 * component</li>
 * </ul>
 *
 * @since 4.0
 */
@NoExtend
public class DefaultComponentLocation implements ComponentLocation, Serializable {

  private static final long serialVersionUID = 4958158607813720623L;

  private final String name;
  private final LinkedList<DefaultLocationPart> parts;
  private final transient List<URI> importChain;
  private volatile String location;

  private transient String rootContainerName;
  private transient TypedComponentIdentifier componentIdentifier;

  /**
   * Creates a virtual {@link ComponentLocation} for a single element, using the core namespace and using UNKNOWN as type. Only
   * meant for situations where a real location cannot be obtained.
   *
   * @param component the name of the element
   * @return a location for it
   *
   * @since 1.4.0
   */
  public static ComponentLocation from(String component) {
    return fromSingleComponent(component);
  }

  /**
   * Creates a virtual {@link ComponentLocation} for a single element, using the core namespace and using UNKNOWN as type. Only
   * meant for situations where a real location cannot be obtained.
   *
   * @param component the name of the element
   * @return a location for it
   *
   * @deprecated use {@link #from(String)} instead.
   */
  @Deprecated
  public static DefaultComponentLocation fromSingleComponent(String component) {
    DefaultLocationPart part = new DefaultLocationPart(component,
                                                       of(TypedComponentIdentifier.builder()
                                                           .type(UNKNOWN)
                                                           .identifier(ComponentIdentifier
                                                               .buildFromStringRepresentation(component))
                                                           .build()),
                                                       empty(),
                                                       OptionalInt.empty(),
                                                       OptionalInt.empty());
    return new DefaultComponentLocation(of(component), asList(part));
  }

  /**
   * @param name  the name of the global element in which the specific component is located.
   * @param parts the set of parts to locate the component.
   */
  public DefaultComponentLocation(Optional<String> name, List<DefaultLocationPart> parts) {
    this(name, parts, emptyList());
  }

  /**
   * @param name        the name of the global element in which the specific component is located.
   * @param parts       the set of parts to locate the component.
   * @param importChain a {@link List} containing an element for the location of every {@code import} tag leading to the file
   *                    containing the owning component.
   */
  public DefaultComponentLocation(Optional<String> name, List<DefaultLocationPart> parts, List<URI> importChain) {
    this.name = name.orElse(null);
    this.parts = new LinkedList<>(parts);
    this.importChain = new ArrayList<>(importChain);
    componentIdentifier = calculateComponentIdentifier(parts);
    rootContainerName = calculateRootContainerName();
  }

  /**
   * {@inheritDoc}
   */
  public Optional<String> getName() {
    return ofNullable(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<LocationPart> getParts() {
    return unmodifiableList(parts);
  }

  @Override
  public TypedComponentIdentifier getComponentIdentifier() {
    if (componentIdentifier == null) {
      throw new NoSuchElementException("No 'componentIdentifier' set for location '" + getLocation() + "'");
    }
    return componentIdentifier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> getFileName() {
    return parts.getLast().getFileName();
  }

  @Override
  public List<URI> getImportChain() {
    return importChain;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Integer> getLineInFile() {
    return parts.getLast().getLineInFile();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Integer> getStartColumn() {
    return parts.getLast().getStartColumn();
  }

  @Override
  public OptionalInt getLine() {
    return parts.getLast().getLine();
  }

  @Override
  public OptionalInt getColumn() {
    return parts.getLast().getColumn();
  }

  /**
   * @return a string representation of the {@link DefaultComponentLocation}.
   */
  @Override
  public String getLocation() {
    if (location == null) {
      synchronized (this) {
        if (location == null) {
          StringBuilder locationBuilder = new StringBuilder();
          for (DefaultLocationPart part : parts) {
            locationBuilder.append("/").append(part.getPartPath());
          }
          location = locationBuilder.replace(0, 1, "").toString();
        }
      }
    }
    return location;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRootContainerName() {
    return rootContainerName;
  }

  private String calculateRootContainerName() {
    return getParts().isEmpty() ? null : getParts().get(0).getPartPath();
  }

  /**
   * Creates a new instance of ComponentLocation adding the specified part.
   *
   * @param partPath       the path of this part
   * @param partIdentifier the component identifier of the part if it's not a synthetic part
   * @param fileName       the file name in which the component was defined
   * @param lineInFile     the line number in which the component was defined
   * @param startColumn    the column number in which the component was defined
   * @return a new instance with the given location part appended.
   *
   * @since 1.3
   */
  public DefaultComponentLocation appendLocationPart(String partPath, Optional<TypedComponentIdentifier> partIdentifier,
                                                     Optional<String> fileName, OptionalInt lineInFile,
                                                     OptionalInt startColumn) {
    return new DefaultComponentLocation(ofNullable(name), ImmutableList.<DefaultLocationPart>builder().addAll(parts)
        .add(new DefaultLocationPart(partPath, partIdentifier, fileName, lineInFile, startColumn)).build());
  }


  /**
   * Creates a new instance of ComponentLocation adding the specified part.
   *
   * @param partPath       the path of this part
   * @param partIdentifier the component identifier of the part if it's not a synthetic part
   * @return a new instance with the given location part appended.
   *
   * @deprecated since 1.3 use {@link #appendLocationPart(String, Optional, Optional, OptionalInt, OptionalInt)} instead.
   */
  @Deprecated
  public DefaultComponentLocation appendLocationPart(String partPath, Optional<TypedComponentIdentifier> partIdentifier,
                                                     Optional<String> fileName, Optional<Integer> lineInFile,
                                                     Optional<Integer> startColumn) {
    return new DefaultComponentLocation(ofNullable(name), ImmutableList.<DefaultLocationPart>builder().addAll(parts)
        .add(new DefaultLocationPart(partPath, partIdentifier, fileName, lineInFile, startColumn)).build());
  }

  /**
   * Utility method that adds a processors part to the location. This is the part used for nested processors in configuration
   * components.
   *
   * @return a new instance with the processors location part appended.
   */
  public DefaultComponentLocation appendProcessorsPart() {
    return new DefaultComponentLocation(ofNullable(name), ImmutableList.<DefaultLocationPart>builder().addAll(parts)
        .add(new DefaultLocationPart("processors", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())).build());
  }

  /**
   * Utility method that adds a router part to the location. This is the part used for nested processors in configuration
   * components.
   *
   * @return a new instance with the processors location part appended.
   */
  public DefaultComponentLocation appendRoutePart() {
    return new DefaultComponentLocation(ofNullable(name), ImmutableList.<DefaultLocationPart>builder().addAll(parts)
        .add(new DefaultLocationPart("route", empty(), empty(), OptionalInt.empty(), OptionalInt.empty())).build());
  }

  /**
   * Utility method that adds a connection part to the location. Keep in mind that this method in no way validates that the actual
   * location is a valid one. Clients should add the required logic before calling this method to make sure that the final
   * location corresponds to a correct element.
   *
   * @return a new instance with the connection location part appended.
   */
  public DefaultComponentLocation appendConnectionPart(Optional<TypedComponentIdentifier> partIdentifier,
                                                       Optional<String> fileName,
                                                       OptionalInt lineInFile,
                                                       OptionalInt startColumn) {
    return new DefaultComponentLocation(ofNullable(name), ImmutableList.<DefaultLocationPart>builder().addAll(parts)
        .add(new DefaultLocationPart("connection", partIdentifier, fileName, lineInFile, startColumn)).build());
  }

  /**
   * A location part represent an specific location of a component within another component.
   *
   * @since 4.0
   */
  @NoInstantiate
  @NoExtend
  public static class DefaultLocationPart implements LocationPart, Serializable {

    private static final long serialVersionUID = 5757545892752260058L;

    private final String partPath;
    private final TypedComponentIdentifier partIdentifier;
    private String fileName;
    private transient int lineInFile = -1;
    private transient int startColumn = -1;

    /**
     * @param partPath       the path of this part
     * @param partIdentifier the component identifier of the part if it's not a synthetic part
     * @param fileName       the file name in which the component was defined
     * @param lineInFile     the line number in which the component was defined
     * @param startColumn    the column number in which the component was defined
     *
     * @since 1.3
     */
    public DefaultLocationPart(String partPath, Optional<TypedComponentIdentifier> partIdentifier, Optional<String> fileName,
                               OptionalInt lineInFile, OptionalInt startColumn) {
      this.partPath = partPath;
      this.partIdentifier = partIdentifier.orElse(null);
      fileName.ifPresent(configFileName -> this.fileName = configFileName);
      lineInFile.ifPresent(line -> this.lineInFile = line);
      startColumn.ifPresent(column -> this.startColumn = column);
    }

    /**
     * @param partPath       the path of this part
     * @param partIdentifier the component identifier of the part if it's not a synthetic part
     * @param fileName       the file name in which the component was defined
     * @param lineInFile     the line number in which the component was defined
     *
     * @deprecated since 1.3 use {@link #DefaultComponentLocation(String, Optional, Optional, Optional, Optional)} instead.
     */
    @Deprecated
    public DefaultLocationPart(String partPath, Optional<TypedComponentIdentifier> partIdentifier, Optional<String> fileName,
                               Optional<Integer> lineInFile, Optional<Integer> startColumn) {
      this.partPath = partPath;
      this.partIdentifier = partIdentifier.orElse(null);
      fileName.ifPresent(configFileName -> this.fileName = configFileName);
      lineInFile.ifPresent(line -> this.lineInFile = line);
      startColumn.ifPresent(column -> this.startColumn = column);
    }

    /**
     * @return the string representation of the part
     */
    @Override
    public String getPartPath() {
      return partPath;
    }

    /**
     * @return if it's a synthetic part this is null, if not then it's the identifier of the configuration element.
     */
    @Override
    public Optional<TypedComponentIdentifier> getPartIdentifier() {
      return ofNullable(partIdentifier);
    }

    @Override
    public Optional<String> getFileName() {
      return ofNullable(fileName);
    }

    @Override
    public Optional<Integer> getLineInFile() {
      return lineInFile == -1
          ? empty()
          : ofNullable(lineInFile);
    }

    @Override
    public OptionalInt getLine() {
      return lineInFile == -1
          ? OptionalInt.empty()
          : OptionalInt.of(lineInFile);
    }

    @Override
    public Optional<Integer> getStartColumn() {
      return startColumn == -1
          ? empty()
          : ofNullable(startColumn);
    }

    @Override
    public OptionalInt getColumn() {
      return startColumn == -1
          ? OptionalInt.empty()
          : OptionalInt.of(startColumn);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      DefaultLocationPart that = (DefaultLocationPart) o;

      if (lineInFile != that.lineInFile) {
        return false;
      }
      if (startColumn != that.startColumn) {
        return false;
      }
      if (!Objects.equals(partPath, that.partPath)) {
        return false;
      }
      if (partIdentifier != null ? !partIdentifier.equals(that.partIdentifier)
          : that.partIdentifier != null) {
        return false;
      }
      return fileName != null ? fileName.equals(that.fileName) : that.fileName == null;
    }

    @Override
    public int hashCode() {
      int result = partPath != null ? partPath.hashCode() : 31;
      result = 31 * result + (partIdentifier != null ? partIdentifier.hashCode() : 0);
      result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
      result = 31 * result + (lineInFile != -1 ? lineInFile : 0);
      result = 31 * result + (startColumn != -1 ? startColumn : 0);
      return result;
    }

    @Override
    public String toString() {
      return "DefaultLocationPart{" +
          "partPath='" + partPath + '\'' +
          ", partIdentifier=" + partIdentifier +
          ", fileName='" + fileName + '\'' +
          ", lineInFile=" + lineInFile +
          ", startColumn=" + startColumn +
          '}';
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
      oos.defaultWriteObject();

      oos.writeObject(lineInFile == -1 ? null : Integer.valueOf(lineInFile));
      oos.writeObject(startColumn == -1 ? null : Integer.valueOf(startColumn));
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
      ois.defaultReadObject();

      final Integer readLine = (Integer) ois.readObject();
      lineInFile = readLine != null ? readLine.intValue() : -1;
      final Integer readColumn = (Integer) ois.readObject();
      startColumn = readColumn != null ? readColumn.intValue() : -1;
    }
  }

  private void readObject(ObjectInputStream in) throws Exception {
    in.defaultReadObject();
    this.componentIdentifier = calculateComponentIdentifier(this.parts);
    this.rootContainerName = calculateRootContainerName();
  }

  protected TypedComponentIdentifier calculateComponentIdentifier(List<DefaultLocationPart> parts) {
    return parts.isEmpty() ? null : parts.get(parts.size() - 1).getPartIdentifier().orElse(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultComponentLocation that = (DefaultComponentLocation) o;

    if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) {
      return false;
    }
    if (!getParts().equals(that.getParts())) {
      return false;
    }
    return getLocation() != null ? getLocation().equals(that.getLocation()) : that.getLocation() == null;
  }

  @Override
  public int hashCode() {
    int result = getName() != null ? getName().hashCode() : 0;
    result = 31 * result + getParts().hashCode();
    result = 31 * result + (getLocation() != null ? getLocation().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DefaultComponentLocation{" +
        "name='" + name + '\'' +
        ", parts=" + parts +
        ", location='" + getLocation() + '\'' +
        '}';
  }
}
