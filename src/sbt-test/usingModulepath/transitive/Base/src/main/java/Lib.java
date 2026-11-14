package lib;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class Lib {
	public static void apply() throws Exception {
		String expected = "Hello <em>World</em>";

		String raw = "<p>Hello <p><em>World</em>";
		String actual = Jsoup.clean(raw, Safelist.simpleText());

		if (! actual.equals(expected)) {
			throw new Exception("Expected did not match actual\nExpected: " + expected + "\nActual: " + actual);
		}
	}
}
