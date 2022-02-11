package com.csl.udp;
import com.xcsl.json.Json;


public class UDPTargetDescriptor {
	

/*
 * {
		  "name":"T2",
		  "host_target":"localhost",
		  "port_target":8001,
		  "id_of_target":"cible",
		  "flow_number":1
		  }
 */



		public String name="";
	;
	public String hostTarget="";
	public int portTarget=0;
	public String idOfTarget="";
	public int flowNumber=0;
		

		public UDPTargetDescriptor(Json jv) {

			this.name=getStringAttribute(jv, "name");
			this.hostTarget=getStringAttribute(jv, "host_target");
			this.portTarget=getIntAttribute(jv, "port_target");
			this.idOfTarget=getStringAttribute(jv, "id_of_target");
			this.flowNumber=getIntAttribute(jv, "flow_number");

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


		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}

		public String getHostTarget() {
			return hostTarget;
		}

		public void setHostTarget(String hostTarget) {
			this.hostTarget = hostTarget;
		}

		public int getPortTarget() {
			return portTarget;
		}

		public void setPortTarget(int portTarget) {
			this.portTarget = portTarget;
		}

		public String getIdOfTarget() {
			return idOfTarget;
		}

		public void setIdOfTarget(String idOfTarget) {
			this.idOfTarget = idOfTarget;
		}

		public int getFlowNumber() {
			return flowNumber;
		}

		public void setFlowNumber(int flowNumber) {
			this.flowNumber = flowNumber;
		}
	
		
	
		public String toString() {
			return hostTarget+':'+portTarget+' '+idOfTarget+':'+flowNumber;
		}

}
