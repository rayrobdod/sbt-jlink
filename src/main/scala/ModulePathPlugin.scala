package name.rayrobdod.sbtJlink

import sbt.{URL => _, _}
import sbt.Keys._
import sbt.util.CacheImplicits.given

object ModulePathPlugin extends AutoPlugin:
	override def requires = sbt.plugins.JvmPlugin
	object autoImport:
		val dependencyModulepath = taskKey[Classpath]("The modulepath of dependencies")
		val modulepathAddModules = taskKey[Seq[String]]("Modules that should be added without being explicitly depended upon")
		val ModulePath = config("modulePath").hide

	import autoImport._

	private def unscopedSettings = Seq(
		ivyConfigurations += ModulePath,
		modulepathAddModules := Nil,
		Compile / dependencyModulepath := Def.uncached {
			val c: xsbti.FileConverter = (fileConverter).value

			update.value
				.filter(configurationFilter(ModulePath.name))
				.allFiles
				.map(f => c.toVirtualFile(f.toPath))
				.map(f => xsbti.HashedVirtualFileRef.of(f.id(), f.contentHashStr(), f.sizeBytes()))
				.map(f => Attributed.blank(f))
		},
		Test / dependencyModulepath := Nil,
	)
	
	private def perScopeSettings(config:sbt.librarymanagement.Configuration) = {
		// using `inConfig` because `(config / jlink / streams).value` is claiming that `value / is not a member of sbt.librarymanagement.Configuration`
		// despite `(config / target).value` definitively showing that Configuration does indeed have a `/` value
		inConfig(config)(Seq(
			run / fork := true,
			javacOptions ++= {
				given c: xsbti.FileConverter = (fileConverter).value
				val retval = List.newBuilder[String]
				if (dependencyModulepath).value.nonEmpty then
					retval += "--module-path"
					retval += (dependencyModulepath).value.files.mkString(java.io.File.pathSeparator)
				end if
				if (modulepathAddModules).value.nonEmpty then
					retval += "--add-modules"
					retval += (modulepathAddModules).value.mkString(",")
				end if
				retval.result()
			},
			javaOptions ++= {
				given c: xsbti.FileConverter = (fileConverter).value
				val retval = List.newBuilder[String]
				if (dependencyModulepath).value.nonEmpty then
					retval += "--module-path"
					retval += (dependencyModulepath).value.files.mkString(java.io.File.pathSeparator)
				end if
				if (modulepathAddModules).value.nonEmpty then
					retval += "--add-modules"
					retval += (modulepathAddModules).value.mkString(",")
				end if
				retval.result()
			},
		))
	}

	override lazy val projectSettings = unscopedSettings ++ perScopeSettings(Compile) ++ perScopeSettings(Test)
