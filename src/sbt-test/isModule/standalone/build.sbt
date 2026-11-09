lazy val isModule_standalone = project
	.in(file("."))
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulePath := true,
		TaskKey[Unit]("verify_discoveredModules") := Def.uncached {
			val expected = Seq("name.rayrobdod.sbtJlink.sbtTest.isModule.standalone")
			val actual = (Compile / discoveredModules).value
			if (expected != actual) sys.error(s"unexpected discoveredModuleNames\n  Expected $expected\n  Actual   $actual")
		},
	)
