lazy val jlink_standaloneJmod = project
	.in(file("."))
	.enablePlugins(JlinkPlugin, JmodPlugin)
	.settings(
		autoScalaLibrary := false,
		TaskKey[Unit]("verify_jlink") := Def.uncached {
			(Compile / jlink).value

			val releaseFile = (Compile / jlink / target).value / "release"
			val releaseContents = java.nio.file.Files.readString(releaseFile.toPath)
			if ! releaseContents.contains("MODULES=\"java.base main\"") then
				sys.error(s"`release` contents:\n$releaseContents")
		},
	)
