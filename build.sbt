name := "gitbucket-redmine-plugin"
organization := "io.github.gitbucket"
version := "0.0.1"
scalaVersion := "2.12.8"
gitbucketVersion := "4.31.0"

libraryDependencies ++= Seq(
  "com.taskadapter" % "redmine-java-api" % "3.1.2",
  "com.lihaoyi" %% "requests" % "0.1.7"
)