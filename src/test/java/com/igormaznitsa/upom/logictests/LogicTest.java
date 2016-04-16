package com.igormaznitsa.upom.logictests;

import com.igormaznitsa.upom.UPomException;
import com.igormaznitsa.upom.UPomModel;
import java.io.File;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LogicTest extends AbstractLogicTest {
  @Test
  public void testThreeLevelMerging() throws Exception {
    final File base = getFolder("threeLevels");
    
    final UPomModel model1 =  new UPomModel(new File(base,"pom1.xml"));
    final UPomModel model2 =  new UPomModel(new File(base,"pom2.xml"));
    final UPomModel model3 =  new UPomModel(new File(base,"pom3.xml"));
    
    final UPomModel result = model1.merge(model2).merge(model3);

    assertEquals(1,result.getModel().getLicenses().size());
    assertEquals(1,result.getModel().getProfiles().size());
    
    assertEquals(4,result.getModel().getDependencies().size());
    assertEquals(3,result.getModel().getDevelopers().size());
    assertEquals(9,result.getModel().getModules().size());
    
    assertEquals("test.group",result.getModel().getGroupId());
    assertEquals("test-artifact3",result.getModel().getArtifactId());
    assertEquals("1.2.3-SNAPSHOT",result.getModel().getVersion());
    assertEquals("jar",result.getModel().getPackaging());
    assertEquals("parent3",result.getModel().getParent().getArtifactId());
    
    assertNull(result.getModel().getScm());
    assertNull(result.getModel().getReporting());
    assertEquals(0,result.getModel().getRepositories().size());
    
    assertEquals(3,result.getModel().getProperties().size());
  }

  @Test
  public void testThreeLevel_SaveAndRestoreState() throws Exception {
    final File base = getFolder("threeLevels");
    
    final UPomModel model1 =  new UPomModel(new File(base,"pom1.xml"));
    final UPomModel model2 =  new UPomModel(new File(base,"pom2.xml"));
    final UPomModel model3 =  new UPomModel(new File(base,"pom3.xml"));

    model1.saveState("dependencies","modules");
    final UPomModel result = model1.merge(model2).merge(model3);
    model1.restoreState();
    
    assertEquals(2,result.getModel().getDependencies().size());
    assertEquals("artifact1",result.getModel().getDependencies().get(0).getArtifactId());
    assertEquals("artifact2",result.getModel().getDependencies().get(1).getArtifactId());
    
    assertEquals(3,result.getModel().getDevelopers().size());
    
    assertEquals(3,result.getModel().getModules().size());
    assertEquals("modul1", result.getModel().getModules().get(0));
    assertEquals("modul2", result.getModel().getModules().get(1));
    assertEquals("modul3", result.getModel().getModules().get(2));
    
    assertEquals("test.group",result.getModel().getGroupId());
    assertEquals("test-artifact3",result.getModel().getArtifactId());
    assertEquals("1.2.3-SNAPSHOT",result.getModel().getVersion());
    assertEquals("jar",result.getModel().getPackaging());
    assertEquals("parent3",result.getModel().getParent().getArtifactId());
    
    assertNull(result.getModel().getScm());
    assertNull(result.getModel().getReporting());
    assertEquals(0,result.getModel().getRepositories().size());
  }

  @Test
  public void testAsXML() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    final String xml = model1.asXML(null,false);
    
    assertNotNull(xml);
    assertTrue(xml.length()>100);
    assertTrue(xml.contains("</project>"));
  }
  
  @Test
  public void testThreeLevel_RemoveEmailOfDevelopers() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    final UPomModel model2 = new UPomModel(new File(base, "pom2.xml"));
    final UPomModel model3 = new UPomModel(new File(base, "pom3.xml"));

    final UPomModel result = model1.merge(model2).merge(model3);
    result.remove("developers/developer/email");
    
    assertEquals(3, result.getModel().getDevelopers().size());
    assertNull(result.getModel().getDevelopers().get(0).getEmail());
    assertNull(result.getModel().getDevelopers().get(1).getEmail());
    assertNull(result.getModel().getDevelopers().get(2).getEmail());
  }

  @Test
  public void testThreeLevel_KeepEmailForAllDevelopers() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    final UPomModel model2 = new UPomModel(new File(base, "pom2.xml"));
    final UPomModel model3 = new UPomModel(new File(base, "pom3.xml"));

    model1.saveState("developers/developer/email");
    final UPomModel result = model1.merge(model2).merge(model3);
    model1.restoreState();
    
    assertEquals(3, result.getModel().getDevelopers().size());
    assertEquals("email1@email",result.getModel().getDevelopers().get(0).getEmail());
    assertEquals("email1@email",result.getModel().getDevelopers().get(1).getEmail());
    assertEquals("email1@email",result.getModel().getDevelopers().get(2).getEmail());
  }

  @Test
  public void testThreeLevel_KeepDependency() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    final UPomModel model2 = new UPomModel(new File(base, "pom2.xml"));
    final UPomModel model3 = new UPomModel(new File(base, "pom3.xml"));

    model1.saveState("dependencies/dependency");
    final UPomModel result = model1.merge(model2).merge(model3);
    model1.restoreState();
    
    assertEquals(1, result.getModel().getDependencies().size());
    assertEquals("com.test", result.getModel().getDependencies().get(0).getGroupId());
    assertEquals("artifact1", result.getModel().getDependencies().get(0).getArtifactId());
    assertEquals("1.0.0", result.getModel().getDependencies().get(0).getVersion());
    assertEquals("test", result.getModel().getDependencies().get(0).getScope());
  }

  @Test
  public void testPath_SetGet() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    model1.set("parent/version", "testparent872364");
    assertEquals("testparent872364",model1.get("parent/version"));
    assertEquals("testparent872364",model1.getModel().getParent().getVersion());
    assertNull(model1.get("url"));
    assertArrayEquals(new String[]{"modul1","modul2","modul3"}, ((Collection)model1.get("modules")).toArray());
  }

  @Test
  public void testPath_GetFromCollection() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    assertEquals("modul1",model1.get("modules/module"));
  }

  @Test
  public void testPath_GetFrom_PrerequisitesMaven() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    assertEquals("3.0.5",model1.get("prerequisites/maven"));
  }

  @Test
  public void testPath_GetFrom_Property() throws Exception {
    final File base = getFolder("threeLevels");
    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    assertEquals(null,model1.get("properties/property.number000"));
    assertEquals("Hello",model1.get("properties/property.number1"));
  }

  @Test
  public void testPath_SetTo_Property() throws Exception {
    final File base = getFolder("threeLevels");
    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    assertEquals(null,model1.get("properties/property.number000"));
    model1.set("properties/property.number000", "HAHAHA");
    assertEquals("HAHAHA",model1.get("properties/property.number000"));
  }

  @Test
  public void testPath_SetTo_PrerequisitesMaven() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    model1.set("prerequisites/maven","1.2.3");
    assertEquals("1.2.3",model1.get("prerequisites/maven"));
  }

  @Test(expected = UPomException.class)
  public void testSet_WrongPathOnStart() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    model1.set("pardent/version", "testparent872364");
  }

  @Test(expected = UPomException.class)
  public void testSet_WrongPathAtEnd() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    model1.set("parent/verbsion", "testparent872364");
  }

  @Test(expected = UPomException.class)
  public void testGet_WrongPathOnStart() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    model1.get("pardent/version");
  }

  @Test(expected = UPomException.class)
  public void testGet_WrongPathAtEnd() throws Exception {
    final File base = getFolder("threeLevels");

    final UPomModel model1 = new UPomModel(new File(base, "pom1.xml"));
    model1.get("parent/verbsion");
  }

}
