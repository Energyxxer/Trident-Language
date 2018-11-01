package com.energyxxer.util;

import java.util.ArrayList;

public class Stack {

	private ArrayList<MethodInvocation> stack = new ArrayList<MethodInvocation>();
	
	public Stack() {}
	
	public void push(MethodInvocation mi) {
		stack.add(mi);
	}
	
	public MethodInvocation pop() {
		if(stack.size() > 0) {
			MethodInvocation rv = stack.get(stack.size()-1);
			stack.remove(stack.size()-1);
			return rv;
		} else {
			return null;
		}
	}
	
	public boolean find(MethodInvocation mi) {
		for(int i = 0; i < stack.size(); i++) {
			if(stack.get(i).equals(mi)) {
				return true;
			}
		}
		return false;
	}
	
	public int size() {
		return stack.size();
	}

	public void add(Stack other) {
		this.stack.addAll(other.stack);
	}

	public Stack clone() {
		Stack st = new Stack();
		st.add(this);
		return st;
	}
}