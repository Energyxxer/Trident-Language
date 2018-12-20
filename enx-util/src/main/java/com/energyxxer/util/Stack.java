package com.energyxxer.util;

public class Stack {

	private java.util.Stack<MethodInvocation> stack = new java.util.Stack<>();
	
	public Stack() {}
	
	public void push(MethodInvocation mi) {
		stack.push(mi);
	}
	
	public MethodInvocation pop() {
		if(!stack.isEmpty()) {
		    return stack.pop();
		} else {
			return null;
		}
	}
	
	public boolean find(MethodInvocation mi) {
		for(int i = stack.size()-1; i >= 0; i--) {
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