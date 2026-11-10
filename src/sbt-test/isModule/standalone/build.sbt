lazy val isModule_standalone = project
	.in(file("."))
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulePath := true,
		TaskKey[Unit]("verify_discoveredAddModules") := Def.uncached {
			val expected = Option("name.rayrobdod.sbtJlink.sbtTest.isModule.standalone")
			val actual = (Compile / discoveredAddModules).value
			if (expected != actual) sys.error(s"unexpected discoveredModuleNames\n  Expected $expected\n  Actual   $actual")
		},
	)
