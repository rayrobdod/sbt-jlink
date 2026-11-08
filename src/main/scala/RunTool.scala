package name.rayrobdod.sbtJlink

import scala.util.Using

object RunTool:
	def apply(
		toolName: String,
		toolArgs: Seq[String],
		stream: sbt.std.TaskStreams[?],
		streamKeyPrefix: String,
	): Unit =
		val outStreamKey: String = s"${streamKeyPrefix}.out"
		val errStreamKey: String = s"${streamKeyPrefix}.err"
		val logger = stream.log

		val tool = java.util.spi.ToolProvider.findFirst(toolName).get
		val result =
			Using.resource(stream.text(outStreamKey)): toolout =>
				Using.resource(stream.text(errStreamKey)): toolerr =>
					tool.run(toolout, toolerr, toolArgs*)

		if 0 != result then
			var line: String = "";

			Using.resource(stream.readText(stream.key, outStreamKey)): toolout =>
				while
					line = toolout.readLine()
					null != line
				do
					logger.error(line)

			Using.resource(stream.readText(stream.key, errStreamKey)): toolerr =>
				while
					line = toolerr.readLine()
					null != line
				do
					logger.error(line)

			sys.error(s"$toolName failed: $result")
	end apply
end RunTool
