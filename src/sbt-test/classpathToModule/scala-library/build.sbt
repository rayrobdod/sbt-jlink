lazy val scalaLibraryModule = project
	.in(file(".sbt/scalaLibraryModule"))
	.enablePlugins(ClasspathToModulePlugin)
	.settings(
		autoScalaLibrary := true,
		scalaVersion := "3.9.0",
		TaskKey[Unit]("verify_classpathToModuleModuleInfo") := Def.uncached {
			val actual = (Compile / classpathToModuleModuleInfo).value

			val expectedExports = Vector("scala", "scala.annotation", "scala.annotation.internal", "scala.annotation.meta", "scala.annotation.unchecked", "scala.beans", "scala.caps", "scala.collection", "scala.collection.concurrent", "scala.collection.convert", "scala.collection.convert.impl", "scala.collection.generic", "scala.collection.immutable", "scala.collection.mutable", "scala.compat", "scala.compiletime", "scala.compiletime.ops", "scala.compiletime.testing", "scala.concurrent", "scala.concurrent.duration", "scala.concurrent.impl", "scala.deriving", "scala.io", "scala.jdk", "scala.jdk.javaapi", "scala.math", "scala.quoted", "scala.quoted.runtime", "scala.ref", "scala.reflect", "scala.reflect.macros.internal", "scala.runtime", "scala.runtime.coverage", "scala.runtime.function", "scala.runtime.java8", "scala.runtime.stdLibPatches", "scala.sys", "scala.sys.process", "scala.util", "scala.util.control", "scala.util.hashing", "scala.util.matching")

			val expected = ClasspathToModuleModuleInfo(
				"scala.library",
				Option.empty,
				Vector("java.base", "jdk.unsupported"),
				expectedExports,
				expectedExports,
				Vector.empty,
				Map.empty,
			)

			if expected != actual then
				sys.error("moduleInfo did not match expected")
		},
	)
