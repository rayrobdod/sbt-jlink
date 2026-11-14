lazy val jmod_classpathOnly = project
	.in(file("."))
	.enablePlugins(ModulepathPlugin, JmodPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulepath := true,
		TaskKey[Unit]("verify_jmod") := Def.uncached {
			import java.nio.file.*
			val conv = fileConverter.value
			val fileToCheck = conv.toPath((Compile / jmod).value)

			val moduleName = _root_.name.rayrobdod.sbtJlink.GetModuleName.ofJmod(fileToCheck)
			if "lib" != moduleName then
				sys.error(s"jmod for wrong module: $moduleName")
		},
	)
