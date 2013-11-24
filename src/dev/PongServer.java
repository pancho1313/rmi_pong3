package dev;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;

public class PongServer extends UnicastRemoteObject implements IPongServer{
	
	public static final int WAITING_FOR_PLAYERS = 0;
	public static final int PLAYING_MATCH = 1;
	public static final int MATCH_FINISHED = 2;
	public static final int MIGRATING = 3;
	
	private int serverState;//estado del pongServer
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	//private static final long serialVersionUID = 6311160989789331741L;
	private int nPlayers;
	private int winScore;
	private String ipHost;
	
	private IPlayer[] players;
	private int[] playersScore;
	private int lastPlayerRebound;
	private int activePlayers;
	
	private static OperatingSystemMXBean mbean;
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	private void reInitMatch(){
		
		activePlayers = 0;
		players = new IPlayer[4];
		
		playersScore = new int[4];
		lastPlayerRebound = -1;
		
		serverState = WAITING_FOR_PLAYERS;
	}
	
	private int addToPlayers(IPlayer p){
		for(int i = 0; i < players.length; i++){
			if(players[i] == null){
				players[i] = p;
				activePlayers++;
				
				try {
					p.setPlayerId(i);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return i;
			}
		}
		return -1;
	}
	
	private void shakeHandsPlease(){
		for(int id = 0; id < players.length; id++){
			IPlayer player = players[id];
			if(player != null){
				
				try {
					player.informPosition();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}
	
	private boolean addPlayer(IPlayer newPlayer) throws RemoteException{
		
		if(activePlayers < nPlayers){
			if(newPlayer != null){//el player fue correctamente inicializado
				
				int id = addToPlayers(newPlayer);
				
				if(serverState == WAITING_FOR_PLAYERS){
					if(activePlayers == nPlayers){
						startNewMatch();
					}else{
						int numPlayers = (nPlayers - activePlayers);
						U.localMessage("Waiting " + numPlayers + ((numPlayers > 1)?" players.":" player."));
					}
				}else if(serverState == PLAYING_MATCH){
					
					for(IPlayer p : players){
						if(p != null){
							p.startNewGame(1,0.8);//TODO: random?
							p.messageFromServer("Let's play!");
						}
					}
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * gestionar el comienzo de una partida, la bandeja de players esta comlpleta.
	 * */
	private void startNewMatch() throws RemoteException{
		
		serverState = PLAYING_MATCH;
		
		U.localMessage("Let's play!");
		
		for(IPlayer p : players){
			if(p != null){
				p.startNewGame(1,0.8);//TODO: random?
				p.messageFromServer("Let's play!");
			}
		}
	}
	

	
	static MyUtil U = new MyUtil();
	
	public PongServer(String _ipHost) throws RemoteException{
		super();
		
		this.ipHost = _ipHost;
		mbean = ManagementFactory.getOperatingSystemMXBean();
		
		//valores iniciales de un server standar
		this.winScore = 10;
		nPlayers = 0;
		
		reInitMatch();
	}
	
	
	/**
	 * para que un player pueda ser publicado por el pongServer
	 * */
	public void sendPlayer(String pPublicName, IPlayer p) throws RemoteException{
		try {
			Naming.rebind(pPublicName, p);
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			IPlayer guest = (IPlayer) Naming.lookup(pPublicName);
			guest.messageFromServer("welcome!");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean iWantToPlay(IPlayer p) throws RemoteException{
		U.localMessage("<< I want to play :)");
		switch(serverState){
		case WAITING_FOR_PLAYERS:
			return addPlayer(p);
		case PLAYING_MATCH:
			return addPlayer(p);
		default:
			U.localMessage(">> NO, go home!");
			return false;
		}
	}
	
	/**
	 * Usado para gestionar la correcta salida de un player
	 * */
	public void iWantToQuit(int playerId) throws RemoteException{
		U.localMessage("<< [player"+playerId+"]: I want to quit =|");
		switch(serverState){
		case WAITING_FOR_PLAYERS:
			
			if(players[playerId] != null){//si no lo habia borrado previamente (en gameover())
				players[playerId].closePlayer();
				players[playerId] = null;
				playersScore[playerId] = 0;
				activePlayers--;
			}
			break;
			
		case PLAYING_MATCH:
			
			players[playerId].closePlayer();
			
			players[playerId] = null;
			playersScore[playerId] = 0;
			if(lastPlayerRebound == playerId)
				lastPlayerRebound = -1;
			activePlayers--;
			
			
			
			U.localMessage("player"+ playerId+" quit.");
			
			//avisar al resto
			for(int i = 0; i < players.length; i++){
				IPlayer p = players[i];
				if(p != null && i != playerId){
					U.localMessage("aviso a player " +i);
					p.enemyGone(playerId);
				}
			}
			
			/**
			 * si todos se fueron antes de terminar la partida
			 * */
			if(activePlayers < 1){
				gameOver();
			}
			
			break;
		default:
			break;
		}
	}
	
	/**
	 * informa al resto de los jugadores la nueva posicion de su bar.
	 * */
	public void iMovedMyBar(int playerId, double x, double y) throws RemoteException{
		if(serverState == MIGRATING){
			return;
		}
		for(int id = 0; id < players.length; id++){
			IPlayer player = players[id];
			if(player != null){
			
				if(id != playerId){
					player.refreshEnemyPos(playerId, x, y);
				}
			
			}
		}
	}
	
	/**
	 * actualiza la posicion de la bola en los demas players,
	 * ademas informa si el player perdio la bola.
	 * */
	public void refreshBall(int playerId, boolean missedBall, double x, double y, double vx, double vy) throws RemoteException{
		if(serverState == MIGRATING){
			return;
		}
		
		//asignar puntaje
		boolean refreshScores = false;
		if(missedBall){
			if(lastPlayerRebound >= 0 && lastPlayerRebound != playerId){
				playersScore[lastPlayerRebound]++;
				refreshScores = true;
			}
		}else{
			lastPlayerRebound = playerId;
		}
		
		for(int id = 0; id < players.length; id++){
			IPlayer player = players[id];
			if(player != null){
				
				if(id != playerId){
					player.refreshBall(playerId, missedBall, x, y, vx, vy);
				}
				if(refreshScores){
					player.refreshScores(playersScore);
				}
				
			}
		}
		
		if(lastPlayerRebound>=0 && playersScore[lastPlayerRebound] >= winScore){//TODO: parametrizar puntaje de termino
			gameOver();
		}
	}
	
	private void gameOver(){
		serverState = MATCH_FINISHED;
		
		for(int id = 0; id < players.length; id++){
			IPlayer player = players[id];
			if(player != null){
				try {
					player.showMatchResults();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		reInitMatch();
		U.localMessage("Match finished :)");
	}
	
	public void recieveServerSettings(int nPlayers, int winScore, int activePlayers, IPlayer[] players, int[] playersScore, int lastPlayerRebound, int serverNextState) throws RemoteException{
		serverState = MIGRATING;
		
		this.nPlayers = nPlayers;
		this.winScore = winScore;
		this.activePlayers = activePlayers;
		this.players = players;
		this.playersScore = playersScore;
		this.lastPlayerRebound = lastPlayerRebound;
		
		U.localMessage("I'm active :D");
		serverState = serverNextState;
	}
	
	public void migrate(String newServerIp) throws RemoteException{
		U.localMessage("Migrando desde "+ipHost+" a "+newServerIp);
		//guardar el estado del server
		int nextServerState = serverState;
		
		//para no atender ninguna peticion de los clientes
		serverState = MIGRATING;
		
		//pasarle los parametros al siguiente server
		try {
			IPongServer nextServer = (IPongServer) Naming.lookup("//"+newServerIp+":1099/PongServer");
			nextServer.recieveServerSettings(nPlayers, winScore, activePlayers, players, playersScore, lastPlayerRebound, nextServerState);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//informar a los clientes la nueva direccion del pongServer
		for(int id = 0; id < players.length; id++){
			IPlayer player = players[id];
			if(player != null){
				try {
					player.refreshServerIp(newServerIp);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		//"desactivar" este server (queda como server de respaldo)
		this.winScore = 10;
		nPlayers = 0;
		reInitMatch();
		U.localMessage("fin de migracion. Ahora soy server de respaldo.");
	}
	
	public double getServerLoad() throws RemoteException{
		return mbean.getSystemLoadAverage();
		/*
		double[] loadavg = new double[3];
		    Scanner scan;
		    File file = new File("/proc/loadavg");
		    try {
		        scan = new Scanner(file);

		        for(int i = 0; i < 3 && scan.hasNextDouble();i++)
		        {
		        	loadavg[i] = scan.nextDouble();
		        }

		    } catch (FileNotFoundException e1) {
		            return -1;
		    }

		
		return loadavg[0];//last [0]: 1 / [1]: 5 / [2]: 15 min
		*/
	}
	
	public void askStartPause() throws RemoteException{
		if(serverState != PLAYING_MATCH)
			return;
		
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
	public void askEndPause() throws RemoteException{
		for(int id = 0; id < players.length; id++){
			IPlayer player = players[id];
			if(player != null){
				try {
					player.endPause();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
