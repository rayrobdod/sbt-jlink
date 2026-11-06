lazy val base = project
	.in(file("Base"))
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
	)

lazy val dependent = project
	.in(file("Dependent"))
	.dependsOn(base)
	.enablePlugins(ModulePathPlugin)
	.settings(
		autoScalaLibrary := false,
	)
