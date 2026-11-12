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
		TaskKey[Unit]("verify_jlink") := Def.uncached {
			val _ = (Compile / jlink).value

			val releaseFile = (Compile / jlink / target).value / "release"
			val releaseContents = java.nio.file.Files.readString(releaseFile.toPath)
			if ! releaseContents.contains("MODULES=\"java.base jdk.unsupported scala.library main\"") then
				sys.error(s"`release` contents:\n$releaseContents")
		},
	)
