import _root_.name.rayrobdod.sbtJlink.JsonProtocol.given

lazy val combinedModule = project
	.in(file("."))
	.enablePlugins(ClasspathToModulePlugin)
	.settings(
		autoScalaLibrary := true,
		scalaVersion := "3.9.0",
		libraryDependencies ++= Seq(
			"org.typelevel" %% "cats-effect" % "3.7.1",
		),
		Compile / classpathToModuleModuleName := "name.rayrobdod.sbtJlink.catsEffect",
		Compile / classpathToModuleModuleInfo := {
			(Compile / classpathToModuleModuleInfo).value
				.withOpens(Vector.empty)
				.withExports(Vector.empty)
		},
		TaskKey[Unit]("verify_classpathToModule") := Def.uncached {
			import java.nio.charset.StandardCharsets.UTF_8

			val conv = fileConverter.value
			val module = (Compile / classpathToModule).value

			val forkOutputStream = new java.io.ByteArrayOutputStream()
			val forkOpts = forkOptions.value
					.withOutputStrategy(OutputStrategy.CustomOutput(forkOutputStream))

			val args = Vector(
				"--module-path",
				conv.toPath(module).toString,
				"--module",
				"name.rayrobdod.sbtJlink.catsEffect/main.HelloWorld"
			)

			Fork.java.apply(forkOpts, args)

			val actual = forkOutputStream.toString(UTF_8)
			val expected = "Hello, World!" + System.lineSeparator()

			if expected != actual then
				sys.error(s"expected: $expected; actual: $actual")
		},
	)
