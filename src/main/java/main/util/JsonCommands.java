package main.util;

public class JsonCommands {
	public void init() {

	}

	static public String startOf(String s) {
		int MAX=50;
		if (s.length()<=MAX) return s;
		else return s.substring(0,MAX-1)+"...";
	}

}
