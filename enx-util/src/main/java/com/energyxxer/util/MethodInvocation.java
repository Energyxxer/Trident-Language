package com.energyxxer.util;

import java.util.Arrays;
import java.util.List;

public class MethodInvocation {
	private Object object;
	private String methodName;
	private List<String> methodSignature;
	private List<Object> params;

	public MethodInvocation(Object object, String methodName, String[] methodSignature, Object[] params) {
		this.object = object;
		this.methodName = methodName;
		this.methodSignature = Arrays.asList(methodSignature);
		this.params = Arrays.asList(params);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof MethodInvocation)) return false;
		
		MethodInvocation o = (MethodInvocation) obj;

		return o.object == object &&
				o.methodName.equals(methodName) &&
				o.methodSignature.equals(methodSignature) &&
				o.params.equals(params);
	}
	
	@Override
	public String toString() {
		String o = "";
		o += object.getClass().getName() + "." + methodName + "(";
		for(int i = 0; i < methodSignature.size(); i++) {
			o += methodSignature.get(i) + " " + params.get(i);
			if(i < methodSignature.size()-1) {
				o += ", ";
			}
		}
		o += ");";
		return o;
	}
}
