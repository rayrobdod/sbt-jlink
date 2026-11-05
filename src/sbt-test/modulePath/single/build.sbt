val verify = TaskKey[Unit]("verify")

lazy val manual = project
	.in(file("."))
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
		libraryDependencies += "org.jsoup" % "jsoup" % "1.20.1" % ModulePath,
	)
	.settings(
		verify := {
			val logger = (verify / streams).value.log
			val expected = "Hello <em>World</em>"
			// TODO: How to capture the output of `run` and compare it to something

			()
		}
)
