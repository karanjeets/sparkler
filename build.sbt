/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import scala.sys.process._

organization := Settings.projectOrganization
maintainer := Settings.projectMaintainer

// Scala/Java Build Options
// spark 2.4.5 now come pre-build with scala 2.12 @ https://archive.apache.org/dist/spark/spark-2.4.5/
scalaVersion in ThisBuild := "2.12.12"
scalacOptions in ThisBuild += "-target:jvm-1.8"
javacOptions in (Compile, doc) in ThisBuild ++= Seq("-source", "1.8")
javacOptions in (Compile, compile) ++= Seq("-target", "1.8")

// Common dependencies
libraryDependencies in ThisBuild ++= Seq(
  Dependencies.pf4j % "provided",
)

developers := List(
  // In alphabetic order
  Developer("user-id",
    "Name",
    "Email",
    url("https://www.apache.org/")
  )
)

lazy val plugins = ProjectRef(file("./"), "plugins")

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    Settings.common,
    name := "sparkler",
    mainClass in Compile := Some("edu.usc.irds.sparkler.Main"),
  )
  .aggregate(api, app, plugins)

lazy val api = (project in file("sparkler-api"))
  .settings(
    Settings.common,
    name := "sparkler-api",
    libraryDependencies ++= Seq(
      Dependencies.jsonSimple exclude("junit", "junit"),
      Dependencies.nutch exclude("*", "*"),
//      Dependencies.Slf4j.api,
//      Dependencies.Slf4j.log4j12,
      Dependencies.snakeYaml,
      Dependencies.Solr.solrj,

      // Test
      Dependencies.jUnit % Test,
      Dependencies.jUnitInterface % Test
    ),
    testOptions += Tests.Argument(TestFrameworks.JUnit,
      "--verbosity=1",
      "--run-listener=edu.usc.irds.sparkler.test.WebServerRunListener")
  )
  .dependsOn(testsBase)

lazy val app = (project in file("sparkler-app"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    Settings.common,
    name := "sparkler-app",
    libraryDependencies ++= Seq(
      // TODO: Only keep necessary dependencies. Rest all should be included as plugin. Eg: extractors
      Dependencies.args4j,
      Dependencies.commonsValidator,
      Dependencies.Jackson.databind exclude("org.slf4j", "slf4j-api"),
      Dependencies.Jackson.core,
      //Dependencies.jsonSimple,
      Dependencies.kafkaClients exclude("org.slf4j", "slf4j-api"),
      //Dependencies.nutch exclude("*", "*"),
      Dependencies.pf4j,
      //Dependencies.Slf4j.api,
      //Dependencies.Slf4j.log4j12,
      //Dependencies.snakeYaml,
      Dependencies.Solr.core,
      //Dependencies.Solr.solrj,
      Dependencies.sparkCore,
      Dependencies.tikaParsers,
    ),
    packageBin in Universal := {
      // Move sparkler-app & its dependencies to {Settings.buildDir}
      val fileMappings = (mappings in Universal).value
      val buildLocation = file(".") / Settings.buildDir / s"${name.value}-${(version in ThisBuild).value}"
      fileMappings foreach {
        case (file, name) => IO.move(file, buildLocation / name)
      }

      // Move conf & bin to {Settings.buildDir}
      IO.copyDirectory(file(".") / Settings.confDir, file(".") / Settings.buildDir / Settings.confDir)
      IO.copyDirectory(file(".") / Settings.binDir, file(".") / Settings.buildDir / Settings.binDir)

      buildLocation
    }
  )
  .dependsOn(api)

lazy val testsBase = (project in file("sparkler-tests-base"))
  .settings(
    Settings.common,
    name := "sparkler-tests-base",
    libraryDependencies ++= Seq(
      Dependencies.Jetty.server,
      Dependencies.Jetty.servlet,
      Dependencies.jUnit,
      Dependencies.Slf4j.api,
      Dependencies.Slf4j.log4j12,
    )
  )


lazy val initGitSubmodule = taskKey[Int]("Initialize Git Submodule")

lazy val ui = (project in file("sparkler-ui"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    Settings.common,
    name := "sparkler-ui",
    autoScalaLibrary := false,
    packageDescription := "Banana Dashboard WAR",
    topLevelDirectory := None,
    initGitSubmodule := {
      val log = streams.value.log
      log.info("Initialize Banana Git Submodule")
      ("git submodule init" #&& "git submodule update").!
    },
    mappings in Universal := {
      val log = streams.value.log
      val initSubmodule: Int = initGitSubmodule.value
      if (initSubmodule != 0) {
        log.error("Banana Git Submodule Initialization Failed")
        System.exit(initSubmodule)
      }
      val bananaMappings: Seq[(File, String)] = Settings.mergeDirs(
        baseDirectory.value / "src" / "banana" / "src",
        baseDirectory.value / "src" / "main" / "java",
        baseDirectory.value / "src" / "main" / "webapp"
      )
      bananaMappings
    },
    packageBin in Universal := {
      val buildLocation: File = baseDirectory.value / "sparkler-dashboard" / s"${name.value}-${(version in ThisBuild).value}.war"
      val packageFile: File = (packageBin in Universal).value
      IO.move(packageFile, buildLocation)
      buildLocation
    }
  )