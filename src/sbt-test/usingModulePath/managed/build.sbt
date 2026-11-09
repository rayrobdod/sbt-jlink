lazy val dependent = project
	.in(file("."))
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		libraryDependencies += "org.jsoup" % "jsoup" % "1.20.1" % ModulePath,
		TaskKey[Unit]("verify_dependencyModulepath") := Def.uncached {
			val expected = Seq(
				"jsoup-1.20.1.jar",
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
				"jsoup-1.20.1.jar",
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
