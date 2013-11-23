package dev;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


public class Server {

	static MyUtil U = new MyUtil();
	
	private static void noSSFound(){
		U.localMessage("Ups! cant connect to SS :o");
		System.exit(0);
	}
	
	public static void main(String[] args) {
		String ipLocalHost = U.getArg(args, 0, "ERROR: no se ha especificado LOCALHOST IP!");
		String ipSServer = U.getArg(args, 1, "ERROR: no se ha especificado SSERVER IP!");
		////////////////////////////////
		
		//revisar si ya existe un PongServer local
		boolean clean = false;
		try {
			IPongServer dummy = (IPongServer) Naming.lookup("//"+ipLocalHost+":1099/PongServer");
			double aux = dummy.getServerLoad();
			U.localMessage("<< [actual server] server load = "+aux);
		} catch (MalformedURLException e1) {
			clean = true;
		} catch (RemoteException e1) {
			clean = true;
		} catch (NotBoundException e1) {
			clean = true;
		}
		if(!clean){
			U.localMessage("Ups! ya hay un PongServer :s");
			return;
		}
		
		
		//publicar el server
		try {
			System.setProperty("java.rmi.server.hostname", ipLocalHost);
			IPongServer pongServer = new PongServer(ipLocalHost);
			Naming.rebind("rmi://localhost:1099/PongServer", pongServer);
			U.localMessage("PongServer publicado en ["+ipLocalHost+"]");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//avisarle al SS que deseo servir
		try {
			U.localMessage("le digo al SS que deseo servir...");
			ISServer sServer = (ISServer) Naming.lookup("//"+ipSServer+":1099/SServer");
			if(sServer.iWantToServe(ipLocalHost)){
				U.localMessage("Soy el servidor activo :D");
			}else{
				U.localMessage("Soy servidor de reserva :|");
			}
		} catch (MalformedURLException e) {
			noSSFound();
		} catch (RemoteException e) {
			noSSFound();
		} catch (NotBoundException e) {
			noSSFound();
		}
	}

}
