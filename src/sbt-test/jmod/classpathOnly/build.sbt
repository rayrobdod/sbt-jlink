lazy val jmod_classpathOnly = project
	.in(file("."))
	.enablePlugins(JmodPlugin)
	.settings(
		autoScalaLibrary := false,
		TaskKey[Unit]("verify_jmod") := Def.uncached {
			import java.nio.file.*
			val conv = fileConverter.value
			val fileToCheck = conv.toPath((Compile / jmod).value)

			val problems = scala.util.Using.resource(FileSystems.newFileSystem(fileToCheck)): fs =>
				val builder = List.newBuilder[String]

				val moduleInfoPath = fs.getPath("classes", "module-info.class")
				if ! Files.exists(moduleInfoPath) then
					builder += "module-info class is missing"
				else
					val moduleInfo = java.lang.classfile.ClassFile.of().parse(moduleInfoPath)
					val moduleName = moduleInfo.findAttribute(java.lang.classfile.Attributes.module).get.moduleName.name.stringValue
					if "lib" != moduleName then
						builder += s"jmod for wrong module: $moduleName"

				builder.result()

			if problems.nonEmpty then
				sys.error(problems.mkString("Problems:\n  ", "\n  ", ""))
		},
	)
