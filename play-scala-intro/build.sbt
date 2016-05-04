

name := """play-scala-intro"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
)

//libraryDependencies += "org.apache.spark" %% "spark-core" % "1.4.1"
//libraryDependencies += "org.apache.spark" % "spark-hive_2.10" % "1.4.1"
//libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "1.2.0-cdh5.3.2" % "provided"


resolvers ++= Seq(
  Resolver.mavenLocal,
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  Resolver.url("jets3t", url("http://www.jets3t.org/maven2"))(Resolver.ivyStylePatterns),
  Resolver.url("Edulify Repository", url("http://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns)
)

// libraryDependencies += "org.apache.spark" %% "spark-sql" % "1.2.1"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4" force(),
  "org.apache.spark"  %% "spark-core"              % "1.6.0",
  "org.apache.spark"  %% "spark-sql"              % "1.6.0",
  "com.typesafe.akka" %% "akka-actor"              % "2.4.2" exclude("com.fasterxml.jackson.core", "jackson-databind"),
  "com.typesafe.akka" %% "akka-slf4j"              % "2.4.2" exclude("com.fasterxml.jackson.core", "jackson-databind"),
  "com.gilt" % "jerkson_2.11" % "0.6.6"
)

//resolvers ++= Seq(
//  Resolver.mavenLocal,
//  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases" (http://dl.bintray.com/scalaz/releases%27) ,
//  "Scala-Tools Maven2 Repository" at "http://scala-tools.org/repo-releases" (http://scala-tools.org/repo-releases%27) ,
//  "Java.net repository" at "http://download.java.net/maven/2" (http://download.java.net/maven/2%27) ,
//  "GeoTools" at "http://download.osgeo.org/webdav/geotools" (http://download.osgeo.org/webdav/geotools%27) ,
//  "Apache" at "https://repository.apache.org/service/local/repositories/releases/content" (https://repository.apache.org/service/local/repositories/releases/content%27) ,
//  "Cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/" (https://repository.cloudera.com/artifactory/cloudera-repos/%27) ,
//  "OpenGeo Maven Repository" at "http://repo.opengeo.org" (http://repo.opengeo.org%27) ,
//  "Typesafe" at "https://repo.typesafe.com/typesafe/releases/" (https://repo.typesafe.com/typesafe/releases/%27) ,
//  "Spray Repository" at "http://repo.spray.io" (http://repo.spray.io%27)
// "com.codahale" %% "jerkson_2.10"  % "0.5.0"
//)