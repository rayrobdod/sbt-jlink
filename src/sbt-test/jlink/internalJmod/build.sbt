lazy val base = project
	.in(file("Base"))
	.enablePlugins(JmodPlugin, ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulePath := true,
	)

lazy val dependent = project
	.in(file("Dependent"))
	.dependsOn(base, base % "modulePath->modulePath")
	.enablePlugins(JlinkPlugin, ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulePath := true,
		TaskKey[Unit]("verify_jlink_fullModulepath") := Def.uncached {
			val expected = Seq(
				"base_3-0.1.0-SNAPSHOT.jmod",
				"dependent_3-0.1.0-SNAPSHOT-modulePath.jar",
			)
			val modules = (Compile / jlink / fullModulepath).value
			val actual = modules.map(_.data.name)

			if expected != actual then
				sys.error(modules.mkString("fullModulepath did not match expected:\n  ", "\n  ", ""))
		},
		TaskKey[Unit]("verify_jlink") := Def.uncached {
			val _ = (Compile / jlink).value

			val releaseFile = (Compile / jlink / target).value / "release"
			val releaseContents = java.nio.file.Files.readString(releaseFile.toPath)
			if ! releaseContents.contains("MODULES=\"java.base lib main\"") then
				sys.error(s"`release` contents:\n$releaseContents")
		},
	)
