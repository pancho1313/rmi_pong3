package dev;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

import javax.swing.JFrame;

public class SSLoop implements KeyListener {

	public final static String TITLE = "Pong/SServer - CC5303";
	public final static int WIDTH = 300, HEIGHT = 100;
	public final static int UPDATE_RATE = 1;

	public JFrame frame;
	public SSCanvas canvas;

	

	public boolean[] keysPressed;
	public boolean[] keysReleased;
	
	private SServer sServer;
	private boolean suicide;
	private HashMap<String,IPongServer> servers;

	private void reset(){
		keysPressed = new boolean[KeyEvent.KEY_LAST];
		keysReleased = new boolean[KeyEvent.KEY_LAST];
		suicide = false;
		canvas.reset();
	}
	
	public SSLoop(SServer _sServer) {
		this.sServer = _sServer;
		
		this.servers = new HashMap<String,IPongServer>();
		for(String key : sServer.serversIp){
			servers.put(key, null);
		}
		
		keysPressed = new boolean[KeyEvent.KEY_LAST];
		keysReleased = new boolean[KeyEvent.KEY_LAST];
		suicide = false;//babies dont die
		init();

	}

	/* Initializes window frame and set it visible */
	public void init() {

		frame = new JFrame(TITLE);
		frame.setLayout(new BorderLayout());
		
		canvas = new SSCanvas(WIDTH, HEIGHT);
		frame.add(canvas);
		frame.pack();
		
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		
		
		reset();//no mover de aqui!

		

		
		
		frame.addKeyListener(this);
		frame.addWindowListener(
			new java.awt.event.WindowAdapter() {
			    @Override
			    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
			    	exitSSLoop();
			    }
		    }
		);

		Thread sSLoop = new Thread(new Runnable(){

			@Override
			public void run(){
				while (!suicide){
					boolean doContinue = false;
					refreshServers();
					
					boolean migrate = false;
					String migrateTo = "";
					double minLoad = 1000;
					double actualLoad = 0;
					ArrayList<Double> info_load = new ArrayList<Double>();
					ArrayList<String> info_ip = new ArrayList<String>();
					Iterator<String> keySetIterator = servers.keySet().iterator();
					while(keySetIterator.hasNext()){
					  String ip = keySetIterator.next();
					  IPongServer server = servers.get(ip);
					  System.out.println("flag: "+ip);
					  if(server != null){
						  System.out.println("flag: 1");
						  double load = -1;
						  try {
							  System.out.println("flag: 2");
							load = server.getServerLoad();
							if(ip.equals(sServer.activeServer)){
								// actualizar estado del server activo para respaldo y recuperacion...
								sServer.refreshGeneralState(server.getPongServerGeneralState());
								System.out.println("flag: 3");
								if(sServer.sServerState==SServer.SERVER_DOWN){
									System.out.println("flag: 4");
									sServer.habemusPongServer();
								}
							}
						} catch (RemoteException e) {
							System.out.println("flag: 5");
							// si no responde (hizo ctrl+c) --> informar su fallecimiento --> deadServer(String ip)
							if(deadServer(ip)){
								doContinue = true;
								continue;
							}
						}
						
						  
						if(ip.equals(sServer.activeServer)){
							if(load > 0.7){
								migrate = true;
								actualLoad = load;
							}
						}else if(load < minLoad){
							//migrar al de carga minima
							minLoad = load;
							migrateTo = ip;
						}
						  info_load.add(load);
						  info_ip.add(ip);
						  
					  }else{
						  System.out.println("flag: 6");
						  IPongServer newServer;
							try {
								System.out.println("flag: 7");
								newServer = (IPongServer) Naming.lookup("//"+ip+":1099/PongServer");
								servers.put(ip, newServer);
								System.out.println("flag: 8");
							} catch (MalformedURLException e) {
								System.out.println("flag: 9");
								if(deadServer(ip)){
									doContinue = true;
									continue;
								}
							} catch (RemoteException e) {
								System.out.println("flag: 10");
								if(deadServer(ip)){
									doContinue = true;
									continue;
								}
							} catch (NotBoundException e) {
								System.out.println("flag: 11");
								if(deadServer(ip)){
									doContinue = true;
									continue;
								}
							}
						
						  
					  }
					}
					
					if(doContinue){
						continue;
					}
					
					//Do migrate!
					if(migrate && !migrateTo.equals("") && actualLoad > minLoad){
						migrate(sServer.activeServer,migrateTo);
					}
					
			        //procesar el input del usuario
			        userKeys();
			        
					//repintar el canvas
			        String[] str = new String[info_load.size()];
			        for(int i = 0; i < str.length; i++){
			        	str[i] = info_ip.get(i)+" : "+info_load.get(i).toString()+((info_ip.get(i).equals(sServer.activeServer))?" (active)":"");
			        }
					canvas.loadInfo = str;
					canvas.repaintSSCanvas();

					//regular los fps
					try {
						Thread.sleep(1000 / UPDATE_RATE); // milliseconds
					} catch (InterruptedException ex) {
					}
				}
				
				frame.dispose();//para matar la ventana del player
			}
		});
		sSLoop.start();
		try {
			sSLoop.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}

	/*------------------------------------------*/
	
	@Override
	public void keyPressed(KeyEvent event) {
		keysPressed[event.getKeyCode()] = true;
		keysReleased[event.getKeyCode()] = false;
	}

	@Override
	public void keyReleased(KeyEvent event) {
		keysPressed[event.getKeyCode()] = false;
		keysReleased[event.getKeyCode()] = true;

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}
	/*----------------------------------------*/
	
	/**
	 * avisa al servidor que el player desea retirarse del juego.
	 * */
	private void exitSSLoop(){
		suicide = true;
	}
	
	/**
	 * procesa las teclas presionadas por el usuario segun el estado del juego.
	 * */
	 private void userKeys(){
		if(keysPressed[KeyEvent.VK_Q]){
			exitSSLoop();
		}else if(keysPressed[KeyEvent.VK_M]){
			//migrate("172.17.69.198", "172.17.69.199");
		}
	 }
	 
	 private void refreshServers(){
		 if(sServer.refreshServers){
			 sServer.refreshServers = false;
			 
			 this.servers = new HashMap<String,IPongServer>();
			 for(String key : sServer.serversIp){
				 servers.put(key, null);
			 }
		 }
	 }

	 private void migrate(String from, String to){
		//TODO: migrate
			System.out.println(">> Migrate! (form "+from+" to "+to+")");
			
			//avisar que debe migrar a ...
			try {
				servers.get(from).migrate(to);
			} catch (RemoteException e) {
				System.out.println("ERROR Migrate!");
			}
			
			//cambiar ip del server activo...
			sServer.activeServer = to;
	 }
	 
	 private boolean deadServer(String ip){
		 System.out.println("dead server ip = "+ip);

		 //eliminar el server del hashmap
		 servers.put(ip, null);
		 
		 //informar el fallecimiento de un server (no responde)
		 return sServer.deadServer(ip);
	 }
}
