package main;

import scala.util.Properties;

final class Main {
	private static void assertEquals(String expected, String actual) throws Exception {
		if (! expected.equals(actual)) {
			throw new Exception("Expected did not match actual\nExpected: " + expected + "\nActual:   " + actual);
		}
	}

	public static void main(String[] args) throws Exception {
		assertEquals("3.9.0", Properties.versionNumberString());
	}
}
