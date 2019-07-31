/*
 * Copyright 2019 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.upom;

import java.util.regex.Pattern;
import jdk.nashorn.internal.objects.annotations.Property;
import org.apache.maven.model.Dependency;

/**
 * Dependency pattern.
 *
 * @since 1.0.2
 */
public class DependencyPattern {

  /**
   * Group ID, allowed wildcard pattern.
   */
  @Property(name = "groupId")
  private String groupId = null;

  /**
   * Artifact ID, allowed wildcard pattern.
   */
  @Property(name = "artifactId")
  private String artifactId = null;

  /**
   * Version, allowed wildcard pattern.
   */
  @Property(name = "version")
  private String version = null;

  /**
   * Scope, allowed wildcard pattern.
   */
  @Property(name = "scope")
  private String scope = null;

  /**
   * Optional flag, allowed wildcard pattern.
   */
  @Property(name = "optional")
  private String optional = null;

  /**
   * System path, allowed wildcard pattern.
   */
  @Property(name = "systemPath")
  private String systemPath = null;

  /**
   * Type, allowed wildcard pattern.
   */
  @Property(name = "type")
  private String type = null;

  /**
   * Classifier, allowed wildcard pattern.
   */
  @Property(name = "classifier")
  private String classifier = null;

  private Pattern groupIdPattern;
  private Pattern artifactIdPattern;
  private Pattern versionPattern;
  private Pattern scopePattern;
  private Pattern optionalPattern;
  private Pattern systemPathPattern;
  private Pattern typePattern;
  private Pattern classifierPattern;

  public String getClassifier() {
    return this.classifier;
  }

  public void setClassifier(final String value) {
    this.classifier = value;
  }

  public String getType() {
    return this.type;
  }

  public void setType(final String value) {
    this.type = value;
  }

  public String getGroupId() {
    return this.groupId;
  }

  public void setGroupId(final String value) {
    this.groupId = value;
  }

  public String getArtifactId() {
    return this.artifactId;
  }

  public void setArtifactId(final String value) {
    this.artifactId = value;
  }

  public String getVersion() {
    return this.version;
  }

  public void setVersion(final String value) {
    this.version = value;
  }

  public String getScope() {
    return this.scope;
  }

  public void setScope(final String value) {
    this.scope = value;
  }

  public String getOptional() {
    return this.optional;
  }

  public void setOptional(final String value) {
    this.optional = value;
  }

  public String getSystemPath() {
    return this.systemPath;
  }

  public void setSystemPath(final String value) {
    this.systemPath = value;
  }

  public boolean maths(final Dependency dependency) {
    if (dependency == null) {
      return false;
    }

    ensurePatterns();

    int counter = 0;
    int totalCounter = 0;

    if (this.groupId != null) {
      totalCounter++;
      if (dependency.getGroupId()!=null && this.groupIdPattern.matcher(dependency.getGroupId()).matches()) {
        counter++;
      }
    }
    if (this.artifactId != null) {
      totalCounter++;
      if (dependency.getArtifactId() != null && this.artifactIdPattern.matcher(dependency.getArtifactId()).matches()) {
        counter++;
      }
    }
    if (this.version != null) {
      totalCounter++;
      if (dependency.getVersion()!=null && this.versionPattern.matcher(dependency.getVersion()).matches()) {
        counter++;
      }
    }
    if (this.classifier != null) {
      totalCounter++;
      if (dependency.getClassifier()!=null && this.classifierPattern.matcher(dependency.getClassifier()).matches()) {
        counter++;
      }
    }
    if (this.type != null) {
      totalCounter++;
      if (dependency.getType()!=null && this.typePattern.matcher(dependency.getType()).matches()) {
        counter++;
      }
    }
    if (this.optional != null) {
      totalCounter++;
      if (dependency.getOptional()!=null && this.optionalPattern.matcher(dependency.getOptional()).matches()) {
        counter++;
      }
    }
    if (this.systemPath != null) {
      totalCounter++;
      if (dependency.getSystemPath()!=null && this.systemPathPattern.matcher(dependency.getSystemPath()).matches()) {
        counter++;
      }
    }
    if (this.scope != null) {
      totalCounter++;
      if (dependency.getScope()!=null && this.scopePattern.matcher(dependency.getScope()).matches()) {
        counter++;
      }
    }

    return totalCounter > 0 && totalCounter == counter;
  }

  private void ensurePatterns() {
    if (this.artifactIdPattern == null) {
      this.artifactIdPattern = makePattern(this.artifactId);
    }
    if (this.groupIdPattern == null) {
      this.groupIdPattern = makePattern(this.groupId);
    }
    if (this.optionalPattern == null) {
      this.optionalPattern = makePattern(this.optional);
    }
    if (this.scopePattern == null) {
      this.scopePattern = makePattern(this.scope);
    }
    if (this.systemPathPattern == null) {
      this.systemPathPattern = makePattern(this.systemPath);
    }
    if (this.versionPattern == null) {
      this.versionPattern = makePattern(this.version);
    }
    if (this.classifierPattern == null) {
      this.classifierPattern = makePattern(this.classifier);
    }
    if (this.typePattern == null) {
      this.typePattern = makePattern(this.type);
    }
  }

  private static Pattern makePattern(final String text) {
    if (text == null) {
      return Pattern.compile(".*");
    }

    final StringBuilder builder = new StringBuilder();

    for (final char c : text.toCharArray()) {
      switch (c) {
        case '*':
          builder.append(".*");
          break;
        case '?':
          builder.append(".");
          break;
        default:
          builder.append(Pattern.quote(Character.toString(c)));
          break;
      }
    }

    return Pattern.compile(builder.toString());
  }

}
