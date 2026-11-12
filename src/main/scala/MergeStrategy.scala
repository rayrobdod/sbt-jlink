package name.rayrobdod.sbtJlink

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.regex.Pattern
import scala.util.Using
import sbt.pathToPathOps

enum MergeStrategy:
	case SingleOrError
	case Manifest

	def apply(entryName: String, srcs: Seq[Path], tmpdir: Path, logger: sbt.Logger): Path = this match
		case SingleOrError =>
			if srcs.size == 1 then
				srcs(0)
			else
				sys.error(s"$entryName -> singleOrError: multiple srcs")

		case Manifest =>
			val discardedMainAttributes = Set[Object](
				new Attributes.Name("Automatic-Module-Name"),
				Attributes.Name.SPECIFICATION_TITLE,
				Attributes.Name.SPECIFICATION_VENDOR,
				Attributes.Name.SPECIFICATION_VERSION,
				Attributes.Name.IMPLEMENTATION_TITLE,
				Attributes.Name.IMPLEMENTATION_VENDOR,
				new Attributes.Name("Implementation-Vendor-Id"),
				Attributes.Name.IMPLEMENTATION_VERSION,
				new Attributes.Name("Implementation-Url"),
			)

			val result = java.util.jar.Manifest()
			srcs.foreach: src =>
				val srcManifest =
					Using.resource(Files.newInputStream(src)): is =>
						java.util.jar.Manifest(is)

				srcManifest.getMainAttributes.forEach: (k, v) =>
					if ! discardedMainAttributes.contains(k) then
						result.getMainAttributes.merge(k, v, (left, right) =>
							if left == right then
								left
							else
								sys.error(s"$entryName -> manifest: multiple $k entries"))

				srcManifest.getEntries.forEach: (name, attributes) =>
					result.getEntries.merge(name, attributes, (resultAttributes, srcAttributes) =>
						srcAttributes.forEach: (k, v) =>
							resultAttributes.merge(k, v, (left, right) =>
								if left == right then
									left
								else
									sys.error(s"$entryName -> manifest: multiple $name/$k entries"))
						resultAttributes
					)

			val outFile = tmpdir / entryName
			Files.createDirectories(outFile.getParent)
			Using.resource(Files.newOutputStream(outFile)): os =>
				result.write(os)
			outFile
	end apply
end MergeStrategy

final class MergeStrategyChooser private (
	val default: MergeStrategy,
	val parts: Vector[(Pattern, MergeStrategy)],
):
	def apply(entryName: String, srcs: Seq[Path], tmpdir: Path, logger: sbt.Logger): Path =
		val chosenStrategy = this.parts.foldLeft(this.default): (folding, part) =>
			if part._1.matcher(entryName).matches then part._2 else folding

		chosenStrategy(entryName, srcs, tmpdir, logger)

	def :+(newPart: (Pattern, MergeStrategy)): MergeStrategyChooser =
		new MergeStrategyChooser(default, parts :+ newPart)

	override def toString: String =
		s"MergeStrategyChooser($default, $parts)"

	override def hashCode: Int =
		java.util.Objects.hash("MergeStrategyChooser", default, parts)

	override def equals(o: Any): Boolean = o match
		case other: MergeStrategyChooser => this.default == other.default && this.parts == other.parts
		case _ => false
end MergeStrategyChooser

object MergeStrategyChooser:
	def apply(default: MergeStrategy): MergeStrategyChooser =
		new MergeStrategyChooser(default, Vector.empty)
	def apply(default: MergeStrategy, parts: Vector[(Pattern, MergeStrategy)]): MergeStrategyChooser =
		new MergeStrategyChooser(default, parts)
end MergeStrategyChooser
