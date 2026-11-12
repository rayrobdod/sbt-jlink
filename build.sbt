lazy val jlink = project
	.in(file("."))
	.enablePlugins(SbtPlugin)
	.enablePlugins(ContrabandPlugin)
	.settings(
		sbtPlugin := true,

		name := "sbt-jlink",
		organization := "name.rayrobdod",
		organizationHomepage := Some(uri("http://rayrobdod.name/")),
		homepage := Some(uri("http://rayrobdod.name/programming/libraries/sbt/javafx/")),
		licenses := List("MIT-0" -> uri("https://opensource.org/license/mit-0/")),
		developers := List(
			Developer(
				"rayrobdod",
				"Raymond Dodge",
				"git@rayrobdod.name",
				uri("https://rayrobdod.name"),
			)
		),
		version := "0.0.0-SNAPSHOT",

		scriptedLaunchOpts ++= Seq("-Xmx1024M", s"-Dplugin.version=${version.value}"),
		scriptedBufferLog := false,
	)
