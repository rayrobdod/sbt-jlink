lazy val base = project
	.in(file("."))
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		scalaVersion := "3.9.0",
		libraryDependencies ++= Seq(
			"org.scalameta" %% "munit" % "1.3.1" % Test,
			"org.scala-lang" % "scala3-library_3" % "3.9.0" % Test,
		),
		Compile / exportToModulePath := true,
		TaskKey[Unit]("verify_Compile_fullModulepath") := Def.uncached {
			val expected = Seq(
				"base_3-0.1.0-SNAPSHOT-modulePath.jar",
			)
			val modules = (Compile / fullModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_Compile_fullClasspath") := Def.uncached {
			val expected = Seq(
			)
			val modules = (Compile / fullClasspath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullClasspath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_Test_fullModulepath") := Def.uncached {
			val expected = Seq(
			)
			val modules = (Test / fullModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_Test_fullClasspath") := Def.uncached {
			val expected = Seq(
				"base_3-0.1.0-SNAPSHOT-tests.jar",
				"base_3-0.1.0-SNAPSHOT-modulePath.jar",
				"munit_3-1.3.1.jar",
				"scala3-library_3-3.9.0.jar",
				"junit-interface-1.3.1.jar",
				"munit-diff_3-1.3.1.jar",
				"junit-4.13.2.jar",
				"scala-library-3.9.0.jar",
				"test-interface-1.0.jar",
				"hamcrest-core-1.3.jar",
			)
			val modules = (Test / fullClasspath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullClasspath did not match expected:\n  ", "\n  ", ""))
		},
	)
