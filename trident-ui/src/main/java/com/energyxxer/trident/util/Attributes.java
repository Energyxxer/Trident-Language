package com.energyxxer.trident.util;

import java.util.ArrayList;

public class Attributes {
	private ArrayList<Object> attributes = new ArrayList<Object>();
	
	public void put(Object attr) {
		if(attributes.indexOf(attr) < 0) {
			attributes.add(attr);
		}
	}
	
	public boolean has(Object attr) {
		return attributes.indexOf(attr) >= 0;
	}
	
	public ArrayList<Object> getAll() {
		return attributes;
	}
	
	@Override
	public String toString() {
		return attributes.toString().substring(1,attributes.toString().length()-2);
	}
}
