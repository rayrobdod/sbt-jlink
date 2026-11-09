lazy val jlink_managedModularJar = project
	.in(file("."))
	.enablePlugins(JlinkPlugin, ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulePath := true,
		libraryDependencies += "org.jsoup" % "jsoup" % "1.20.1" % ModulePath,
		TaskKey[Unit]("verify_jlink_fullModulepath") := Def.uncached {
			val expected = Seq(
				"jsoup-1.20.1.jar",
				"jlink_managedmodularjar_3-0.1.0-SNAPSHOT-modulePath.jar",
			)
			val modules = (Compile / jlink / fullModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_jlink") := Def.uncached {
			(Compile / jlink).value

			val releaseFile = (Compile / jlink / target).value / "release"
			val releaseContents = java.nio.file.Files.readString(releaseFile.toPath)
			if ! releaseContents.contains("MODULES=\"java.base java.xml org.jsoup main\"") then
				sys.error(s"`release` contents:\n$releaseContents")
		},
	)
