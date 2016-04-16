# Introduction
Sometime I develop and publish maven plugins in maven central. The Most comfortable way to organize maven plugin testing is to make a pom module hierarchy because maven keeps module build in defined order, also it allows to share common options between modules. But in the case there is some issue, all child modules have link to their parent and the parent must be published also together with children. So I developed the uber-pom plugin to make some around way.

# Changelog
__1.0.1__
 - added flag `removeSiblingDuplications` to find sibling duplications in the result uber pom XML and removing them. By default it is turned off.
 - added support for system property 'upom.delete.on.exit' to override value of 'deleteOnExit' parameter (issue #2)

__1.0__
 - Initial version

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
        <version>1.0.1</version>
        <configuration>
          <remove>
            <section>parent</section>
            <section>modules</section>
            <section>profiles/profile/modules</section>
          </remove>
          <removeSiblingDuplications>true</removeSiblingDuplications>
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
## How to save uber-pom in another place?
The Generated uber-pom by default will be placed in the same folder where the project pom is. You can change that through `<folder>` setting.
```
<configuration>
  <folder>/anotherFolderToSaveUberPom</folder>
</configuration>
```
## I wanna change the result uber-pom name
By default the uber-pom has name `uber-pom.xml` but sometime it is needed to be changed, it is possible through `<name>` setting.
```
<configuration>
  <name>customUberPom.xml</name>
</configuration>
```
## I can't find uber-pom after session
By default the generated and saved uber-pom will be removed after session. If you want to keep the file then disable its deleting with flag `<deleteOnExit>`
```
<configuration>
  <deleteOnExit>false</deleteOnExit>
</configuration>
```
## How to merge only restricted number of hierarchy levels?
By default the plugin merges all hierarchy levels, but you can restrict the number through the `<depth>` parameter
```
<configuration>
  <depth>2</depth>
</configuration>
```
In the example, only two upper levels will be involved into merging.

# Donation   
If you like the software you can make some donation to the author   
[![https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
