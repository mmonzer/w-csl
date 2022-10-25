package com.ucsl.json;

public class JsonSelectorElement {

	int index=-1;
	String field=null;
	boolean startSelector=false; 
	boolean array=false;
	boolean root=false;
	JsonSelectorElement parent=null;
	JsonSelectorElement child=null;
	

	public  JsonSelectorElement(int i) {
		index=i;
	}
	public  JsonSelectorElement(double d) {
		 index=(int)d;
	}
	public  JsonSelectorElement(String s) {
		if (s.trim().compareTo("*")==0) {
			startSelector=true;
		}
		else
			field=s;
	}


	public boolean isStarSelector() {
		return startSelector;
	}
	
	public boolean isIndex() {
		return field==null && !startSelector;
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getField() {
		return field;
	}
	
	
	
	public boolean isArray() {
		return array;
	}
	public void setArray(boolean array) {
		this.array = array;
	}
	
	
	public boolean isRoot() {
		return root;
	}
	public JsonSelectorElement setRoot(boolean root) {
		this.root = root;
		return this;
	}
	
	
	public String getName() {
		if (field==null) return "["+index+']';
		return field;
	}
	
	public JsonSelectorElement getParent() {
		return parent;
	}
	public void setParent(JsonSelectorElement parent) {
		this.parent = parent;
		parent.setChild(this);
	}
	
	
	public JsonSelectorElement getChild() {
		return child;
	}
	public void setChild(JsonSelectorElement child) {
		this.child = child;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		if (field==null) {
			if (startSelector) return "*";
			return ""+index;
		}
		return field;
	}
}
