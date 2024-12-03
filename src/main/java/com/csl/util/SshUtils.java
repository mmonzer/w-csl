package com.csl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshUtils {
	Session session;
	/**
	 * Permet d'initaliser la connexion ssh (Attention cette version ne fait pas de vérification de KnowHost)
	 * @param user l'username 
	 * @param password le mot de passe (sera remplacé par une authentification par certificat)
	 * @param host l'adresse ip de l'hote distant
	 * @param port le port de l'hote distant
	 */
	public SshUtils(String user,String password, String host, int port){
		try{
			JSch jsch=new JSch();  
		    session=jsch.getSession(user, host, port);
		    session.setPassword(password);
		    java.util.Properties config = new java.util.Properties(); 
		    config.put("StrictHostKeyChecking", "no");
		    session.setConfig(config);
		    session.setTimeout(4000);
		    session.connect();
		  }  
		  catch(Exception e) {
				System.out.println("Le tap "+host+" est innacessible pour le moment (Jsch error)");
		  }
	}
	/**
	 * Initialise une connexion SSH en vérifiant que la signature de l'hote distant est connue dans le fichier knowHost dont le chemin est passé en argument
	 * @param user l'username 
	 * @param password le mot de passe (sera remplacé par une authentification par certificat)
	 * @param host l'adresse ip de l'hote distant
	 * @param port le port de l'hote distant
	 * @param knownHostsPath Le chemin vers le fichier know Host local (en général ~/.ssh/known_hosts
	 */
	public SshUtils(String user,String password, String host, int port, String knownHostsPath){
		  try{
		    JSch jsch=new JSch();  
		    jsch.setKnownHosts(knownHostsPath);
		    session=jsch.getSession(user, host, port);
		  	Hashtable<String, String> config = new Hashtable<String, String>();
		  	config.put("server_host_key", "ecdsa-sha2-nistp256");
		  	session.setConfig(config);
		    session.setPassword(password);
		    session.connect();
		  }  
		  catch(Exception e) {
				System.out.println("Le tap "+host+" est innacessible pour le moment (Jsch error)");
		  }
	}	  
	  
	/**
	 * Permet d'exécuter une commande sur l'hote initialité
	 * @param command Chaine de caractères correspondante à la commande à exécuter
	 * @return Le résultat de la commande
	 * @throws JSchException
	 * @throws IOException
	 */
	public String remoteExec(String command) throws JSchException, IOException {
		ArrayList<String> resultPart = new ArrayList<String>();
		Channel channel=session.openChannel("exec");
		((ChannelExec)channel).setCommand(command);
		channel.setInputStream(null);
		System.setErr(System.err);
		InputStream in=channel.getInputStream();
		channel.connect();
		byte[] tmp=new byte[1024];
		while(true){
			while(in.available()>0){
				int i=in.read(tmp, 0, 1024);
				if(i<0)break;
					resultPart.add(new String(tmp, 0, i));
			}
			if(channel.getExitStatus()==-1){
				if(in.available()>0) continue;
				//System.out.println("exit-status: "+channel.getExitStatus());
				break;
			}
				try{Thread.sleep(1000);}catch(Exception ee){}
		}
		channel.disconnect();
		session.disconnect();
		String result = "";
		for(String s : resultPart)
		result = result+s+"\n";
		return result;
	}
	
	
	public String remoteExecNoWait(String command) throws JSchException, IOException {
		ArrayList<String> resultPart = new ArrayList<String>();
		Channel channel=session.openChannel("exec");
		((ChannelExec)channel).setCommand(command);
		channel.setInputStream(null);
		System.setErr(System.err);
		InputStream in=channel.getInputStream();
		channel.connect();
		byte[] tmp=new byte[1024];
		int n=0;
		while(n<3){
			while(in.available()>0){
				int i=in.read(tmp, 0, 1024);
				if(i<0)break;
					resultPart.add(new String(tmp, 0, i));
			}
			if(channel.getExitStatus()==-1){
				if(in.available()>0) continue; 
				//System.out.println("exit-status: "+channel.getExitStatus());
				break;
			}
				try{Thread.sleep(1000);}catch(Exception ee){}
				n++;
		}
		channel.disconnect();
		session.disconnect();
		String result = "";
		for(String s : resultPart)
		result = result+s+"\n";
		return result;
	}
	
	public void endConnection() {
		this.session.disconnect();
	}
	
	private int checkAck(InputStream in) throws IOException{
	    int b=in.read();
	    if(b==0) return b;
	    if(b==-1) return b;

	    if(b==1 || b==2){
	      StringBuffer sb=new StringBuffer();
	      int c;
	      do {
		c=in.read();
		sb.append((char)c);
	      }
	      while(c!='\n');
	      if(b==1){ // error
		System.out.print(sb.toString());
	      }
	      if(b==2){ // fatal error
		System.out.print(sb.toString());
	      }
	    }
	    return b;
	  }
	
	/**
	 * Permet d'envoyer un fichier à une machine distante avec la connexion ssh déjà initialisée
	 * @param localFilePath Chemin vers le fichier local
	 * @param distantFilePath Chemin vers le fichier distant
	 * @throws IOException 
	 * @throws JSchException 
	 */
	public void sendFile(String localFilePath, String distantFilePath) throws IOException, JSchException {
		String lfile=localFilePath;
	    String rfile=distantFilePath;
	    boolean ptimestamp = false;

		// exec 'scp -t rfile' remotely
		rfile=rfile.replace("'", "'\"'\"'");
		rfile="'"+rfile+"'";
		String command="scp " + (ptimestamp ? "-p" :"") +" -t "+rfile;
		Channel channel=session.openChannel("exec");
		((ChannelExec)channel).setCommand(command);
		
		// get I/O streams for remote scp
		OutputStream out=channel.getOutputStream();
		InputStream in=channel.getInputStream();
		channel.connect();
		if(checkAck(in)!=0){
			////System.exit(0);
		}
		
		File _lfile = new File(lfile);
		long filesize=_lfile.length();
		command="C0644 "+filesize+" ";
		if(lfile.lastIndexOf('/')>0){
			command+=lfile.substring(lfile.lastIndexOf('/')+1);
		}
		else{
			command+=lfile;
		}
		command+="\n";
		out.write(command.getBytes()); out.flush();
		if(checkAck(in)!=0){
			//System.exit(0);
		}
		
		// send a content of lfile
		FileInputStream fis=null;
		fis=new FileInputStream(lfile);
		byte[] buf=new byte[1024];
		while(true){
			int len=fis.read(buf, 0, buf.length);
			if(len<=0) break;
			out.write(buf, 0, len); //out.flush();
		}
		fis.close();
		fis=null;
		// send '\0'
		buf[0]=0; out.write(buf, 0, 1); out.flush();
		if(checkAck(in)!=0){
			//System.exit(0);
		}
		out.close();
		
		channel.disconnect();
		
	}
	
	/**
	 * Permet d'envoyer un fichier à une machine distante avec la connexion ssh déjà initialisée
	 * @param localFilePath Chemin vers le fichier local
	 * @param distantFilePath Chemin vers le fichier distant
	 * @throws IOException 
	 * @throws JSchException 
	 */
	public void getFile(String distantFilePath, String localFilePath) throws IOException, JSchException {
		String lfile=localFilePath;
	    String rfile=distantFilePath;
	    boolean ptimestamp = false;      
	    String prefix=null;
	    FileOutputStream fos=null;
	 // exec 'scp -f rfile' remotely
	      rfile=rfile.replace("'", "'\"'\"'");
	      rfile="'"+rfile+"'";
	      String command="scp -f "+rfile;
	      Channel channel=session.openChannel("exec");
	      ((ChannelExec)channel).setCommand(command);

	      // get I/O streams for remote scp
	      OutputStream out=channel.getOutputStream();
	      InputStream in=channel.getInputStream();

	      channel.connect();

	      byte[] buf=new byte[1024];

	      // send '\0'
	      buf[0]=0; out.write(buf, 0, 1); out.flush();

	      while(true){
	    	  int c=checkAck(in);
	        if(c!='C'){
		  break;
		}

	        // read '0644 '
	        in.read(buf, 0, 5);

	        long filesize=0L;
	        while(true){
	          if(in.read(buf, 0, 1)<0){
	            // error
	            break; 
	          }
	          if(buf[0]==' ')break;
	          filesize=filesize*10L+(long)(buf[0]-'0');
	        }

	        String file=null;
	        for(int i=0;;i++){
	          in.read(buf, i, 1);
	          if(buf[i]==(byte)0x0a){
	            file=new String(buf, 0, i);
	            break;
	  	  }
	        }

	        // send '\0'
	        buf[0]=0; out.write(buf, 0, 1); out.flush();

	        // read a content of lfile
	        fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
	        int foo;
	        while(true){
	          if(buf.length<filesize) foo=buf.length;
		  else foo=(int)filesize;
	          foo=in.read(buf, 0, foo);
	          if(foo<0){
	            // error 
	            break;
	          }
	          fos.write(buf, 0, foo);
	          filesize-=foo;
	          if(filesize==0L) break;
	        }
	        fos.close();
	        fos=null;

		if(checkAck(in)!=0){
		  System.exit(0);
		}

	        // send '\0'
	        buf[0]=0; out.write(buf, 0, 1); out.flush();
	      }

	}
}
