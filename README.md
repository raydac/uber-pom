# Introduction
Sometime I develop and publish maven plugins in maven central. The Most comfortable way to organize maven plugin testing is to make a pom module hierarchy because maven keeps module build in defined order, also it allows to share common options between modules. But in the case there is some issue, all child modules have link to their parent and the parent must be published also together with children. So I developed the uber-pom plugin to make some around way.

# How it works?
The Plugin just merging all pom.xml in hierarchy (or only defined depth of the hierarchy) and saves the generated uber-pom into defined place, then it saves link to the file into the current maven project model. It works on the VALIDATE phase. So the result packed artifact will have the uber-pom packed into the result archive.

# May be there is official solution?
May be yes, I have found [pre-released maven-flatten](http://mojo.codehaus.org/flatten-maven-plugin/) which may be doing the same business but I prefer my own solutions.

# How to use?
## Add the plugin in pom.xml
Just add the plugin into pom.xml of the project which needs uber-pom
```
  <build>
    <plugins>
    ...
      <plugin>
        <groupId>com.igormaznitsa</groupId>
        <artifactId>uber-pom</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <configuration>
          <remove>
            <section>parent</section>
            <section>modules</section>
          </remove>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>upom</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    ...
    </plugins>
  </build>
```
__NB! By default the plugin just merging pom models in hierarchy, so you should add `<remove>` section to remove the 'parent' and 'modules' from the result uber-pom. I don't make that automaticaly to keep universality.__
## I want to remove some sections in the result
Just add list of paths to the sections into `<configuration><remove>` and the sections will be removed from the result. 
```
  <configuration>
    <remove>
      <section>developers</section>
      <section>build/plugins</section>
      <section>developers/developer/email</section>
    </remove>
  </configuration>
```
## I want keep some sections unchanged!
Add paths to such sections into `<configuration><keep>`
```
  <configuration>
    <keep>
      <section>developers</section>
      <section>description</section>
    </keep>
  </configuration>
```
The Sections in the result will be the same as in the original project pom.xml.
## How to change value of some pom parameters?
Sometime it is good to change some record in the pom.xml, the plugin allows to do that
```
<configuration>
 <set>
    <property>
      <name>description</name>
      <value>It is new description of the pom.xml</value>
    </property>
    <property>
      <name>developers/developer/email</name>
      <value>newemail@alldevelopers.com</value>
    </property>
  </set>
</configuration>
```
