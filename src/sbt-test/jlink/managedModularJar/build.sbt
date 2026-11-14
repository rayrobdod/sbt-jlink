lazy val jlink_managedModularJar = project
	.in(file("."))
	.enablePlugins(JlinkPlugin, ModulepathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulepath := true,
		libraryDependencies += "org.jsoup" % "jsoup" % "1.20.1" % Modulepath,
		TaskKey[Unit]("verify_jlink_fullModulepath") := Def.uncached {
			val expected = Seq(
				"jsoup-1.20.1.jar",
				"jlink_managedmodularjar_3-0.1.0-SNAPSHOT-modulepath.jar",
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
			(Compile / jlink).value

			def fileContains(subpath: String, expecting: String): Unit =
				val file = (Compile / jlink / target).value / subpath
				val contents = java.nio.file.Files.readString(file.toPath)
				if ! contents.contains(expecting) then
					sys.error(s"`$subpath` contents:\n$contents")

			fileContains("release", "MODULES=\"java.base java.xml org.jsoup main\"")
			fileContains("bin/Main", " -m main/main.Main ")
			if Option(System.getProperty("os.name")).map(_.contains("Windows")).getOrElse(false) then
				fileContains("bin/Main.bat", " -m main/main.Main ")
		},
	)
