lazy val scalaLibraryModule = project
	.in(file(".sbt/scalaLibraryModule"))
	.enablePlugins(ClasspathToModulePlugin)
	.settings(
		autoScalaLibrary := true,
		scalaVersion := "3.9.0",
	)

lazy val dependent = project
	.in(file("Dependent"))
	.dependsOn(scalaLibraryModule % "modulePath->modulePath")
	.enablePlugins(JlinkPlugin, ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulePath := true,
		TaskKey[Unit]("verify_jlink_fullModulepath") := Def.uncached {
			val expected = Seq(
				"scalalibrarymodule_3-0.1.0-SNAPSHOT-classpathToModule.jar",
				"dependent_3-0.1.0-SNAPSHOT-modulePath.jar",
			)
			val modules = (Compile / jlink / fullModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_jlink_jlinkLaunchers") := Def.uncached {
			val expected = Seq(
				JlinkLauncher("Main", "main", Some("main.Main"))
			)
			val actual = (Compile / jlink / jlinkLaunchers).value

			if expected != actual then
				sys.error(actual.mkString("jlinkLaunchers did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_jlink") := Def.uncached {
			val _ = (Compile / jlink).value

			def fileContains(subpath: String, expecting: String): Unit =
				val file = (Compile / jlink / target).value / subpath
				val contents = java.nio.file.Files.readString(file.toPath)
				if ! contents.contains(expecting) then
					sys.error(s"`$subpath` contents:\n$contents")

			fileContains("release", "MODULES=\"java.base jdk.unsupported scala.library main\"")
			fileContains("bin/Main", " -m main/main.Main ")
			fileContains("bin/Main.bat", " -m main/main.Main ")
		},
	)
