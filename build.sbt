lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.7",
  organization := "ch.inventsoft",
  scalacOptions += "-feature",
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
)

lazy val core = project.in(file("core")).
  settings(commonSettings: _*).
  settings(
    name := "free-to-compose",
    libraryDependencies += "org.spire-math" %% "cats" % "0.2.0",
    libraryDependencies += "com.chuusai" %% "shapeless" % "2.2.5"
  )

lazy val example = project.in(file("example")).
  dependsOn(core).
  settings(commonSettings: _*).
  settings(
    name := "free-to-compose-example"
  )