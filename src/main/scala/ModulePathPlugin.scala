package name.rayrobdod.sbtJlink

import sbt.*
import sbt.Keys.*
import sbt.util.CacheImplicits.given
import java.nio.file.Path

object ModulePathPlugin extends AutoPlugin:
	override def requires = sbt.plugins.JvmPlugin
	object autoImport:
		val ModulePath = config("modulePath")
		val TestModulepath = config("test-modulepath").extend(ModulePath)

		val exportToModulePath = settingKey[Boolean]("If true, do not export packageBin to the compile classpath, instead export packageBin to the modulepath")
		val discoveredAddModules = taskKey[Option[String]]("The discovered java platform modules in this project")

		val unmanagedModulepath = taskKey[Classpath]("Modulepath entries (deep) that are manually managed.")
		val managedModulepath = taskKey[Classpath]("The modulepath consisting of external, managed library dependencies.")
		val externalDependencyModulepath = taskKey[Classpath]("The classpath consisting of library dependencies, both managed and unmanaged.")
		val internalDependencyModulepath = taskKey[Classpath]("The internal (inter-project) modulepath.")
		val dependencyModulepath = taskKey[Classpath]("The modulepath consisting of internal and external, managed and unmanaged dependencies.")
		val exportedModulepath = taskKey[Classpath]("Build products that go on the exported modulepath.")
		val fullModulepath = taskKey[Classpath]("The exported modulepath, consisting of build products and unmanaged and managed, internal and external dependencies.")

		val unmanagedAddModules = taskKey[Seq[String]]("Added module entries that as manually managed")
		val managedAddModules = taskKey[Seq[String]]("Added module entries from external, managed library dependencies")
		val dependencyAddModules = taskKey[Seq[String]]("Added module entries, from all dependencies")
		val exportedAddModules = taskKey[Seq[String]]("This project's java platform module")
		val fullAddModules = taskKey[Seq[String]]("Added module entries, from build products and all dependencies")
	import autoImport.*

	override def globalSettings = Seq(
		exportToModulePath := false,
	)

	private def unscopedSettings = Seq(
		ivyConfigurations += ModulePath,
		ivyConfigurations += TestModulepath,

		Compile / managedModulepath := Def.uncached {
			val c: xsbti.FileConverter = (fileConverter).value

			update.value
				.filter(configurationFilter(ModulePath.name))
				.allFiles
				.map(f => c.toVirtualFile(f.toPath))
				.map(f => f: xsbti.HashedVirtualFileRef)
				.map(f => Attributed.blank(f))
		},
		Test / managedModulepath := Def.uncached {
			val c: xsbti.FileConverter = (fileConverter).value

			update.value
				.filter(configurationFilter(TestModulepath.name))
				.allFiles
				.map(f => c.toVirtualFile(f.toPath))
				.map(f => f: xsbti.HashedVirtualFileRef)
				.map(f => Attributed.blank(f))
		},
		Compile / exportToModulePath := exportToModulePath.value,
		Test / exportToModulePath := exportToModulePath.value,
	)

	private def internalDependencyModulepathImplTask(
			projectRef: ProjectRef,
			conf: Configuration,
			data: Def.Settings,
			deps: sbt.internal.BuildDependencies,
			track: TrackLevel,
			log: Logger
	) : Def.Initialize[Task[Classpath]] = Def.value[Task[Classpath]] {
		deps.classpath(projectRef)
			// `filter` on `dep.configuration`?
			.map: (dep: sbt.ClasspathDep[sbt.ProjectRef]) =>
				dep.project
			.map: (dep: ResolvedReference) =>
				(dep / ConfigKey(conf.name) / exportedModulepath).get(data) match
					case Some(x) => x
					case None => constant(Nil)
			.join
			.map: classpaths =>
				classpaths
					.flatten
					.distinct
	}
	private val internalDependencyModulepathImpl: Def.Initialize[Task[Classpath]] =
		Def.task {((
			thisProjectRef.value,
			configuration.value,
			settingsData.value,
			buildDependencies.value,
			trackInternalDependencies.value,
			(internalDependencyModulepath / streams).value.log,
		))}
		.flatMapTask(internalDependencyModulepathImplTask)

	private def EmptyIfMainModuleIsDefined(config:sbt.librarymanagement.Configuration, key: TaskKey[Classpath]): Def.Setting[Task[Classpath]] =
		key := Def.uncached:
			if (config / exportToModulePath).value then
				Nil
			else
				key.value
	end EmptyIfMainModuleIsDefined

	/**
	 * `--add-modules` is cumulative, but `--module-path` is last wins
	 * Just avoid having multiple of either options
	 */
	private def insertModules(prevOptions: Seq[String], addedModulepath: Seq[Path], addedAddModules: Seq[String]): Seq[String] =
		val retval = prevOptions.to(scala.collection.mutable.Buffer)

		def append(key: String, separator: String, addends: Seq[String]): Unit =
			if addends.nonEmpty then
				val index = retval.indexOf(key)
				if index < 0 then
					retval += key
					retval += addends.mkString(separator)
				else
					retval(index + 1) = retval(index + 1)
						.split(separator)
						.to(Vector)
						.++(addends)
						.distinct
						.mkString(separator)
		end append

		append("--module-path", java.io.File.pathSeparator, addedModulepath.map(_.toString))
		append("--add-modules", ",", addedAddModules)

		retval.to(Vector)
	end insertModules

	private def perScopeSettings(config:sbt.librarymanagement.Configuration) = {
		// using `inConfig` because `(config / jlink / streams).value` is claiming that `value / is not a member of sbt.librarymanagement.Configuration`
		// despite `(config / target).value` definitively showing that Configuration does indeed have a `/` value
		inConfig(config)(Seq(
			run / fork := true,

			discoveredAddModules := {
				val c: xsbti.FileConverter = (fileConverter).value
				val jarFileVF = (Compile / packageBin).value
				GetModuleName.ofJar(c.toPath(jarFileVF))
			},


			packageBin / configuration := {
				if exportToModulePath.value then
					if Compile == (packageBin / configuration).value then
						ModulePath
					else if Test == (packageBin / configuration).value then
						TestModulepath
					else
						???
				else
					(packageBin / configuration).value
			},

			EmptyIfMainModuleIsDefined(config, exportedProductsNoTracking),
			EmptyIfMainModuleIsDefined(config, exportedProductsIfMissing),
			EmptyIfMainModuleIsDefined(config, exportedProducts),
			EmptyIfMainModuleIsDefined(config, exportedProductJarsNoTracking),
			EmptyIfMainModuleIsDefined(config, exportedProductJarsIfMissing),
			EmptyIfMainModuleIsDefined(config, exportedProductJars),

			exportedModulepath := {
				if exportToModulePath.value then
					Seq(Attributed.blank((packageBin).value: xsbti.HashedVirtualFileRef))
				else
					Seq.empty
			},


			unmanagedModulepath := Nil, // TODO
			externalDependencyModulepath := Def.uncached{
				Classpaths.concat(unmanagedModulepath, managedModulepath).value
			},
			internalDependencyModulepath := Def.uncached {
				internalDependencyModulepathImpl.value
			},
			dependencyModulepath := Def.uncached{
				Classpaths.concat(internalDependencyModulepath, externalDependencyModulepath).value
			},
			fullModulepath := {
				Classpaths.concat(dependencyModulepath, exportedModulepath).value
			},


			unmanagedAddModules := Nil,
			managedAddModules := {
				val c: xsbti.FileConverter = (fileConverter).value
				(Compile / dependencyModulepath).value.map: attributed =>
					val vf = attributed.data
					val path = c.toPath(vf)
					GetModuleName.ofJar(path).getOrElse(sys.error(s"Modulepath entry did not have module name: $path"))
			},
			dependencyAddModules := Def.uncached {
				unmanagedAddModules.value ++ managedAddModules.value
			},
			exportedAddModules := {
				if exportToModulePath.value then
					discoveredAddModules.value.toSeq
				else
					Seq.empty
			},
			fullAddModules := {
				dependencyAddModules.value ++ exportedAddModules.value
			},


			javacOptions := {
				given c: xsbti.FileConverter = (fileConverter).value
				insertModules(
					javacOptions.value,
					(dependencyModulepath).value.files,
					(dependencyAddModules).value,
				)
			},
			javaOptions := {
				given c: xsbti.FileConverter = (fileConverter).value
				insertModules(
					javaOptions.value,
					(fullModulepath).value.files,
					(fullAddModules).value,
				)
			},
		))
	}

	override lazy val projectSettings = unscopedSettings ++ perScopeSettings(Compile) ++ perScopeSettings(Test)
