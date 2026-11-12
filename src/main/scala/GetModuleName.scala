package name.rayrobdod.sbtJlink

import java.lang.classfile.Attributes.{module => moduleAttributeMapper}
import java.lang.classfile.ClassFile
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.zip.ZipFile.OPEN_READ
import scala.jdk.OptionConverters.given
import scala.util.Using

object GetModuleName:
	def ofJar(jarFileName: Path): Option[String] =
		Using
			.resource(new JarFile(jarFileName.toFile, true, OPEN_READ, Runtime.version)): jarfile =>
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

	def ofAutomaticJar(jarFileName: Path): Option[String] =
		Using
			.resource(new JarFile(jarFileName.toFile, true, OPEN_READ, Runtime.version)): jarfile =>
				val attrName = new Attributes.Name("Automatic-Module-Name")
				val attrs = jarfile.getManifest.getMainAttributes
				Option.when(attrs.containsKey(attrName))(attrs.get(attrName).toString)

	def ofJmod(jmodFileName: Path): String =
		Using.resource(FileSystems.newFileSystem(jmodFileName)): jmodFileSystem =>
			val moduleInfoFilename = jmodFileSystem.getPath("classes", "module-info.class")
			val moduleInfo = ClassFile.of().parse(moduleInfoFilename)
			moduleInfo.findAttribute(moduleAttributeMapper).get.moduleName.name.stringValue
end GetModuleName
