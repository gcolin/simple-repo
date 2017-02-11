# Simple repo

A tiny Maven proxy and repository. This project has the same goal of famous repository managers such as Sonatype OSS Nexus, Apache Archiva and JFrog Artifactory. The major difference is that this repository manager can handle huge repositories without sacrifiying your RAM or your CPU. *Simple repo* uses some old and dirty technologies that makes it fast and Java container friendly.

## Why using this library?

* Proxy for other Maven repositories
* Hosts Maven artefacts
* JRE 1.7+
* Memory efficient and super fast
* Need only servlet api 3.0 (works with Tomcat 7+ or Jetty 8+)
* Can export a hosted repository to a static HTML web site
* A minimal version sized less than 100 KB
* A nice interface

## How to build

You need **maven** installed.

```bash
    mvn clean package
```

For the minimal version
```bash
    mvn clean package -P !full
```

the minimal version with Linux
```bash
    mvn clean package -P \!full
```

A war archive is generated in the *simple-repo/target* folder. Rename it to *simple-repo.war*.

Deploy the generated **war** archive into a Java Container (Tested with Tomcat 6 and Tomcat 8).

For generating the maven site in *target/site* directory.

```bash
    mvn site
```

## Where are stored the files

The files are stored in **~/.simplerepo**. For storing files in another directory, 
set the variable *simplerepo.root* (-Dsimplerepo.root=/path/to/repo).

## Configuration

### Configure simple-repo

The configuration is accessible through JMX. If you cannot access JMX via JConsole, 
you can use [jmx-web-console](https://github.com/gcolin/jmx-web-console) for configuring JMX via a web interface.

The configuration is accessible with the links *Repositories* and *Plugins* in the menu. In your container, add a user with the role *repo-admin*. For Tomcat, 
update *TOMCAT_HOME/conf/tomcat-users.xml*. For Jetty, see [the instructions](https://wiki.eclipse.org/Jetty/Tutorial/Realms).

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

## Proxy another type of repository

Simple-repo acts as a stupid proxy and stores as a stupid secured ftp like server. So, maybe other repository types can work too.

## Plugins

Plugins  
* can be disabled 
* are optional
* are independant from each other

You can build your own version or *Simple repo* by commenting some plugin dependencies in *simple-repo/pom.xml*.

## Package search

The search functionnality is provided by a plugin installed by default with the standard version. It uses an embedded Apache Derby database and can handle a very important number of artifacts without impacting the RAM. The display of an artifact is close to mvnrepository.

A button *reindex* clear and rebuild the database.

## Theme

The application uses the Bootstrap 3 standards and you can choose amoung various theme of Bootswatch.

## Extracting a repository

The application can export a repository (for example the *release* repository) to a static HTML website with a search functionality coded in JavaScript. With this functionality, you can create your own online repository with a simple HTML file hosting.

For starting exporting, go to the *Plugins* page.