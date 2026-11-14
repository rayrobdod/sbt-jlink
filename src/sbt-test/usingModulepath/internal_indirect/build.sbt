lazy val first = project
	.in(file("First"))
	.enablePlugins(ModulepathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulepath := true,
	)

lazy val second = project
	.in(file("Second"))
	.dependsOn(first, first % "modulepath->modulepath")
	.enablePlugins(ModulepathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulepath := true,
	)

lazy val third = project
	.in(file("Third"))
	.dependsOn(second, second % "modulepath->modulepath")
	.enablePlugins(ModulepathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulepath := true,
		TaskKey[Unit]("verify_dependencyModulepath") := Def.uncached {
			val expected = Seq(
				"second_3-0.1.0-SNAPSHOT-modulepath.jar",
				"first_3-0.1.0-SNAPSHOT-modulepath.jar",
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
				"second_3-0.1.0-SNAPSHOT-modulepath.jar",
				"first_3-0.1.0-SNAPSHOT-modulepath.jar",
				"third_3-0.1.0-SNAPSHOT-modulepath.jar",
			)
			val modules = (Compile / fullModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_fullClasspath") := Def.uncached {
			val expected = Seq(
			)
			val modules = (Compile / fullClasspath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullClasspath did not match expected:\n  ", "\n  ", ""))
		},
	)
