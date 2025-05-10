package main.Main;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

final class Main {
	public static void main(String[] args) {
		String raw = "<p>Hello <p><em>World</em>";
		String clean = Jsoup.clean(raw, Safelist.simpleText()); 
		System.out.println(clean);
	}
}
