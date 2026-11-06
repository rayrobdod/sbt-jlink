package name.rayrobdod.sbtJlink.sbtTest.isModule.standalone;

final class Main {
	private static final String expected = "name.rayrobdod.sbtJlink.sbtTest.isModule.standalone";

	public static void main(String[] args) throws Exception {
		String actual = Main.class.getModule().getName();
		// If this is not running on the module path, `actual` will be null

		if (! expected.equals(actual)) {
			throw new Exception("Expected did not match actual\nExpected: " + expected + "\nActual: " + actual);
		}
	}
}
