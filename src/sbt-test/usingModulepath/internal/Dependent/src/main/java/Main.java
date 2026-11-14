package main;

import lib.Lib;

final class Main {
	private static void assertEquals(String expected, String actual) throws Exception {
		if (! expected.equals(actual)) {
			throw new Exception("Expected did not match actual\nExpected: " + expected + "\nActual:   " + actual);
		}
	}

	public static void main(String[] args) throws Exception {
		assertEquals("Foobarbaz", Lib.message);
		assertEquals("lib", Lib.class.getModule().getName());
		// If this is not running on the module path, `Lib.class.getModule().getName()` will be null
	}
}
