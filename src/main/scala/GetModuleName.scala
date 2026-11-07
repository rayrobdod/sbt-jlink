package name.rayrobdod.sbtJlink

import java.nio.file.Path
import java.nio.file.FileSystems
import java.lang.classfile.Attributes.{module => moduleAttributeMapper}
import scala.util.Using
import scala.jdk.OptionConverters.given
import java.lang.classfile.ClassFile

object GetModuleName:
	def ofJar(jarFileName: Path): Option[String] =
		import java.util.zip.ZipFile.OPEN_READ
		Using
			.resource(new java.util.jar.JarFile(jarFileName.toFile, true, OPEN_READ, Runtime.version)): jarfile =>
				Option(jarfile.getJarEntry("module-info.class")).map: moduleInfoEntry =>

					val transferStream = new java.io.ByteArrayOutputStream
					Using.resource(jarfile.getInputStream(moduleInfoEntry)): is =>
						is.transferTo(transferStream)

					transferStream.toByteArray
			.flatMap: moduleInfoBytes =>
				val moduleInfo = ClassFile.of().parse(moduleInfoBytes)
				moduleInfo.findAttribute(moduleAttributeMapper).toScala
			.map: moduleAttribute =>
				moduleAttribute.moduleName.name.stringValue

	def ofJmod(jmodFileName: Path): String =
		Using.resource(FileSystems.newFileSystem(jmodFileName)): jmodFileSystem =>
			val moduleInfoFilename = jmodFileSystem.getPath("classes", "module-info.class")
			val moduleInfo = ClassFile.of().parse(moduleInfoFilename)
			moduleInfo.findAttribute(moduleAttributeMapper).get.moduleName.name.stringValue
end GetModuleName
