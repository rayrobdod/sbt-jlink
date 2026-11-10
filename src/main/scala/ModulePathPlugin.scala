package name.rayrobdod.sbtJlink

import sbt.*
import sbt.Keys.*
import sbt.util.CacheImplicits.given
import java.nio.file.Path

object ModulePathPlugin extends AutoPlugin:
	override def requires = sbt.plugins.JvmPlugin
	object autoImport:
		val ModulePath = config("modulePath")

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

	private def EmptyIfMainModuleIsDefined(key: TaskKey[Classpath]): Def.Setting[Task[Classpath]] =
		key := Def.uncached:
			if exportToModulePath.value then
				Nil
			else
				key.value
	end EmptyIfMainModuleIsDefined

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
					ModulePath
				else
					(packageBin / configuration).value
			},

			EmptyIfMainModuleIsDefined(exportedProductsNoTracking),
			EmptyIfMainModuleIsDefined(exportedProductsIfMissing),
			EmptyIfMainModuleIsDefined(exportedProducts),
			EmptyIfMainModuleIsDefined(exportedProductJarsNoTracking),
			EmptyIfMainModuleIsDefined(exportedProductJarsIfMissing),
			EmptyIfMainModuleIsDefined(exportedProductJars),

			exportedModulepath := {
				if exportToModulePath.value then
					Seq(Attributed.blank((Compile / packageBin).value: xsbti.HashedVirtualFileRef))
				else
					Seq.empty
			},


			unmanagedModulepath := Nil, // TODO
			managedModulepath := Def.uncached {
				val c: xsbti.FileConverter = (fileConverter).value

				update.value
					.filter(configurationFilter(ModulePath.name))
					.allFiles
					.map(f => c.toVirtualFile(f.toPath))
					.map(f => xsbti.HashedVirtualFileRef.of(f.id(), f.contentHashStr(), f.sizeBytes()))
					.map(f => Attributed.blank(f))
			},
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
					GetModuleName.ofJar(path).getOrElse(sys.error(s"ModulePath entry did not have module name: $path"))
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


			javacOptions ++= {
				given c: xsbti.FileConverter = (fileConverter).value
				val retval = List.newBuilder[String]
				if (dependencyModulepath).value.nonEmpty then
					retval += "--module-path"
					retval += (dependencyModulepath).value.files.mkString(java.io.File.pathSeparator)
				end if
				if (dependencyAddModules).value.nonEmpty then
					retval += "--add-modules"
					retval += (dependencyAddModules).value.mkString(",")
				end if
				retval.result()
			},
			javaOptions ++= {
				given c: xsbti.FileConverter = (fileConverter).value
				val retval = List.newBuilder[String]
				if (fullModulepath).value.nonEmpty then
					retval += "--module-path"
					retval += (fullModulepath).value.files.mkString(java.io.File.pathSeparator)
				end if
				if (fullAddModules).value.nonEmpty then
					retval += "--add-modules"
					retval += (fullAddModules).value.mkString(",")
				end if
				retval.result()
			},
		))
	}

	override lazy val projectSettings = unscopedSettings ++ perScopeSettings(Compile) ++ perScopeSettings(Test)
