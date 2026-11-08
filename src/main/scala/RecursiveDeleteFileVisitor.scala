package name.rayrobdod.sbtJlink

import java.nio.file.*

final class RecursiveDeleteFileVisitor extends FileVisitor[Path]:
	override def postVisitDirectory(path: Path, ex: java.io.IOException): FileVisitResult =
		Files.delete(path)
		FileVisitResult.CONTINUE
	override def preVisitDirectory(path: Path, attrs: attribute.BasicFileAttributes): FileVisitResult =
		FileVisitResult.CONTINUE
	override def visitFile(path: Path, attrs: attribute.BasicFileAttributes): FileVisitResult =
		Files.delete(path)
		FileVisitResult.CONTINUE
	override def visitFileFailed(path: Path, ex: java.io.IOException): FileVisitResult =
		FileVisitResult.TERMINATE
