#Vine

Vine is package manager for the JVM



##Overview

Vine is a package manager for Maven Central. Simply supply the Maven coordinates (the group, artifactId and version)
of a runnable jar and Vine can download and install it, dependencies included. IE: "vine install org.apache.ivy:ivy:1.9.7"
All you need then is to have ~/.vine/bin on your path and ivy will be instantly available. 

This ability is expanded by a few other techniques. 

* Endorsed packages: Some packages are famous enough to be given shortened nicknames to represent both group and artifactId
IE: "vine install ant:1.9.7"
* Binary packages: Some packages have binary zips/tars that are also available in Maven, so by supplying that info we can
leverage those as well. IE: "vine install org.apache.maven:apache-maven:zip:bin:3.3.9" will install Maven from the zip
and will put all it's scripts in the same folder as normal jar installs.














##Thesis

Many modern languages these days have an officially supported package manager. 
Ruby has Gem, Python Pip, Nodejs has npm, so on and so forth. The JVM almost seems
alone in not being bundled with a package manager. Now for sure Java has a whole zoo
of unofficial but widely used build tools, from Ant + Ivy and Maven to Gradle, SBT, Leiningen 
and beyond, but these are all strictly build tools. None of them serve the dual propose
of being package managers as well. The closest the JVM has is SDKMAN, which granted is
a very good tool, it quite limited. All packages available must be approved and specially 
cared for. While that's great for highly ranked packages and ensures only high-quality 
project will be available, it limits what a single individual can do alone. For example, 
anyone can upload a handy cli program to npm any time they like and anyone else in the world
can install it straight away, without any human intervention along the way. Npm and others
leverage the exact same tools used for dependency management for installing programs as well,
and that is a very powerful combination not found in the JVM world. 

Vine is an attempt to fix that. Not by reinventing the packaging, but by leveraging the 
existing resources we already have available: Maven Central. Perhaps the biggest and most
important code repository in the world in terms of quality and volume of code and use. So
many great tools are already out there, sitting in Maven repos just waiting for the right
tool to come along and allow you to download them directly. That is what Vine is for. 
With a simple string "vine install org.apache.ivy:ivy:1.9.7" I can install Apache Ivy 
without any fuss. After all, Ivy is in Maven Central, it's a runnable Jar with a main
method, a manifest that specifies it, it doesn't even have any dependencies. And if it did
have dependencies, like say, JGit does, there is no reason we can't use the maven pom info
already provided to us to download them as well. 














