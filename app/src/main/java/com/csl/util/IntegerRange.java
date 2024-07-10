package com.csl.util;

import lombok.Getter;

@Getter
public class IntegerRange {

	int min=0;
	int max=0;
	
	public IntegerRange(String s) {
		
		if (s.contains(":")) {
			String[] stokens=s.split(":");
			min = Integer.parseInt(stokens[0]);
			max = Integer.parseInt(stokens[1]);
		}
		else {
			min = Integer.parseInt(s);
			max=min;
		}
			
	}

    public String toString() {
		return ""+min +':'+max;
	}
	
	
	public static void main(String[] args) {
		
		System.out.println(new IntegerRange("1:56"));
		System.out.println(new IntegerRange("10"));
		
	}
	
}
