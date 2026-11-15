lazy val first = project
	.in(file("First"))
	.enablePlugins(JmodPlugin, ModulepathPlugin)
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
	.enablePlugins(JlinkPlugin, ModulepathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulepath := true,
		TaskKey[Unit]("verify_jlink_fullModulepath") := Def.uncached {
			val expected = Seq(
				"second_3-0.1.0-SNAPSHOT-modulepath.jar",
				"first_3-0.1.0-SNAPSHOT.jmod",
				"third_3-0.1.0-SNAPSHOT-modulepath.jar",
			)
			val modules = (Compile / jlink / fullModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_jlink_jlinkLaunchers") := Def.uncached {
			val expected = Seq(
				JlinkLauncher("Third", "third", Some("third.Third"))
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

			fileContains("release", "MODULES=\"java.base first second third\"")
			fileContains("bin/Third", " -m third/third.Third ")
			if Option(System.getProperty("os.name")).map(_.contains("Windows")).getOrElse(false) then
				fileContains("bin/Third.bat", " -m third/third.Third ")
		},
	)
