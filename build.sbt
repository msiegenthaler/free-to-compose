scalaVersion := "2.11.7"

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.7",
  organization := "ch.inventsoft",
  scalacOptions += "-feature",
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1"),
  baseUrl in versioneye := "https://www.versioneye.com",
  apiPath in versioneye := "/api/v2",
  publishCrossVersion in versioneye := true
)

lazy val core = project.in(file("core")).
  settings(commonSettings: _*).
  settings(
    name := "free-to-compose",
    libraryDependencies += "org.typelevel" %% "cats" % "0.5.0",
    libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.1",
    propertiesPath in versioneye := ".versioneye.properties"
  )
  .enablePlugins(VersionEyePlugin)

lazy val example = project.in(file("example")).
  dependsOn(core).
  settings(commonSettings: _*).
  settings(
    name := "free-to-compose-example"
  )