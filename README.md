# Simple repo

A tiny Maven proxy and repository. This project has the same goal of famous repository managers such as Sonatype OSS Nexus, Apache Archiva and JFrog Artifactory. The major difference is that this repository manager use very few RAM and CPU.

## Why using this library?

* Proxy for other Maven repositories
* Hosts Maven artefacts
* JRE 1.11+
* Memory efficient and super fast
* Need only servlet api 3.0 (works with Tomcat 7+ or Jetty 8+)
* The war is tiny: less than 100 KB
* A nice interface

## How to build

You need **maven** installed.

```bash
    mvn clean package
```

A war archive is generated in the *simple-repo/target* folder. Rename it to *simple-repo.war*.

Deploy the generated **war** archive into a Java Container (Tested with Tomcat 6 and Tomcat 8/9).

## Where are stored the files

The files are stored in **~/.simplerepo**. For storing files in another directory, 
set the variable *simplerepo.root* (-Dsimplerepo.root=/path/to/repo).

### Remove an artifact

You can sefely remove an artifact from **~/.simplerepo** or *simplerepo.root*. As the system does not have a database or a cache. It will not break something.

## Configuration

### Understanding the configuration

There are **3 types of repositories**:
* hosted repository (no url and no includes)
* proxy repository (url and no includes)
* include repository (no url and includes)
The type of repository is no configurable. The system will deduce the type of repository with its configuration.

**maxSnapshots** in 10 by default. This is the maximum number of snapshots by type. If there are more snapshot, the oldest is removed.

**notFoundCache** is the time in microseconds before the system rechecks a remote resource that cannot be found.

### Configure simple-repo

The configuration is accessible through JMX. If you cannot access JMX via JConsole, 
you can use [jmx-web-console](https://github.com/gcolin/jmx-web-console) for configuring JMX via a web interface.

The configuration is also accessible with the links *Settings* in the menu.

### Security

To be able to push an artifact, you must have a user with the role *repo-upload*. 

To be able to configure simple-repo, you must have a user with the role *repo-admin*.

For Tomcat, 
update *TOMCAT_HOME/conf/tomcat-users.xml*. 

For Jetty, see [the instructions](https://wiki.eclipse.org/Jetty/Tutorial/Realms).


### Configure Maven

Open *MAVEN_HOME/conf/settings.xml*, add a mirror
```xml
    <settings>
      ...
      <mirrors>
        ...
         <mirror>
          <id>simplerepo</id>
          <mirrorOf>*</mirrorOf>
          <url>http://localhost:8080/simple-repo/repository/public</url>
        </mirror>
        ...
      </mirrors>
      ...
    <settings>
```

### Uploading artifact

You can upload manually with a command
```bash
    mvn deploy:deploy-file -DgroupId=com.company -DartifactId=project -Dversion=1.0 -DgeneratePom=true -Dpackaging=jar -DrepositoryId=simple-repo-releases -Durl=http://localhost:8080/simple-repo/repository/thirdparty -Dfile=project-1.0.jar
```

### Deploy artifact

In your container, add a user with the role *repo-upload*. For Tomcat, 
update *TOMCAT_HOME/conf/tomcat-users.xml*. For Jetty, see [the instructions](https://wiki.eclipse.org/Jetty/Tutorial/Realms).

Open *MAVEN_HOME/conf/settings.xml*, add a server
```xml
    <settings>
      ...
      <servers>
        ...
        <server>
          <id>simple-repo-snapshots</id>
          <username>userName</username>
          <password>userPassword</password>
        </server>
        <server>
          <id>simple-repo-releases</id>
          <username>userName</username>
          <password>userPassword</password>
        </server>
        ...
      </servers>
      ...
    </settings>
```

In your pom.xml, add
```xml
    <project>
      ...
      <distributionManagement>
            <snapshotRepository>
               <id>simple-repo-snapshots</id>
               <url>http://localhost:8080/simple-repo/repository/snapshots</url>
            </snapshotRepository>
            <repository>
                <id>simple-repo-releases</id>
               <url>http://localhost:8080/simple-repo/repository/releases</url>
            </repository>
        </distributionManagement>
      ...
    </project>
```

And execute the maven deploy command
```bash
    maven deploy
```

