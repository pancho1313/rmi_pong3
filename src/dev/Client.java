package dev;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {

	static MyUtil U = new MyUtil();
	private Player myPlayer;
	private ISServer sServer;
	private IPongServer server;
	private String serverIp;
	
	public Client(String ipLocalHost, String ipSServer){
		System.setProperty("java.rmi.server.hostname", ipLocalHost);
		U.localMessage("Connecting to PongServer...");
		
		//contactar al SServer y obtener la ip del servidor activo
		try {
			sServer = (ISServer) Naming.lookup("//"+ipSServer+":1099/SServer");
			serverIp = sServer.whoIstheServer();
		} catch (MalformedURLException e) {
			cantPlay();
		} catch (RemoteException e) {
			cantPlay();
		} catch (NotBoundException e) {
			cantPlay();
		}
		
		
		//contactar al server
		try {
			server = (IPongServer) Naming.lookup("//"+serverIp+":1099/PongServer");
		} catch (MalformedURLException e) {
			cantPlay();
		} catch (RemoteException e) {
			cantPlay();
		} catch (NotBoundException e) {
			cantPlay();
		}
		
		//crear myPlayer
		try {
			myPlayer = new Player(ipSServer);
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//pedir jugar pong
		try {
			if(server.iWantToPlay((IPlayer)myPlayer)){
				startPongWindow();
			}else{
				cantPlay();
			}
		} catch (RemoteException e) {
			cantPlay();
		}
		
		
		
	}
	
	private void startPongWindow(){
		new Pong(myPlayer, server);
	}
	
	private void cantPlay(){
		U.localMessage("Not now my friend, try later.");
		System.exit(0);
	}
	
	public static void main(String[] args) {
		String ipLocalHost = U.getArg(args, 0, "ERROR: no se ha especificado LOCALHOST IP!");
		String ipSServer = U.getArg(args, 1, "ERROR: no se ha especificado SSERVER IP!");
		new Client(ipLocalHost, ipSServer);
	}
	
}
