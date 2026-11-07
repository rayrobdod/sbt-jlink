lazy val jlink_managedModularJar = project
	.in(file("."))
	.enablePlugins(JlinkPlugin, ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		libraryDependencies += "org.jsoup" % "jsoup" % "1.20.1" % ModulePath,
		TaskKey[Unit]("verify_jlink") := Def.uncached {
			(Compile / jlink).value

			val releaseFile = (Compile / jlink / target).value / "release"
			val releaseContents = java.nio.file.Files.readString(releaseFile.toPath)
			if ! releaseContents.contains("MODULES=\"java.base java.xml org.jsoup main\"") then
				sys.error(s"`release` contents:\n$releaseContents")
		},
	)
