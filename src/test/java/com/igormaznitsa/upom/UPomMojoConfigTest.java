package com.igormaznitsa.upom;

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
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.*;
import static org.junit.Assert.*;
import org.junit.Test;

public class UPomMojoConfigTest extends AbstractMojoTestCase {

  private UPomMojo init(final File config) throws Exception {
    final MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
    final ProjectBuildingRequest buildingRequest = executionRequest.getProjectBuildingRequest();
    final ProjectBuilder projectBuilder = this.lookup(ProjectBuilder.class);
    final MavenProject project = projectBuilder.build(config, buildingRequest).getProject();
    final UPomMojo mojo = (UPomMojo) this.lookupConfiguredMojo(project, "upom");
    return mojo;
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  public void testDefaultConfig() throws Exception {
    final File pom = getTestFile("src/test/resources/com/igormaznitsa/upom/testcfgs/testDefaultConfig.xml");
    assertNotNull(pom);
    assertTrue(pom.exists());
    
    final UPomMojo myMojo = init(pom);
    assertNotNull(myMojo);

    assertTrue(myMojo.isDeleteOnExit());
    assertNull(myMojo.getKeep());
    assertNull(myMojo.getRemove());
    assertNotNull(myMojo.getFolder());
    assertEquals("uber-pom.xml", myMojo.getName());
    assertEquals(-1, myMojo.getDepth());
  }

  @Test
  public void testNonDefaultConfig() throws Exception {
    final File pom = getTestFile("src/test/resources/com/igormaznitsa/upom/testcfgs/testNonDefaultConfig.xml");
    assertNotNull(pom);
    assertTrue(pom.exists());

    final UPomMojo myMojo = init(pom);

    assertFalse(myMojo.isDeleteOnExit());
    assertArrayEquals(new String[]{"keep/keep1", "keep1/keep2"}, myMojo.getKeep());
    assertArrayEquals(new String[]{"path/path1", "path1/path2"}, myMojo.getRemove());
    assertEquals("/test/folder", myMojo.getFolder().getAbsolutePath());
    assertEquals("testName.xml", myMojo.getName());
    assertEquals(678, myMojo.getDepth());

  }

}
