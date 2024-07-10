package main.util;

public class JsonCommands {

<<<<<<< HEAD

	//static CSLAlertManager cslAlertManager= CSLAlertManager.instance.setname("Intrusion detection");



	static public void init() {

//		CSLServer.getJCmdManager().registerCmd("test_alert", new JsonCmd() {
//
//			@Override
//			public Json exec(Json params) {
//				System.out.println("start exec");
//				System.out.println("Exec JCmd test_cmd :"+params);
//				System.out.println("Fin exec");
//				Json j=Json.object();
//				j.set("result", "ok");
//				j.set("value",1);
//
//				double x=Math.random()*1000;
//
//				int n=(int)(Math.random()*5);
//
//				String[] w = new String[] {"INFO", "TOLERABLE", "MODERATE", "HIGH", "CRITICAL"};
//
//
//				cslAlertManager.sendAlert(
//						new AlertDescriptor(n,"test alert","xxx=testval"),true,false);
//				cslAlertManager.sendAlert(new AlertDescriptor(0,"test alert 0","xxx=testval"),true,false);
//				cslAlertManager.sendAlert(new AlertDescriptor(1,"test alert 1","xxx=testval"),true,false);
//				cslAlertManager.sendAlert(new AlertDescriptor(2,"test alert 2","xxx=testval"),true,false);
//				cslAlertManager.sendAlert(new AlertDescriptor(3,"test alert 3","xxx=testval"),true,false);
//				cslAlertManager.sendAlert(new AlertDescriptor(4,"test alert 4","xxx=testval"),true,false);
//						
//				return j;
//			}
//		});
//
//		
//		CSLServer.getJCmdManager().registerCmd("exit", new JsonCmd() {
//
//			@Override
//			public Json exec(Json params) {
//				System.out.println("start exec");
//				System.out.println("Exec JCmd test_cmd2 :"+params);
//				System.out.println("Fin exec");
//				Json j=Json.object();
//				j.set("result", "ok");
//				j.set("value",1);
//
//				System.exit(0);
//
//				
//				return j;
//			}
//		});
//		
//		CSLServer.getJCmdManager().registerCmd("test_cmd2", new JsonCmd() {
//
//			@Override
//			public Json exec(Json params) {
//				System.out.println("start exec");
//				System.out.println("Exec JCmd test_cmd2 :"+params);
//				System.out.println("Fin exec");
//				Json j=Json.object();
//				j.set("result", "ok");
//				j.set("value",1);
//
//				int n=(int)(Math.random()*5);
//
//				String[] w = new String[] {"INFO", "TOLERABLE", "MODERATE", "HIGH", "CRITICAL"};
//
//
//				cslAlertManager.sendAlert(new AlertDescriptor(n,"test alert","xxx=testval"),true,false);
//				return j;
//			}
//		});

		
	}


=======
	static public void init() {
		
	}

>>>>>>> origin/feature/refactor_code
	static public String startOf(String s) {
		int MAX=50;
		if (s.length()<=MAX) return s;
		else return s.substring(0,MAX-1)+"...";
	}
<<<<<<< HEAD
	
	

	

	static {

		//	cslAlertManager.init();

	}
=======
>>>>>>> origin/feature/refactor_code

}
