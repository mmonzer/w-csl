package com.csl.udp;
import com.xcsl.json.Json;


public class UDPVarDescriptor {
	

/*
 * {
		  "name":"T2",
	    {"name":"TSET","value":"0","description":"tem setpoint","block":"b1",
		 	    "targets":["T1","T2"]},
		 	    {"name":"T","value":"0","description":"temp","block":"b1"},
		 		{"name":"V","value":"0","description":"vol"}	
		  }
 */



		String name="";
	;
		String description="";
		String block="";
		double value=0;
	
		

		public UDPVarDescriptor(Json jv) {

			this.name=getStringAttribute(jv, "name");
			this.description=getStringAttribute(jv, "description");
			this.block=getStringAttribute(jv, "block");
			this.value=getDoubleAttribute(jv, "value");
	
		}

		public String getStringAttribute(Json j,String n) {
			if (j==null) return "";
			Json o = j.get(n);
			if (o==null) return "";
			return o.asString();
		}
		
		public int getIntAttribute(Json j,String n) {
			if (j==null) return -1;
			Json o = j.get(n);
			if (o==null) return -1;
			return o.asInteger();
		}
		public double getDoubleAttribute(Json j,String n) {
			if (j==null) return -1;
			Json o = j.get(n);
			if (o==null) return -1;
			return o.asDouble();
		}


		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getBlock() {
			return block;
		}

		public void setBlock(String block) {
			this.block = block;
		}

		public double getValue() {
			return value;
		}

		public void setValue(double value) {
			this.value = value;
		}

	

}
