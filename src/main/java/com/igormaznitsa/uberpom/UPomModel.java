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
package com.igormaznitsa.uberpom;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.merge.ModelMerger;
import org.apache.maven.project.MavenProject;

public final class UPomModel {
  
  private final Model model;
  private final Map<String, Object> savedValues = new HashMap<String, Object>();
  
  public UPomModel(final File modelFile) throws Exception {
    final FileInputStream in = new FileInputStream(modelFile);
    try {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      this.model = reader.read(in,true);
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

  public void merge(final UPomModel other) throws Exception {
    final ModelMerger merger = new ModelMerger();
    merger.merge(this.model, other.model, true, null);
  }
  
  public boolean remove(final String removePath) throws Exception {
    return processPathStepToSet(splitPath(removePath), 0, this.model, null);
  }
  
  private static Method findMethod(final Class<?> klazz, final String methodName, final boolean onlyPublic) {
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
  
  private static Field findDeclaredFieldForName(final Class<?> klazz, final String fieldName) {
    Field result = null;
    for (final Field m : klazz.getDeclaredFields()) {
      if (m.getName().equalsIgnoreCase(fieldName)) {
        result = m;
        break;
      }
    }
    if (result != null) {
      result.setAccessible(true);
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
  
  private static Collection<?> cloneCollection(final Collection<?> collection) throws Exception {
    final Class collectionClass = collection.getClass();
    final Constructor constructor = collectionClass.getConstructor(Collection.class);
    return (Collection<?>) constructor.newInstance(collection);
  }
  
  private static Map<?, ?> cloneMap(final Map<?, ?> map) throws Exception {
    final Class mapClass = map.getClass();
    final Constructor constructor = mapClass.getConstructor(Map.class);
    return (Map<?, ?>) constructor.newInstance(map);
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
      
      final Class<?>[] params = setter.getParameterTypes();
      if (params.length == 0) {
        throw new UPomException("Detected zero setter '" + makePathStr(path, pathStart) + "\'");
      }
      else if (params.length == 1) {
        setter.invoke(instance, ensureCloning((Object) value));
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
      
      if (nextInstance instanceof Collection) {
        final Type returnType = getter.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
          final ParameterizedType paramType = (ParameterizedType) returnType;
          final Type[] argTypes = paramType.getActualTypeArguments();
          
          final boolean itemsAreLastInPath = path.length - 1 == pathStart + 1;
          
          if (itemsAreLastInPath) {
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
                throw new UPomException("Can't set the value to multiple collection items '" + makePathStr(path, pathStart + 1) + '\'');
              }
            }
          }
          
          final String nextPathItem = path[pathStart + 1].toLowerCase(Locale.ENGLISH);
          if (argTypes[0].getTypeName().toLowerCase(Locale.ENGLISH).endsWith(nextPathItem)) {
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
      
      final Class<?>[] params = getter.getParameterTypes();
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
      
      if (nextInstance instanceof Collection) {
        final Type returnType = getter.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
          final ParameterizedType paramType = (ParameterizedType) returnType;
          final Type[] argTypes = paramType.getActualTypeArguments();
          
          final boolean itemsAreLastInPath = path.length - 1 == pathStart + 1;
          
          if (itemsAreLastInPath) {
            return new ArrayList(((Collection) nextInstance));
          }
          
          final String nextPathItem = path[pathStart + 1].toLowerCase(Locale.ENGLISH);
          if (argTypes[0].getTypeName().toLowerCase(Locale.ENGLISH).endsWith(nextPathItem)) {
            final Collection result = new ArrayList();
            for (final Object collectionItem : (Collection) nextInstance) {
              result.add(processPathStepToGet(path, pathStart + 2, collectionItem));
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
      else {
        return processPathStepToGet(path, pathStart + 1, nextInstance);
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

}
