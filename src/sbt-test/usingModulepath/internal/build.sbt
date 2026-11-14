lazy val base = project
	.in(file("Base"))
	.enablePlugins(ModulepathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulepath := true,
	)

lazy val dependent = project
	.in(file("Dependent"))
	.dependsOn(base, base % "modulepath->modulepath")
	.enablePlugins(ModulepathPlugin)
	.settings(
		autoScalaLibrary := false,
		TaskKey[Unit]("verify_dependencyModulepath") := Def.uncached {
			val expected = Seq(
				"base_3-0.1.0-SNAPSHOT-modulepath.jar",
			)
			val modules = (Compile / dependencyModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("dependencyModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_dependencyClasspath") := Def.uncached {
			val expected = Seq(
			)
			val modules = (Compile / dependencyClasspath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("dependencyClasspath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_fullModulepath") := Def.uncached {
			val expected = Seq(
				"base_3-0.1.0-SNAPSHOT-modulepath.jar",
			)
			val modules = (Compile / fullModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_fullClasspath") := Def.uncached {
			val expected = Seq(
				"dependent_3-0.1.0-SNAPSHOT.jar",
			)
			val modules = (Compile / fullClasspath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullClasspath did not match expected:\n  ", "\n  ", ""))
		},
	)
