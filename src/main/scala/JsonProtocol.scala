package name.rayrobdod.sbtJlink

import java.util.regex.Pattern
import sjsonnew.*
import sjsonnew.BasicJsonProtocol.given

// Even if I wanted a caked-up JsonProtocol;
// I couldn't use contraband to make this: contraband cannot handle the `Map[String, Vector[String]]` field.
// Having a `Map` field causes Contraband's code generator to decide that the protocol must extend `_root_.MapFormats`.

object JsonProtocol:
	given ModuleInfoJsonFormat: JsonFormat[ModuleInfo] with
		override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): ModuleInfo =
			__jsOpt match
				case Some(__js) =>
					unbuilder.beginObject(__js)
					val moduleName = unbuilder.readField[String]("moduleName")
					val moduleVersion = unbuilder.readField[Option[String]]("moduleVersion")
					val requires = unbuilder.readField[Vector[String]]("requires")
					val exports = unbuilder.readField[Vector[String]]("exports")
					val opens = unbuilder.readField[Vector[String]]("opens")
					val uses = unbuilder.readField[Vector[String]]("uses")
					val provides = unbuilder.readField[Map[String, Vector[String]]]("provides")
					unbuilder.endObject()
					ModuleInfo(moduleName, moduleVersion, requires, exports, opens, uses, provides)
				case None =>
					deserializationError("Expected JsObject but found None")
		override def write[J](obj: ModuleInfo, builder: Builder[J]): Unit =
			builder.beginObject()
			builder.addField("moduleName", obj.moduleName)
			builder.addField("moduleVersion", obj.moduleVersion)
			builder.addField("requires", obj.requires)
			builder.addField("exports", obj.exports)
			builder.addField("opens", obj.opens)
			builder.addField("uses", obj.uses)
			builder.addField("provides", obj.provides)
			builder.endObject()
	end ModuleInfoJsonFormat

	given PatternJsonFormat: JsonFormat[Pattern] with
		override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): Pattern =
			__jsOpt match
				case Some(__js) =>
					unbuilder.beginObject(__js)
					val regex = unbuilder.readField[String]("regex")
					val flags = unbuilder.readField[Int]("flags")
					unbuilder.endObject()
					Pattern.compile(regex, flags)
				case None =>
					deserializationError("Expected JsObject but found None")
		override def write[J](obj: Pattern, builder: Builder[J]): Unit =
			builder.beginObject()
			builder.addField("regex", obj.pattern)
			builder.addField("flags", obj.flags)
			builder.endObject()
	end PatternJsonFormat

	given MergeStrategyJsonFormat: IsoString[MergeStrategy] =
		IsoString.iso(_.toString, MergeStrategy.valueOf)

	given MergeStrategyChooserJsonFormat: JsonFormat[MergeStrategyChooser] with
		override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): MergeStrategyChooser =
			__jsOpt match
				case Some(__js) =>
					unbuilder.beginObject(__js)
					val default = unbuilder.readField[MergeStrategy]("default")
					val parts = unbuilder.readField[Vector[(Pattern, MergeStrategy)]]("parts")
					unbuilder.endObject()
					MergeStrategyChooser(default, parts)
				case None =>
					deserializationError("Expected JsObject but found None")
		override def write[J](obj: MergeStrategyChooser, builder: Builder[J]): Unit =
			builder.beginObject()
			builder.addField("default", obj.default)
			builder.addField("parts", obj.parts)
			builder.endObject()
	end MergeStrategyChooserJsonFormat

	given MainModuleJsonFormat: JsonFormat[MainModule] with
		override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): MainModule =
			__jsOpt match
				case Some(__js) =>
					unbuilder.beginObject(__js)
					val moduleName = unbuilder.readField[String]("moduleName")
					val mainClass = unbuilder.readField[Option[String]]("mainClass")
					unbuilder.endObject()
					MainModule(moduleName, mainClass)
				case None =>
					deserializationError("Expected JsObject but found None")
		override def write[J](obj: MainModule, builder: Builder[J]): Unit =
			builder.beginObject()
			builder.addField("moduleName", obj.moduleName)
			builder.addField("mainClass", obj.mainClass)
			builder.endObject()
	end MainModuleJsonFormat

	given JlinkLauncherJsonFormat: JsonFormat[JlinkLauncher] with
		override def read[J](__jsOpt: Option[J], unbuilder: Unbuilder[J]): JlinkLauncher =
			__jsOpt match
				case Some(__js) =>
					unbuilder.beginObject(__js)
					val name = unbuilder.readField[String]("name")
					val main = unbuilder.readField[MainModule]("main")
					unbuilder.endObject()
					JlinkLauncher(name, main)
				case None =>
					deserializationError("Expected JsObject but found None")
		override def write[J](obj: JlinkLauncher, builder: Builder[J]): Unit =
			builder.beginObject()
			builder.addField("name", obj.name)
			builder.addField("main", obj.mainModule)
			builder.endObject()
	end JlinkLauncherJsonFormat

end JsonProtocol
