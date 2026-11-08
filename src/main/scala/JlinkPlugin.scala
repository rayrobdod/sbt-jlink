package name.rayrobdod.sbtJlink

import sbt.*
import sbt.Keys.*
import sbt.util.CacheImplicits.given
import scala.util.Using

object JlinkPlugin extends AutoPlugin:
	override def requires = sbt.plugins.JvmPlugin
	object autoImport:
		val jlinkRun = inputKey[Unit]("Runs the artifact built by jlink")
		val jlink = taskKey[Unit]("assemble a custom runtime image")
		val jlinkOptions = taskKey[Seq[String]]("Additional options for jlink")
	import autoImport.*
	import ModulePathPlugin.autoImport.*
	import JmodPlugin.autoImport.*

	override lazy val globalSettings = Seq(
		jlinkOptions := Nil
	)

	private def selfModulepathImplTask(
			projectRef: ResolvedReference,
			conf: Configuration,
	): Def.Initialize[Task[Classpath]] =
		Def.task{
			val jmodValue = (projectRef / ConfigKey(conf.name) / jmod).?.value
				.map: file =>
					Seq(Attributed.blank(file))

			val exportedProductsValue = (projectRef / ConfigKey(conf.name) / exportedProducts).value
			val mainModuleValue = ((projectRef / ConfigKey(conf.name) / mainModule) ?? None).value
				.map: _ =>
					exportedProductsValue

			jmodValue
				.orElse(mainModuleValue)
				.getOrElse(Seq.empty)
		}

	private def selfModulepathImpl: Def.Initialize[Task[Classpath]] =
		Def.task {((
			thisProjectRef.value,
			configuration.value,
		))}
		.flatMapTask(selfModulepathImplTask)

	private def internalDependencyModulepathImplTask(
			projectRef: ProjectRef,
			conf: Configuration,
			data: Def.Settings,
			deps: sbt.internal.BuildDependencies,
			track: TrackLevel,
			log: Logger
	): Def.Initialize[Task[Classpath]] =
		Def.value[Task[Classpath]] {
			deps.classpath(projectRef)
				// `filter` on `dep.configuration`?
				.map: (dep: sbt.ClasspathDep[sbt.ProjectRef]) =>
					dep.project
				.map: (dep: ResolvedReference) =>
					selfModulepathImplTask(dep, conf).evaluate(data)
				.join
				.map: (classpaths: Seq[Classpath]) =>
					classpaths.flatten.distinct
		}
	end internalDependencyModulepathImplTask

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
	end internalDependencyModulepathImpl


	private def unscopedSettings = Seq(
	)

	private def perScopeSettings(config:sbt.librarymanagement.Configuration) = {
		inConfig(config)(Seq(
			jlink / externalDependencyModulepath := {
				(externalDependencyModulepath ?? Seq.empty).value
			},
			jlink / internalDependencyModulepath := {
				internalDependencyModulepathImpl.value
			},
			jlink / dependencyModulepath := {
				Classpaths.concat(jlink / internalDependencyModulepath, jlink / externalDependencyModulepath).value
			},
			jlink / exportedProducts := Def.uncached {
				selfModulepathImpl.value
			},
			jlink / fullModulepath := {
				Classpaths.concat(jlink / dependencyModulepath, jlink / exportedProducts).value
			},


			jlink / fullAddModules := {
				val conv = fileConverter.value
				val modulePathPluginModules = (fullAddModules ?? Seq.empty).value
				val jmodPluginModules = jmod.?.value.map(vf => GetModuleName.ofJmod(conv.toPath(vf)))

				modulePathPluginModules
					++ jmodPluginModules
			},


			jlink / target := (config / target).value / "jlink",
			jlink := {
				val conv = fileConverter.value
				val stream = (jlink / streams).value
				val logger = stream.log
				val outDirectory = (jlink / target).value.toPath

				val args = locally:
					val builder = Vector.newBuilder[String]

					builder += "--module-path"
					builder += (jlink / fullModulepath).value.map(avf => conv.toPath(avf.data)).mkString(java.io.File.pathSeparator)

					builder += "--output"
					builder += outDirectory.toString

					builder += "--add-modules"
					builder += (jlink / fullAddModules).value.mkString(",")

					builder ++= jlinkOptions.value

					builder.result()
				logger.debug(args.mkString(" "))

				if java.nio.file.Files.exists(outDirectory) then
					java.nio.file.Files.walkFileTree(
						outDirectory,
						new RecursiveDeleteFileVisitor
					)
				end if

				RunTool(
					"jlink",
					args,
					stream,
					"jlink",
				)

				val vf: xsbti.VirtualFileRef = conv.toVirtualFile(outDirectory)
				Def.declareOutputDirectory(vf)
			},


			jlinkRun / forkOptions := Def.uncached {
				ForkOptions(
					javaHome = Option((jlink / target).value),
					outputStrategy = (jlink / outputStrategy).value,
					bootJars = Vector.empty,
					workingDirectory = Option(baseDirectory.value),
					runJVMOptions = Vector.empty,
					connectInput = true,
					envVars = Map.empty,
				)
			},
			jlinkRun / mainModule := Def.uncached {
				val conv = fileConverter.value
				jmod.?.value
					.map: jmodFile =>
						GetModuleName.ofJmod(conv.toPath(jmodFile))
					.orElse:
						val mainModuleValue = (mainModule ?? None).value
						val mainClassValue = mainClass.value

						mainModuleValue.zip(mainClassValue).map: (mod, clazz) =>
							s"${mod}/${clazz}"
			},
			jlinkRun := Def.uncached {
				jlink.value

				val logger = (jlink / streams).value.log

				val args = Seq("--module", (jlinkRun / mainModule).value.getOrElse(sys.error("No main module set")))
				logger.debug(args.mkString(" "))

				Fork.java.apply((jlinkRun / forkOptions).value, args)
			},
		))
	}

	override lazy val projectSettings = unscopedSettings ++ perScopeSettings(Compile) ++ perScopeSettings(Test)
