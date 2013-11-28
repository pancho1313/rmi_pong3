package dev;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;


public class SServer extends UnicastRemoteObject implements ISServer{

	public static final int OK = 1;
	public static final int SERVER_DOWN = 2;
	
	public int sServerState;//estado del SServer
	/////////////////////////////////////////////////////////////////////
	
	public ArrayList<String> serversIp;
	public String activeServer;
	private int nPlayers;
	private int winScore;
	private int activePlayers;
	private IPlayer[] players;
	private int[] playersScore;
	private int lastPlayerRebound;
	private int serverNextState;
	
	public boolean refreshServers;
	
	static MyUtil U = new MyUtil();
	
	public SServer(int numPlayers, int winScore) throws RemoteException{
		sServerState = OK;
		serversIp = new ArrayList<String>();
		refreshServers = false;
		activeServer = "";
		this.nPlayers = numPlayers;
		this.winScore = winScore;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean iWantToServe(String ip) throws RemoteException{
		if(sServerState == SERVER_DOWN && ip.equals(activeServer)){
			habemusPongServer();
			U.localMessage("It's ALIVE!!! :D");
			return true;
		}
		boolean youActive = false;
		U.localMessage("<< ["+ip+"]: deseo servir...");
		
		//si ya existia esta ip en la lista de servers
		for(String s : serversIp){
			if(s.equals(ip)){
				U.localMessage(">> ip repetida!  >:o");
				return false;
			}
		}
		
		serversIp.add(ip);
		U.localMessage("{"+ip+"} agregado a serversIP["+(serversIp.size()-1)+"]");
		if(serversIp.size() == 1){
			activeServer = ip;
			setInitialServer(ip);
			U.localMessage("activeServer = serversIP["+activeServer+"] = {"+ip+"}");
			youActive = true;
		}
		refreshServers = true;
		return youActive;
	}
	
	public String whoIstheServer() throws RemoteException{
		U.localMessage("<< whoIstheServer?");
		if(sServerState==OK && !activeServer.equals("")){
			U.localMessage(">> ["+activeServer+"]");
			return activeServer;
		}
		
		U.localMessage(">> no hay un server activo!");
		return "";//TODO: esto es muy feo!
		
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private boolean restoreActiveServer(){
		IPongServer server;
		
		//contactar al servidor
		try {
			server = (IPongServer) Naming.lookup("//"+activeServer+":1099/PongServer");
			
			//enviar parametros al server
			try {
				server.recieveServerSettings(nPlayers, winScore, activePlayers, players, playersScore, lastPlayerRebound, serverNextState);
				return true;
			} catch (RemoteException e1) {
				return false;
			}
			
		} catch (MalformedURLException e) {
			return false;
		} catch (RemoteException e) {
			return false;
		} catch (NotBoundException e) {
			return false;
		}
	}
	
	private void setInitialServer(String ipServer){
		IPongServer server;
		
		//contactar al servidor inicial
		try {
			server = (IPongServer) Naming.lookup("//"+ipServer+":1099/PongServer");
			
			//crear parametros iniciales del server
			activePlayers = 0;
			players = new IPlayer[4];
			playersScore = new int[4];
			lastPlayerRebound = -1;
			serverNextState = PongServer.WAITING_FOR_PLAYERS;
			
			//enviar parametros iniciales al server
			try {
				server.recieveServerSettings(nPlayers, winScore, activePlayers, players, playersScore, lastPlayerRebound, serverNextState);
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean deadServer(String ip){
		if(ip.equals(activeServer) && sServerState==OK){
			U.localMessage("[SSLoop]:");
			activeServerDown();
			return false;
		}
		
		if(serversIp.remove(ip)){
			U.localMessage("ip "+ip+" descartada como server  :,I");	
			refreshServers = true;
			return true;
		}
		return false;
	}
	
	public static void main(String[] args) {
		String ipLocalHost = U.getArg(args, 0, "ERROR: no se ha especificado LOCALHOST IP!");
		String nPlayers = U.getArg(args, 1, "ERROR: no se ha especificado la cantidad de jugadores!");
		//String winScore = U.getArg(args, 2, "ERROR: no se ha especificado el puntaje de termino!");
		
		int numPlayers;
		if(nPlayers.equals("2")){
			numPlayers = 2;
		}else if(nPlayers.equals("3")){
			numPlayers = 3;
		}else if(nPlayers.equals("4")){
			numPlayers = 4;
		}else{
			numPlayers = 2;
		}
		
		int winScore = 10;
		////////////////////////////////
		
		System.setProperty("java.rmi.server.hostname", ipLocalHost);
		ISServer sServer = null;
		try {
			sServer = new SServer(numPlayers, winScore);
			Naming.rebind("rmi://localhost:1099/SServer", sServer);
			U.localMessage("SServer iniciado para "+numPlayers+" players.");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//loop
		new SSLoop((SServer)sServer);
	}
	
	public void habemusPongServer(){
		
		//restaurar estado del Server
		restoreActiveServer();
		
		//restaurar estado de jugadores
		for(int id = 0; id < players.length; id++){
			IPlayer player = players[id];
			if(player != null){
				try {
					player.refreshServerIp(activeServer);
					player.endPause();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		//restaurar el estado de SS
		sServerState = OK;
	}
	
	private void activeServerDown(){
		U.localMessage("ERROR GRAVE: ha fallecido el PongServer Activo!  D:");
		
		//anunciar el problema
		sServerState = SERVER_DOWN;

		//avisarle a los clientes
		for(int id = 0; id < players.length; id++){
			IPlayer player = players[id];
			if(player != null){
				try {
					player.startPause();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}	
	}

	public void serverDown() throws RemoteException{
		if(sServerState==OK){
			U.localMessage("[Pong]: ");
			activeServerDown();
		}
	}
	
	public void refreshGeneralState(Object[] resp){
		
		nPlayers = (Integer) resp[0];
		winScore = (Integer) resp[1];
		activePlayers = (Integer) resp[2];
		players = (IPlayer[]) resp[3];
		playersScore = (int[]) resp[4];
		lastPlayerRebound = (Integer) resp[5];
		serverNextState = (Integer) resp[6];
		 
	}
}
