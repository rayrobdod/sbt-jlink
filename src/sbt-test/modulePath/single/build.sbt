val verify = TaskKey[Unit]("verify")

lazy val manual = project
	.in(file("."))
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		libraryDependencies += "org.jsoup" % "jsoup" % "1.20.1" % ModulePath,
	)
