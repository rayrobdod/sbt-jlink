lazy val jlink_standaloneJmod = project
	.in(file("."))
	.enablePlugins(JlinkPlugin, JmodPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulepath := true,
		TaskKey[Unit]("verify_jlink_fullModulepath") := Def.uncached {
			val expected = Seq(
				"jlink_standalonejmod_3-0.1.0-SNAPSHOT.jmod",
			)
			val modules = (Compile / jlink / fullModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_jlink_jlinkLaunchers") := Def.uncached {
			val expected = Seq(
				JlinkLauncher("main", "main", None)
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

			fileContains("release", "MODULES=\"java.base main\"")
			fileContains("bin/main", " -m main/main.Main ")
			if Option(System.getProperty("os.name")).map(_.contains("Windows")).getOrElse(false) then
				fileContains("bin/main.bat", " -m main/main.Main ")
		},
	)
