# Introduction
Sometime I develop and publish maven plugins in maven central. The Most comfortable way to organize maven plugin testing is to make a pom module hierarchy because maven keeps module build in defined order, also it allows to share common options between modules. But in the case there is some issue, all child modules have link to their parent and the parent must be published also together with children. So I developed the uber-pom plugin to make some around way.

# How it works?
The Plugin just merging all pom.xml in hierarchy (or only defined depth of the hierarchy) and saves the generated uber-pom into defined place, then it saves link to the file into the current maven project model. It works on the VALIDATE phase. So the result packed artifact will have the uber-pom packed into the result archive.

# May be there is official solution?
May be yes, I have found [pre-released maven-flatten](http://mojo.codehaus.org/flatten-maven-plugin/) which may be doing the same business but I prefer my own solutions.
