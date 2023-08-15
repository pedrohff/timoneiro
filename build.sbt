name := "sparkhello"
version := "1.0"
scalaVersion := "2.13.11"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.4.1"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.4.1"
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.4.1"
// https://mvnrepository.com/artifact/mysql/mysql-connector-java
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.33"

//libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-10_2.11" % "3.4.1"