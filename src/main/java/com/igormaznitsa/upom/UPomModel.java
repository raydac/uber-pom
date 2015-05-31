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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.merge.ModelMerger;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class UPomModel {

  private static final String MAVEN_MODEL_PACKAGE_PREFIX = "org.apache.maven.model.";

  private final Model model;
  private final Map<String, Object> savedValues = new HashMap<String, Object>();

  public UPomModel(final File modelFile) throws Exception {
    final FileInputStream in = new FileInputStream(modelFile);
    try {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      this.model = reader.read(in, true);
    }
    finally {
      IOUtils.closeQuietly(in);
    }
  }

  public UPomModel(final Model pom) {
    this.model = pom.clone();
  }

  public String asXML() throws IOException {
    final MavenXpp3Writer mavenWritter = new MavenXpp3Writer();
    final StringWriter buffer = new StringWriter(16384);
    mavenWritter.write(buffer, this.model);
    return buffer.toString();
  }

  public void saveState(final String... keepPaths) throws Exception {
    this.savedValues.clear();
    for (final String p : keepPaths) {
      this.savedValues.put(p, this.processPathStepToGet(splitPath(p), 0, this.model));
    }
  }

  public void restoreState() throws Exception {
    for (final Map.Entry<String, Object> e : this.savedValues.entrySet()) {
      this.processPathStepToSet(splitPath(e.getKey()), 0, this.model, e.getValue());
    }
  }

  public void restoreStateFrom(final UPomModel model) throws Exception {
    for (final Map.Entry<String, Object> e : model.savedValues.entrySet()) {
      this.processPathStepToSet(splitPath(e.getKey()), 0, this.model, e.getValue());
    }
  }

  public Model getModel() {
    return this.model;
  }

  public UPomModel merge(final UPomModel other) throws Exception {
    final ModelMerger merger = new ModelMerger();
    merger.merge(this.model, other.model, true, null);
    return this;
  }

  public boolean remove(final String removePath) throws Exception {
    return processPathStepToSet(splitPath(removePath), 0, this.model, null);
  }

  public void set(final String path, final String value) throws Exception {
    this.processPathStepToSet(splitPath(path), 0, this.model, value);
  }

  public Object get(final String path) throws Exception {
    return this.processPathStepToGet(splitPath(path), 0, this.model);
  }

  private static Method findMethod(final Class klazz, final String methodName, final boolean onlyPublic) {
    Method result = null;
    for (final Method m : klazz.getMethods()) {
      if (onlyPublic && !Modifier.isPublic(m.getModifiers())) {
        continue;
      }
      if (m.getName().equalsIgnoreCase(methodName)) {
        result = m;
        break;
      }
    }
    return result;
  }

  private static Field findDeclaredFieldForName(final Class klazz, final String fieldName) {
    Field result = null;
    Class curr = klazz;
    while (curr.getName().startsWith(MAVEN_MODEL_PACKAGE_PREFIX)) {
      for (final Field m : curr.getDeclaredFields()) {
        if (m.getName().equalsIgnoreCase(fieldName)) {
          result = m;
          break;
        }
      }
      if (result != null) {
        result.setAccessible(true);
        break;
      }
      curr = klazz.getSuperclass();
    }
    return result;
  }

  private static String makePathStr(final String[] path, final int toIndex) {
    final StringBuilder result = new StringBuilder();
    for (int i = 0; i <= toIndex; i++) {
      if (result.length() > 0) {
        result.append('/');
      }
      result.append(path[i]);
    }
    return result.toString();
  }

  private static Collection cloneCollection(final Collection collection) throws Exception {
    final Class collectionClass = collection.getClass();
    final Constructor constructor = collectionClass.getConstructor(Collection.class);
    return (Collection) constructor.newInstance(collection);
  }

  private static Map cloneMap(final Map map) throws Exception {
    final Class mapClass = map.getClass();
    final Constructor constructor = mapClass.getConstructor(Map.class);
    return (Map) constructor.newInstance(map);
  }

  private static Object ensureCloning(final Object obj) throws Exception {
    if (obj == null) {
      return null;
    }
    final Method clone = findMethod(obj.getClass(), "clone", true);
    final Object result;
    if (clone == null) {
      if (obj instanceof Map) {
        result = cloneMap((Map) obj);
      }
      else if (obj instanceof Collection) {
        result = cloneCollection((Collection) obj);
      }
      else {
        result = obj;
      }
    }
    else {
      result = clone.invoke(obj);
    }
    return result;
  }

  private static void setField(final Object instance, final Field field, final Object value) throws Exception {
    final Object currentValue = field.get(instance);
    if (currentValue == null) {
      if (value != null) {
        field.setAccessible(true);
        field.set(instance, value);
      }
    }
    else if (currentValue instanceof Map) {
      ((Map) currentValue).clear();
      if (value != null) {
        ((Map) currentValue).putAll((Map) value);
      }
    }
    else if (currentValue instanceof Collection) {
      ((Collection) currentValue).clear();
      if (value != null) {
        ((Collection) currentValue).addAll((Collection) value);
      }
    }
    else {
      field.set(instance, ensureCloning(value));
    }
  }

  private static Object getField(final Object instance, final Field field) throws Exception {
    return ensureCloning(field.get(instance));
  }

  private boolean processPathStepToSet(final String[] path, final int pathStart, final Object instance, final Object value) throws Exception {
    final String fieldName = path[pathStart];

    if (pathStart == path.length - 1) {
      // last step
      // find setter
      final Method setter = findMethod(instance.getClass(), "set" + fieldName, true);
      if (setter == null) {
        throw new UPomException("Can't find model field '" + makePathStr(path, pathStart) + '\'');
      }

      final Class[] params = setter.getParameterTypes();
      if (params.length == 0) {
        throw new UPomException("Detected zero setter '" + makePathStr(path, pathStart) + "\'");
      }
      else if (params.length == 1) {
        setter.invoke(instance, ensureCloning(value));
      }
      else {
        final Field field = findDeclaredFieldForName(instance.getClass(), fieldName);
        if (field != null) {
          setField(instance, field, value);
        }
        else {
          throw new UPomException("Unsupported type for '" + makePathStr(path, pathStart) + "\'");
        }
      }
      return true;
    }
    else {
      // find getter
      final Method getter = findMethod(instance.getClass(), "get" + fieldName, true);
      if (getter == null) {
        throw new UPomException("Can't find model field '" + makePathStr(path, pathStart) + '\'');
      }
      final Object nextInstance = getter.invoke(instance);
      if (nextInstance == null) {
        return false;
      }

      final boolean theNextPathItemIsLastOne = path.length - 1 == pathStart + 1;
      if (nextInstance instanceof Collection) {
        final Type returnType = getter.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
          final ParameterizedType paramType = (ParameterizedType) returnType;
          final Type[] argTypes = paramType.getActualTypeArguments();

          if (theNextPathItemIsLastOne) {
            if (value == null) {
              ((Collection) nextInstance).clear();
              return true;
            }
            else {
              if (value instanceof Collection) {
                ((Collection) nextInstance).clear();
                for (final Object obj : ((Collection) value)) {
                  ((Collection) nextInstance).add(obj);
                }
                return true;
              }
              else {
                ((Collection) nextInstance).clear();
                return ((Collection) nextInstance).add(value);
              }
            }
          }

          final String nextPathItem = path[pathStart + 1].toLowerCase(Locale.ENGLISH);
          if (argTypes[0].toString().toLowerCase(Locale.ENGLISH).endsWith(nextPathItem)) {
            boolean result = false;
            for (final Object collectionItem : (Collection) nextInstance) {
              result |= processPathStepToSet(path, pathStart + 2, collectionItem, value);
            }
            return result;
          }
          else {
            throw new UPomException("Collection element type is not '" + makePathStr(path, pathStart + 1) + '\'');
          }
        }
        else {
          throw new UPomException("Can't find model field '" + makePathStr(path, pathStart) + '\'');
        }
      }
      else if (nextInstance instanceof Map) {
        final Map map = (Map) nextInstance;
        final String nextPathItem = path[pathStart + 1];
        if (theNextPathItemIsLastOne) {
          if (value == null) {
            map.remove(nextPathItem);
          }
          else {
            map.put(nextPathItem, value);
          }
          return true;
        }
        else {
          return map.containsKey(nextPathItem) ? processPathStepToSet(path, pathStart + 2, map.get(nextPathItem), value) : false;
        }
      }
      else {
        return processPathStepToSet(path, pathStart + 1, nextInstance, value);
      }
    }
  }

  private Object processPathStepToGet(final String[] path, final int pathStart, final Object instance) throws Exception {
    final String fieldName = path[pathStart];

    if (pathStart == path.length - 1) {
      // last step
      // find getter
      final Method getter = findMethod(instance.getClass(), "get" + fieldName, true);
      if (getter == null) {
        throw new UPomException("Can't find model field '" + makePathStr(path, pathStart) + '\'');
      }

      final Class[] params = getter.getParameterTypes();
      if (params.length == 0) {
        return ensureCloning(getter.invoke(instance));
      }
      else {
        final Field field = findDeclaredFieldForName(instance.getClass(), fieldName);
        if (field != null) {
          return getField(instance, field);
        }
        else {
          throw new UPomException("Unsupported type for '" + makePathStr(path, pathStart) + "\'");
        }
      }
    }
    else {
      // find getter
      final Method getter = findMethod(instance.getClass(), "get" + fieldName, true);
      if (getter == null) {
        throw new UPomException("Can't find model field '" + makePathStr(path, pathStart) + '\'');
      }
      final Object nextInstance = getter.invoke(instance);
      if (nextInstance == null) {
        return false;
      }

      final boolean theNextPathItemIsLastOne = path.length - 1 == pathStart + 1;

      if (nextInstance instanceof Collection) {
        final Type returnType = getter.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
          final ParameterizedType paramType = (ParameterizedType) returnType;
          final Type[] argTypes = paramType.getActualTypeArguments();

          if (theNextPathItemIsLastOne) {
            // take only the first value
            return ((Collection) nextInstance).isEmpty() ? null : ((Collection) nextInstance).iterator().next();
          }

          final String nextPathItem = path[pathStart + 1].toLowerCase(Locale.ENGLISH);
          if (argTypes[0].toString().toLowerCase(Locale.ENGLISH).endsWith(nextPathItem)) {
            return ((Collection) nextInstance).isEmpty() ? null
                    : processPathStepToGet(path, pathStart + 2, ((Collection) nextInstance).iterator().next());
          }
          else {
            throw new UPomException("Collection element type is not '" + makePathStr(path, pathStart + 1) + '\'');
          }
        }
        else {
          throw new UPomException("Can't find model field '" + makePathStr(path, pathStart) + '\'');
        }
      }
      else if (nextInstance instanceof Map) {
        final Map map = (Map) nextInstance;
        final String nextPathItem = path[pathStart + 1];
        if (theNextPathItemIsLastOne) {
          return map.get(nextPathItem);
        }
        else {
          return map.containsKey(nextPathItem) ? processPathStepToGet(path, pathStart + 2, map.get(nextPathItem)) : null;
        }
      }
      else {
        return processPathStepToGet(path, pathStart + 1, nextInstance);
      }
    }
  }

  public void injectIntoProject(final Log log, final MavenProject project) throws Exception {
    for (final Method setter : project.getClass().getMethods()) {
      final String methodName = setter.getName();
      final Class<?>[] setterParams = setter.getParameterTypes();
      if (setterParams.length == 1 && methodName.startsWith("set")) {
        final String paramName = methodName.substring(3).toLowerCase(Locale.ENGLISH);
        if (paramName.equals("build")) {
          continue;
        }

        Method getter = null;
        for (final Method g : this.model.getClass().getMethods()) {
          final Class<?>[] getterParams = g.getParameterTypes();
          if (getterParams.length == 0 && g.getName().equalsIgnoreCase("get" + paramName)) {
            getter = g;
            break;
          }
        }
        if (getter != null && setterParams[0].isAssignableFrom(getter.getReturnType())) {
          final Object value = getter.invoke(this.model);
          if (value == null) {
            log.debug(getter.getName() + "() X-> " + setter.getName() + "()");
          }
          else {
            log.debug(getter.getName() + "() --> " + setter.getName() + "()");
            setter.invoke(project, getter.invoke(this.model));
          }
        }
      }
    }
  }

  private static String[] splitPath(final String path) {
    final String[] result = path.trim().split("\\/");
    return result;
  }

  public void assignTo(final MavenProject project) {
    project.setOriginalModel(this.model);
  }

  @Override
  public String toString(){
    return this.model.toString();
  }
  
}
