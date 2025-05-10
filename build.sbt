lazy val jlink = project
	.in(file("."))
	.enablePlugins(SbtPlugin)
	.settings(
		sbtPlugin := true,

		libraryDependencies += {
			"org.apache.derby" % "derby" % "10.17.1.0"
		},

		name := "sbt-jlink",
		organization := "name.rayrobdod",
		organizationHomepage := Some(url("http://rayrobdod.name/")),
		homepage := Some(url("http://rayrobdod.name/programming/libraries/sbt/javafx/")),
		licenses := List("MIT-0" -> url("https://opensource.org/license/mit-0/")),
		developers := List(
			Developer(
				"rayrobdod",
				"Raymond Dodge",
				"git@rayrobdod.name",
				url("https://rayrobdod.name"),
			)
		),
		version := "0.0.0-SNAPSHOT",

		scriptedLaunchOpts ++= Seq("-Xmx1024M", s"-Dplugin.version=${version.value}"),
		scriptedBufferLog := false,
	)
