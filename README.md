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

## How to install

You need **maven** installed.

```bash
    mvn install
```

Deploy the generated **war** archive into a Java Container (Tested with Tomcat 6 and Tomcat 8).

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
      <url>http://localhost:8080/simple-repo/content/repositories/public</url>
    </mirror>
    ...
  </mirrors>
  ...
<settings>
```

## Uploading artifact

For uploading artifact, you need to create the directory structure manually and 
add the files manually. The other solution is to get the source and to deploy the
 artifact on an hosted repository.

## Deploy artifact

No security is enabled, everyone can deploy an artifact on an hosted repository.