package lib.test

import lib.Lib

final class LibTest extends munit.FunSuite {
	test ("Can acccess lib.Lib") {
		assertEquals(Lib.message, "Foobarbaz")
	}

	test ("lib.Lib is on modulepath".ignore) {
			assertEquals(classOf[Lib].getModule().getName(), "lib");
	}
}
