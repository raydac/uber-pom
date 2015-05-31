/* 
 * Copyright 2015 Igor Maznitsa (http://www.igormaznitsa.com).
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

import java.io.File;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/**
 * Maven plugin to merge pom files in project hierarchy, also allows make
 * modifications in the result pom.
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 */
@Mojo(name = "upom", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class UPomMojo extends AbstractMojo {

  public static final String LINE_SEPARATOR = System.getProperty("line.separator", "/r/n");

  /**
   * The Project to be processed.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  protected MavenProject project;

  /**
   * The Folder where the uber-pom should be saved.
   */
  @Parameter(name = "folder", defaultValue = "${basedir}")
  protected File folder;

  /**
   * The Name of the uber-pom file.
   */
  @Parameter(name = "name", defaultValue = "uber-pom.xml")
  protected String name;

  /**
   * List of paths to be removed from the result pom file. Example of a path:
   * "build/plugins"
   */
  @Parameter(name = "remove")
  protected String[] remove;

  /**
   * List of sections to not be modified in the result. Example of a path:
   * "build/plugins"
   */
  @Parameter(name = "keep")
  protected String[] keep;

  /**
   * Enforce filling project parameters by values the generated uber-pom. By
   * default the uber-pom will be just saved and link to new the file will be
   * redirected. If the parameter is true then uber-pom model values will be
   * injected in fields of the current maven project model.
   */
  @Parameter(name = "enforceInjecting", defaultValue = "false")
  protected boolean enforceInjecting;

  /**
   * Delete generated pom file after session.
   */
  @Parameter(name = "deleteOnExit", defaultValue = "true")
  protected boolean deleteOnExit;

  /**
   * Number of levels in hierarchy to merge, If less than zero then whole
   * hierarchy will be merged.
   */
  @Parameter(name = "depth", defaultValue = "-1")
  protected int depth;

  /**
   * String properties to be set in the result pom.
   */
  @Parameter(name = "set")
  protected Properties set;

  public File getFolder() {
    return this.folder;
  }

  public String getName() {
    return this.name;
  }

  public boolean isDeleteOnExit() {
    return this.deleteOnExit;
  }

  public int getDepth() {
    return this.depth;
  }

  public String[] getRemove() {
    return this.remove == null ? null : this.remove.clone();
  }

  public String[] getKeep() {
    return this.keep == null ? null : this.keep.clone();
  }

  public boolean isEnforceInjecting() {
    return this.enforceInjecting;
  }

  public Properties getSet() {
    return this.set;
  }

  private Model[] collectFullHierarchy(final MavenProject project) {
    final List<Model> result = new ArrayList<Model>();
    MavenProject current = project;
    while (current != null) {
      result.add(0, current.getOriginalModel());
      current = current.getParent() == null ? null : current.getParent();
    }
    return result.toArray(new Model[result.size()]);
  }

  private static UPomModel[] collectModels(final MavenProject project, final int depth) {
    final List<UPomModel> result = new ArrayList<UPomModel>();
    int levels = depth < 0 ? Integer.MAX_VALUE : depth;
    MavenProject current = project;
    while (current != null && levels-- >= 0) {
      result.add(0, new UPomModel(current.getOriginalModel()));
      current = current.getParent();
    }

    return result.toArray(new UPomModel[result.size()]);
  }

  private void updateProjectForNewPom(final UPomModel upomModel, final File uberPomFile) throws Exception {
    upomModel.assignTo(this.project);
    getLog().debug("Model assigned to project");
    this.project.setFile(uberPomFile);
    getLog().debug("File has been set to project");
  }

  private File saveUberPom(final UPomModel model) throws Exception {
    final File uberPomFile = new File(this.folder, this.name);
    FileUtils.write(uberPomFile, model.asXML(), "UTF-8");
    if (this.deleteOnExit) {
      getLog().info("NB! The Result uber-pom file marked to be removed after JVM session");
      uberPomFile.deleteOnExit();
    }
    return uberPomFile;
  }

  private static String getNameOfModel(final Model model) {
    if (model == null) {
      return "";
    }
    final String group = model.getGroupId();
    final String artifact = model.getArtifactId();
    final String name = model.getName();
    final String version = model.getVersion();

    final StringBuilder result = new StringBuilder();

    if (group == null) {
      result.append("<inherited>");
    }
    else {
      result.append(group);
    }
    result.append(':');
    if (artifact == null) {
      result.append("<inherited>");
    }
    else {
      result.append(artifact);
    }
    result.append(':');
    if (name == null) {
      result.append("<inherited>");
    }
    else {
      result.append(name);
    }
    result.append(':');
    if (version == null) {
      result.append("<inherited>");
    }
    else {
      result.append(version);
    }

    return result.toString();
  }

  private static void spaces(final StringBuilder buffer, int len) {
    while (len-- > 0) {
      buffer.append(' ');
    }
  }

  private static String drawHierarchy(final Model[] fullHierarchy, final UPomModel[] processedHierarchy) {
    final StringBuilder result = new StringBuilder();

    final int TAB = 4;

    int startIndex = fullHierarchy.length - processedHierarchy.length;
    int insets = 0;
    // draw non-included
    for (int i = 0; i <= startIndex; i++) {
      if (result.length() > 0) {
        result.append(LINE_SEPARATOR);
        spaces(result, insets);
        result.append((char) 0x2506);
        result.append(LINE_SEPARATOR);
        spaces(result, insets);
        result.append((char) 0x2570).append((char) 0x2504);
      }
      result.append(getNameOfModel(fullHierarchy[i]));
      insets += TAB;
    }
    // draw included
    for (int i = 1; i < processedHierarchy.length; i++) {
      if (result.length() > 0) {
        result.append(LINE_SEPARATOR);
        spaces(result, insets);
        result.append((char) 0x2503);
        result.append(LINE_SEPARATOR);
        spaces(result, insets);
        result.append((char) 0x2517).append((char) 0x2501);
      }
      result.append(getNameOfModel(processedHierarchy[i].getModel()));
      insets += TAB;
    }
    return result.toString();
  }

  private static int maxLength(final String[] strs) {
    if (strs == null || strs.length == 0) {
      return 0;
    }
    int max = 0;
    for (final String s : strs) {
      if (s.length() > max) {
        max = s.length();
      }
    }
    return max;
  }

  private static String makeDotString(final int length) {
    final StringBuilder result = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      result.append('.');
    }
    return result.toString();
  }

  @Override
  public void execute() throws MojoExecutionException {
    String strToPrint = null;

    try {
      final Model[] collectFullHierarchy = collectFullHierarchy(this.project);
      final UPomModel[] models = collectModels(this.project, this.depth);

      getLog().info(".........................................................");
      for (final String s : drawHierarchy(collectFullHierarchy, models).split("\\n")) {
        getLog().info(s);
      }
      getLog().info(".........................................................");

      final UPomModel main = models[0];

      for (int i = 1; i < models.length; i++) {
        final boolean last = i == models.length - 1;
        final UPomModel model = models[i];
        if (last && this.keep != null && this.keep.length > 0) {
          getLog().info("");

          getLog().debug("Freezing state of sections for result project pom:" + Arrays.toString(this.keep));
          model.saveState(this.keep);

          for (final String s : this.keep) {
            getLog().info("Freezing path \'" + s + "\' in the result pom");
          }

          getLog().debug("Merging last model");
          main.merge(model);

          getLog().debug("Restoring state of sections for project pom:" + Arrays.toString(this.keep));
          main.restoreStateFrom(model);
        }
        else {
          getLog().debug("Merging model");
          main.merge(model);
        }
      }

      getLog().info("");

      final String REMOVE_PREFIX = "Remove ";
      int maxLength = REMOVE_PREFIX.length() + maxLength(this.remove) + 12;

      if (this.remove != null && this.remove.length > 0) {
        for (final String path : this.remove) {
          final String prefix = REMOVE_PREFIX + '\'' + path + '\'';
          strToPrint = prefix + makeDotString(maxLength - prefix.length());
          final boolean removed = main.remove(path);
          getLog().info(strToPrint + (removed ? "OK" : "NOT FOUND"));
          strToPrint = null;
        }
      }
      getLog().info("");

      if (this.set != null && !this.set.isEmpty()) {
        strToPrint = null;
        for (final String key : this.set.stringPropertyNames()) {
          final String value = this.set.getProperty(key);
          try {
            getLog().info("Set value to path : '" + key + "\'=\'" + value + '\'');
            main.set(key, value);
          }
          catch (Exception ex) {
            getLog().debug(ex);
            throw new UPomException("Can't set string value to '" + key + '\'');
          }
        }
        getLog().info("");
      }

      getLog().debug("Saving uber-pom into project");
      final File saveUberPom = saveUberPom(main);

      getLog().info("Uber-pom saved as '" + saveUberPom.getAbsolutePath() + '\'');

      getLog().debug("Injecting new uber-pom into project");
      updateProjectForNewPom(main, saveUberPom);

      getLog().info("Uber-pom assigned to project");

      if (this.enforceInjecting) {
        getLog().info("NB! Injecting generated uber-pom parameters into inside project fields!");
        main.injectIntoProject(getLog(), this.project);
      }
    }
    catch (UPomException ex) {
      getLog().debug(ex);

      if (strToPrint != null) {
        getLog().info(strToPrint + "ERROR");
      }
      getLog().error(ex.getMessage());
      throw new MojoExecutionException("Error during processing", ex);
    }
    catch (Exception ex) {
      throw new MojoExecutionException("Error during processing", ex);
    }
  }
}
