# Simple repo

A tiny Maven proxy and repository.

## Why using this library?

* Proxy for Maven repositories
* Host Maven artefacts
* Very fast
* JRE 1.6+
* Very small (minimal war size about 32 KB)
* Small footprint
* No database
* Configuration interface via JMX (easy to secure)
* Need only servlet api 2.4 (can run on old and new containers)

## How to build

You need **maven** installed.

```bash
    mvn clean package
```

A war archive is generated in the *target* folder. Rename it to *simple-repo.war*.

Deploy the generated **war** archive into a Java Container (Tested with Tomcat 6 and Tomcat 8).

For generating the maven site in *target/site* directory.

```bash
    mvn site
```

## Where are stored the files

The files are stored in **~/.simplerepo**. For storing files in another directory, 
set the variable *simplerepo.root* (-Dsimplerepo.root=/path/to/repo).

## Configure Maven

Open *MAVEN_HOME/conf/settings.xml*, add a mirror
```xml
    <settings>
      ...
      <mirrors>
        ...
         <mirror>
          <id>simplerepo</id>
          <mirrorOf>*</mirrorOf>
          <url>http://localhost:8080/simple-repo/maven/public</url>
        </mirror>
        ...
      </mirrors>
      ...
    <settings>
```

## Uploading artifact

You can upload manually with a command
```bash
    mvn deploy:deploy-file -DgroupId=com.company -DartifactId=project -Dversion=1.0 -DgeneratePom=true -Dpackaging=jar -DrepositoryId=simple-repo-releases -Durl=http://localhost:8080/simple-repo/maven/thirdparty -Dfile=project-1.0.jar
```

## Deploy artifact

In your container, add a user with the role *manager-upload*. For Tomcat, 
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
               <url>http://localhost:8080/simple-repo/maven/snapshots</url>
            </snapshotRepository>
            <repository>
                <id>simple-repo-releases</id>
               <url>http://localhost:8080/simple-repo/maven/releases</url>
            </repository>
        </distributionManagement>
      ...
    </project>
```

And execute the maven deploy command
```bash
    maven deploy
```
