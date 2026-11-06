lazy val base = project
	.in(file("Base"))
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		libraryDependencies += "org.jsoup" % "jsoup" % "1.20.1" % ModulePath,
	)

lazy val dependent = project
	.in(file("Dependent"))
	.dependsOn(base)
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		TaskKey[Unit]("verify_modulepath") := Def.uncached {
			val modules = (Compile / fullModulepath).value
			if modules.size != 1 then
				throw new Exception("" + modules)
		},
	)
