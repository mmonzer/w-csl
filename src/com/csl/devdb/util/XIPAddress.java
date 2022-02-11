package com.csl.devdb.util;

public class XIPAddress {

	static String DEFAULT_LOCAL_NETWORK_NAME="local";
	static String DEFAULT_GLOBAL_NETWORK_NAME="global";
			
	boolean valid=false;
	
	boolean privateIP=true;
	
	String error="Invalid address";
	String ip="";
	String network="";
	
	public XIPAddress(String s) {
		// TODO Auto-generated constructor stub
	
		parse(s);
		
	}
	
	public XIPAddress() {
		
	}
	
	public XIPAddress(String ip, String network) {
		// TODO Auto-generated constructor stub
	
		String s=ip;
		if (!network.isEmpty()) s=s+"@"+network;
		parse(ip);
		
		
	}
	
	
	public XIPAddress  normalize(String ip, String network)  {
		
		if (ip.isEmpty()) return this;
		
		if (ip.compareTo("localhost")==0) ip="0.0.0.0";
		
		int[] values=parseIP4Address(ip);
		if (values==null) return this;
		
		if (isGlobal(values)) {
			this.ip=ip;
			this.network=DEFAULT_GLOBAL_NETWORK_NAME;
			valid=true;
			privateIP=false;
		}
		else {
			this.ip=ip;
			if (network.isEmpty())
				this.network=DEFAULT_LOCAL_NETWORK_NAME;
			else
				this.network=network;
			valid=true;
			privateIP=true;
		}
		
		return this;
	}
	
	public XIPAddress  normalize(String ip)  {
		
		
		int p=ip.indexOf("@");
		String foundNet="";
		
		if (ip==null) return this;
		if (p>=0) {
			foundNet=ip.substring(p+1,ip.length());
			ip=ip.substring(0,p);		
		}
		
		if (ip.isEmpty()) return this;
		
		if (ip.compareTo("localhost")==0) ip="0.0.0.0";
		
		
		int[] values=parseIP4Address(ip);
		if (values==null) return this;
		
		this.ip=ip;
		
		if (isGlobal(values)) {
			this.ip=ip;
			this.network=DEFAULT_GLOBAL_NETWORK_NAME;
			valid=true;
			privateIP=false;
		}
		else {
			if (foundNet.isEmpty()) foundNet="local";
			network=foundNet;
			valid=true;
			privateIP=true;
		}
		
		return this;
	}
	
	
	
	private boolean isGlobal(int[] values) {
		
		if ((values[0]==192)&&(values[1]==168)) {
			return false;
		}
		else if ((values[0]==172)&&(values[1]>=16)&&(values[1]<=31)) {
			return false;
		}
		else if ((values[0]==10)) {
			return false;
		}
		else if ((values[0]==127)&&(values[1]==0)&&(values[0]==0)&&(values[0]==1)) {
			return false;
		}
		else if ((values[0]==0)&&(values[1]==0)&&(values[0]==0)&&(values[0]==0)) {
			return false;
		}
		return true;
	}
	
	
	private void parse(String s) {
	
		int p=s.indexOf("@");
		String foundNet="";
		
		if (s==null) return;
		if (p>=0) {
			foundNet=s.substring(p+1,s.length());
			s=s.substring(0,p);		
		}
		if (s.isEmpty()) return;
		
		if (s.compareTo("localhost")==0) s="0.0.0.0";
		
		int[] values=parseIP4Address(s);
		if (values==null) return;
		
		ip=s;
		
		boolean global=isGlobal(values);
		
		if (global) {
			network="global";
			privateIP=false;
			if (!foundNet.isEmpty()) {
				if (foundNet.compareTo(DEFAULT_GLOBAL_NETWORK_NAME)!=0) {
					error=error+": cannot specify network for not private ip";
					return;  // invalid address
				}
				else
					valid=true;
			}
			else
				valid=true;
		}
		else {
			if (foundNet.isEmpty()) foundNet="local";
			network=foundNet;
			valid=true;
			privateIP=true;
		}
		
		
	}
	
	public static int[] parseIP4Address(String ipAddress) {
		
		int[] values=new int[4];
        if (ipAddress.matches("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")) {
            String[] groups = ipAddress.split("\\.");

            for (int i = 0; i <= 3; i++) {
                String segment = groups[i];
                if (segment == null || segment.length() <= 0) {
                    return null;
                }

                int value = 0;
                try {
                    value = Integer.parseInt(segment);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (value > 255) {
                    return null;
                }
                values[i]=value;
            }
            return values;
        }
        return null;
    }



	public boolean isValid() {
		return valid;
	}



	public boolean isPrivateIP() {
		return privateIP;
	}



	public String getError() {
		return error;
	}




	public String getIP() {
		return ip;
	}




	public String getNetwork() {
		return network;
	}

	public String toString() {
		
		String s;
		if (isPrivateIP())
			s= ip+"@"+network;
		else
			s= ip;
		
		if (!valid) return s+"("+error+")";
		
		return s;
	}
	
	public String toKey() {
		
		//if (isPrivateIP())
		//	return ip+"@"+network;
		
		return ip;
	}

	public static void main(String[] args) {
		
		
		System.out.println(new XIPAddress("1.1.1.1@global"));
		System.out.println(new XIPAddress("192.168.1.1"));
		System.out.println(new XIPAddress("192.168.1.1@local2"));
		
		System.out.println(new XIPAddress().normalize("192.168.1.1").toKey());
		System.out.println(new XIPAddress().normalize("10.1.1.1@loac34").toKey());
		System.out.println(new XIPAddress().normalize("11.1.1.1@zaza").toKey());
		
	}


	
	
}
