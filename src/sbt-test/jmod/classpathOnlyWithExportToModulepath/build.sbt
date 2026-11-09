lazy val jmod_classpathOnly = project
	.in(file("."))
	.enablePlugins(ModulePathPlugin, JmodPlugin)
	.settings(
		autoScalaLibrary := false,
		exportToModulePath := true,
		TaskKey[Unit]("verify_jmod") := Def.uncached {
			import java.nio.file.*
			val conv = fileConverter.value
			val fileToCheck = conv.toPath((Compile / jmod).value)

			val moduleName = _root_.name.rayrobdod.sbtJlink.GetModuleName.ofJmod(fileToCheck)
			if "lib" != moduleName then
				sys.error(s"jmod for wrong module: $moduleName")
		},
	)
