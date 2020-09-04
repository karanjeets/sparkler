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

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal
import sbt._
import sbt.Keys._
import sbt.Resolver.mavenLocal
import sbtassembly.AssemblyPlugin.autoImport._

object Settings {
  lazy val organization = "edu.usc.irds.sparkler"
  lazy val version = "0.2.1-SNAPSHOT"
  lazy val maintainer = "irds-l@mymaillists.usc.edu"
  lazy val buildDir = "build"
  lazy val pluginsDir = "plugins"
  lazy val common = Seq(
    autoScalaLibrary := false,
    scalacOptions ++=  Seq(
      "-unchecked",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-deprecation",
      "-encoding",
      "utf8"
    ),
    resolvers ++= Seq(
      mavenLocal,
      "Typesafe Releases" at "https://repo.typesafe.com/typesafe/ivy-releases/",
      "Restlet Repository" at "https://maven.restlet.com/",
      "JBoss Repository" at "https://repository.jboss.org/nexus/content/repositories/",
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Scala-Tools Snapshots" at "https://scala-tools.org/repo-snapshots/"
    )
  )
  lazy val plugin = common ++ baseAssemblySettings ++ Seq(
      //assemblyJarName in assembly := s"${name.value}-${Settings.version}.jar",
      assemblyOutputPath in assembly := file(".") / buildDir / pluginsDir / s"${name.value}-${Settings.version}.jar",
      mappings in Universal := {
        val universalMappings = (mappings in Universal).value
        val fatJar = (assembly in Compile).value
        universalMappings :+ (fatJar -> ("lib/" + fatJar.getName))
      },
      // Discard META-INF as it's a very common conflict
      // Others, keep the first copy on merge
      assemblyMergeStrategy in assembly := {
        case PathList("META-INF", _ @ _*) => MergeStrategy.discard
        case _ => MergeStrategy.last
      }
  )

  def pluginManifest(id: String, className: String,
                     dependencies: List[String]): Seq[Def.Setting[Task[Seq[PackageOption]]]] = {
    Seq(
      packageOptions in (Compile, packageBin) += Package.ManifestAttributes(
        "Plugin-Id" -> id,
        "Plugin-Class" -> className,
        "Plugin-Version" -> version,
        "Plugin-Provider" -> organization,
        "Plugin-Dependencies" -> dependencies.mkString(",")
      )
    )
  }
}