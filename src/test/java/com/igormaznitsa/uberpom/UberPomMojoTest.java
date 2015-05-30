package com.igormaznitsa.uberpom;

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


import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Assert;
import org.junit.Test;

public class UberPomMojoTest extends AbstractMojoTestCase {

//  @Override
//  protected void setUp() throws Exception {
//    super.setUp();
//  }
//
//  @Override
//  protected void tearDown() throws Exception {
//    super.tearDown();
//  }
//
//  public void testDefaultConfig() throws Exception {
//    final File pom = getTestFile("src/test/resources/com/igormaznitsa/jute/testcfgs/testDefaultConfig.xml");
//    assertNotNull(pom);
//    assertTrue(pom.exists());
//
//    final UberPomMojo myMojo = (UberPomMojo) lookupMojo("pom", pom);
//    assertNotNull(myMojo);
//  }
//
//  public void testNonDefaultConfig() throws Exception {
//    final File pom = getTestFile("src/test/resources/com/igormaznitsa/jute/testcfgs/testNonDefaultConfig.xml");
//    assertNotNull(pom);
//    assertTrue(pom.exists());
//
//    final UberPomMojo myMojo = (UberPomMojo) lookupMojo("pom", pom);
//    assertNotNull(myMojo);
//  }

      final List<String> d = new ArrayList<String>();
  
      public List<String> getTTT(){
        return d;
      }
      
    @Test
    public void testTest(){
      for (Method method : UberPomMojoTest.class.getMethods()) {
        Class returnClass = method.getReturnType();
        if (Collection.class.isAssignableFrom(returnClass)) {
          Type returnType = method.getGenericReturnType();
          if (returnType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) returnType;
            Type[] argTypes = paramType.getActualTypeArguments();
            if (argTypes.length > 0) {
              System.out.println("Generic type is " + argTypes[0]);
            }
          }
        }
      }
    }
  
}
