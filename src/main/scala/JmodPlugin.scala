package name.rayrobdod.sbtJlink

import sbt.*
import sbt.Keys.*
import sbt.util.CacheImplicits.given
import scala.util.Using

object JmodPlugin extends AutoPlugin:
	override def requires = sbt.plugins.JvmPlugin
	object autoImport:
		val jmod = taskKey[xsbti.HashedVirtualFileRef]("Produce a jmod artifact")
		val jmodOptions = taskKey[Seq[String]]("Additional options for jmod")
	import autoImport.*

	override lazy val globalSettings = Seq(
		jmodOptions := Nil
	)

	private def unscopedSettings = Seq(
	)

	private def perScopeSettings(config:sbt.librarymanagement.Configuration) =
		inConfig(config)(Seq(
			jmod / artifact := {
				Defaults.artifactSetting.value
					.withType("jmod")
					.withExtension("jmod")
			},
			jmod / artifactPath := Defaults.artifactPathSetting(jmod / artifact).value,
			jmod / fullClasspath := {
				Seq(Attributed.blank((Compile / packageBin).value))
			},
			jmod := {
				val conv = fileConverter.value
				val stream = (jmod / streams).value
				val logger = stream.log
				val outFile = conv.toPath((jmod / artifactPath).value)

				val args = locally:
					val builder = Vector.newBuilder[String]
					builder += "create"

					(jmod / packageTimestamp).value.foreach: epoch =>
						builder += "--date"
						builder += java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochMilli(epoch))

					(jmod / mainClass).value.foreach: clazz =>
						builder += "--main-class"
						builder += clazz

					builder += "--class-path"
					builder += (jmod / fullClasspath).value.map(avf => conv.toPath(avf.data)).mkString(java.io.File.pathSeparator)

					builder ++= jmodOptions.value

					builder += outFile.toString
					builder.result()
				logger.debug(args.mkString(" "))

				java.nio.file.Files.deleteIfExists(outFile)

				RunTool(
					"jmod",
					args,
					stream,
					"jmod",
				)

				val vf: xsbti.VirtualFile = conv.toVirtualFile(outFile)
				Def.declareOutput(vf)
				vf: xsbti.HashedVirtualFileRef
			}
		))

	override lazy val projectSettings = unscopedSettings ++ perScopeSettings(Compile) ++ perScopeSettings(Test)
