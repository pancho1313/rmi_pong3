package dev;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

import javax.swing.JFrame;

public class Pong implements KeyListener {

	public final static String TITLE = "Pong - CC5303";
	public final static int WIDTH = 480, HEIGHT = 480;
	public final static int UPDATE_RATE = 60;
	public final static int DX = 5;
	public final static double DV = 0.1;

	public JFrame frame;
	public MyCanvas canvas;

	public Bar[] bars;
	private Bar myBar;
	public Bar ball;

	
	private double reboundAngle;//in rads
	private double ballSpeed;

	public boolean[] keysPressed;
	public boolean[] keysReleased;
	

	private Player myPlayer;
	private IPongServer pongServer;
	private boolean suicide;

	private void reset(){
		keysPressed = new boolean[KeyEvent.KEY_LAST];
		keysReleased = new boolean[KeyEvent.KEY_LAST];
		suicide = false;
		canvas.reset();
		canvas.myPlayerId = myPlayer.getPlayerId();
		bars = canvas.bars;
		myBar = bars[myPlayer.getPlayerId()];
		ball = canvas.ball;
	}
	
	public Pong(Player _myPlayer, IPongServer _pongServer) {

		reboundAngle = 65 * Math.PI / 180;//angulo de rebote
		ballSpeed = 4;
		
		myPlayer = _myPlayer;
		pongServer = _pongServer;
		
		
		keysPressed = new boolean[KeyEvent.KEY_LAST];
		keysReleased = new boolean[KeyEvent.KEY_LAST];
		suicide = false;//babies dont die
		init();

	}

	/* Initializes window frame and set it visible */
	public void init() {

		frame = new JFrame(TITLE);
		frame.setLayout(new BorderLayout());
		
		canvas = new MyCanvas(WIDTH, HEIGHT);
		canvas.addKeyListener(this);
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
			    	try {
						pongServer.iWantToQuit(myPlayer.getPlayerId());
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    }
		    }
		);

		Thread game = new Thread(new Runnable(){

			@Override
			public void run(){
				while (myPlayer.showPlayerInterface() && !suicide){
					
					/* decidir que hacer segun el estado del juego */
					int state = myPlayer.getGameState();
			        switch (state) {
			            case Player.WAITING_NEW_MATCH:
			            	/*algo();*/
			            	break;
			            case Player.BRACE_YOURSELF:
			            	sendMyBarPos();
			            	myPlayer.setGameState(Player.PLAYING_MATCH);
			            	break;
			            case Player.PLAYING_MATCH:
			            	/*algo();*/
			            	moveBall();
			            	refreshEnemyDrawingPos();//actualizar las posiciones de los enemigos
			            	refreshScores();
			            	break;
			            case Player.RESET:
			            	//reiniciar variables
			            	reset();
			            	myPlayer.setGameState(Player.BRACE_YOURSELF);
			            	state = Player.WAITING_NEW_MATCH;
			            	/*algo();*/
			            	break;
			            case Player.SHOW_MATCH_RESULTS:
			            	refreshScores();
			            	/*algo();*/
			            	break;
			            case Player.PAUSE:
			            	/*algo();*/
			            	break;
			            default:
			            	/*algoDefault();*/
			            	break;
			        }
					
			        //procesar el input del usuario
			        userKeys(state);
			        
			        //verificar si el server ha migrado
			        refreshServerIp();
			        
					//repintar el canvas
					canvas.gameState = state;
					canvas.repaintMyCanvas();

					//regular los fps
					try {
						Thread.sleep(1000 / UPDATE_RATE); // milliseconds
					} catch (InterruptedException ex) {
					}
				}
				
				frame.dispose();//para matar la ventana del player
			}
		});
		game.start();
		try {
			game.join();
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
	private void exitGame(){
		try {
			pongServer.iWantToQuit(myPlayer.getPlayerId());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void refreshScores(){
		if(myPlayer.refreshScores){
			myPlayer.refreshScores = false;
			
			canvas.scores = myPlayer.scores; 
		}
	}
	
	private void refreshServerIp(){
		if(myPlayer.refreshServerIp){
			myPlayer.refreshServerIp = false;
			String newIp = myPlayer.serverIp;
			
			try {
				pongServer = (IPongServer) Naming.lookup("//"+newIp+":1099/PongServer");
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
	}
	
	/**
	 * procesa el movimiento de la pelota.
	 * */
	private void moveBall(){
		//TODO: separar en más sub-metodos.
		// actualiza posicion y velocidad
		if(myPlayer.refreshBallPos){
			myPlayer.refreshBallPos = false;//ok
			
			ball.x = myPlayer.ballParameters[0];
			ball.y = myPlayer.ballParameters[1];
			ball.vx = myPlayer.ballParameters[2];
			ball.vy = myPlayer.ballParameters[3];
			
			//pintar pelota
			if(myPlayer.refreshBallColor){
				myPlayer.refreshBallColor = false;//ok
				
				int colorId = (int)myPlayer.ballParameters[4];
				if(colorId < 0){
					ball.color = Color.WHITE;
				}else{
					ball.color = MyCanvas.COLORS[colorId];	
				}
			}
			
			//ajustar a los limites del tablero
			if(ball.x < 0){
				ball.x = 1;
			}else if(ball.x >= WIDTH){
				ball.x = WIDTH - 2;
			}
			if(ball.y < 0){
				ball.y = 1;
			}else if(ball.y >= HEIGHT){
				ball.y = HEIGHT - 2;
			}
		}

		boolean mustSendBallPos = false;
		boolean missedBall = true;
		double nextBallX = ball.x + ball.vx;
		double nextBallY = ball.y + ball.vy;
		int bar = myPlayer.getPlayerId();
        switch(bar){
            case MyCanvas.LEFT_BLUE:
            	if(ball.vx < 0){
            		if(nextBallX < myBar.right()){
            			if(nextBallY <= myBar.bottom() && nextBallY >= myBar.top()){//myBar pong
	            			missedBall = false;
	            			
	            			double pong = myBar.y - myBar.top();
	            			double ping = myBar.y - ball.y;
	            			ball.y += ball.vy;
	            			
	            			//vector velocidad
	            			double y_ = 0;
	            			double x_ = ballSpeed;
	            			//rotar vector
	            			double angle = reboundAngle*(ping/pong);
	            			ball.vy = x_*Math.sin(angle) + y_*Math.cos(angle);
	            			ball.vx = x_*Math.cos(angle) - y_*Math.sin(angle);
	            			
	            			mustSendBallPos = true;
	            			break;
            			}else if(nextBallX < 0){//wall pong
            				missedBall = true;
            				ball.vx = -ball.vx;
            				mustSendBallPos = true;
	            			break;
            			}
            		}
            		ball.x = nextBallX;
            	}else{
            		//rebote horizontal con pared
                	if(nextBallX >= WIDTH){
                		ball.vx = -ball.vx;
                	}
                	ball.x += ball.vx;
            	}
            	
            	//rebote vertical con paredes
            	if(nextBallY < 0 || nextBallY >= HEIGHT){
            		ball.vy = -ball.vy;
            	}
            	ball.y += ball.vy;
            	/*algo();*/
            	break;
            case MyCanvas.RIGHT_YELLOW:
            	if(ball.vx > 0){
            		if(nextBallX > myBar.left()){
            			if(nextBallY <= myBar.bottom() && nextBallY >= myBar.top()){//myBar pong
	            			missedBall = false;
	            			
	            			double pong = myBar.y - myBar.top();
	            			double ping = myBar.y - ball.y;
	            			ball.y += ball.vy;
	            			
	            			//vector velocidad
	            			double y_ = 0;
	            			double x_ = -ballSpeed;
	            			//rotar vector
	            			double angle = -reboundAngle*(ping/pong);
	            			ball.vy = x_*Math.sin(angle) + y_*Math.cos(angle);
	            			ball.vx = x_*Math.cos(angle) - y_*Math.sin(angle);
	            			
	            			mustSendBallPos = true;
	            			break;
            			}else if(nextBallX >= WIDTH){//wall pong
            				missedBall = true;
            				ball.vx = -ball.vx;
            				mustSendBallPos = true;
	            			break;
            			}
            		}
            		ball.x = nextBallX;
            	}else{
            		//rebote horizontal con pared
                	if(nextBallX < 0){
                		ball.vx = -ball.vx;
                	}
                	ball.x += ball.vx;
            	}
            	
            	//rebote vertical con paredes
            	if(nextBallY < 0 || nextBallY >= HEIGHT){
            		ball.vy = -ball.vy;
            	}
            	ball.y += ball.vy;
            	break;
            case MyCanvas.TOP_GREEN:
            	if(ball.vy < 0){
            		if(nextBallY < myBar.bottom()){
            			if(nextBallX <= myBar.right() && nextBallX >= myBar.left()){//myBar pong
	            			missedBall = false;
	            			
	            			double pong = myBar.x - myBar.left();
	            			double ping = myBar.x - ball.x;
	            			ball.x += ball.vx;
	            			
	            			//vector velocidad
	            			double y_ = ballSpeed;
	            			double x_ = 0;
	            			//rotar vector
	            			double angle = -reboundAngle*(ping/pong);
	            			ball.vy = x_*Math.sin(angle) + y_*Math.cos(angle);
	            			ball.vx = x_*Math.cos(angle) - y_*Math.sin(angle);
	            			
	            			
	            			mustSendBallPos = true;
	            			break;
            			}else if(nextBallY < 0){//wall pong
            				missedBall = true;
            				ball.vy = -ball.vy;
            				mustSendBallPos = true;
	            			break;
            			}
            		}
            		ball.y = nextBallY;
            	}else{
            		//rebote vertical con pared
                	if(nextBallY >= HEIGHT){
                		ball.vy = -ball.vy;
                	}
                	ball.y += ball.vy;
            	}
            	
            	//rebote horizontal con paredes
            	if(nextBallX < 0 || nextBallX >= WIDTH){
            		ball.vx = -ball.vx;
            	}
            	ball.x += ball.vx;
            	break;
            case MyCanvas.BOTTOM_RED:
            	if(ball.vy > 0){
            		if(nextBallY > myBar.top()){
            			if(nextBallX <= myBar.right() && nextBallX >= myBar.left()){//myBar pong
	            			missedBall = false;
	            			
	            			double pong = myBar.x - myBar.left();
	            			double ping = myBar.x - ball.x;
	            			ball.x += ball.vx;
	            			
	            			//vector velocidad
	            			double y_ = -ballSpeed;
	            			double x_ = 0;
	            			//rotar vector
	            			double angle = reboundAngle*(ping/pong);
	            			ball.vy = x_*Math.sin(angle) + y_*Math.cos(angle);
	            			ball.vx = x_*Math.cos(angle) - y_*Math.sin(angle);
	            			
	            			mustSendBallPos = true;
	            			break;
            			}else if(nextBallY >= HEIGHT){//wall pong
            				missedBall = true;
            				ball.vy = -ball.vy;
            				mustSendBallPos = true;
	            			break;
            			}
            		}
            		ball.y = nextBallY;
            	}else{
            		//rebote horizontal con pared
                	if(nextBallY < 0){
                		ball.vy = -ball.vy;
                	}
                	ball.y += ball.vy;
            	}
            	
            	//rebote vertical con paredes
            	if(nextBallX < 0 || nextBallX >= WIDTH){
            		ball.vx = -ball.vx;
            	}
            	ball.x += ball.vx;
            	break;
            default:
            	/*algoDefault();*/
            	break;
        }
		//rebote TODO: revisar rebote en esquinas del tablero!
        
		if(mustSendBallPos){
			if(!missedBall){
				ball.color = MyCanvas.COLORS[myPlayer.getPlayerId()];
			}
			
			try {
				pongServer.refreshBall(myPlayer.getPlayerId(), missedBall, ball.x, ball.y, ball.vx, ball.vy);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * procesa las teclas presionadas por el usuario segun el estado del juego.
	 * */
	 private void userKeys(int gameState){
		 switch (gameState) {
         case Player.WAITING_NEW_MATCH:
         	/*algo();*/
         	break;
         case Player.PLAYING_MATCH:
			if(keysPressed[KeyEvent.VK_Q]){
				exitGame();
			}
			
			if(myPlayer.getPlayerId() < 2){//izq der
				if(keysPressed[KeyEvent.VK_UP]){
					if(myBar.top() - DX >= 0)
						myBar.y -= DX;
				}
				if(keysPressed[KeyEvent.VK_DOWN]){
					if(myBar.bottom() + DX < HEIGHT)
						myBar.y += DX;
				}
			}else{//down up
				if(keysPressed[KeyEvent.VK_LEFT]){
					if(myBar.left() - DX > 0)
						myBar.x -= DX;
				}
				if(keysPressed[KeyEvent.VK_RIGHT]){
					if(myBar.right() + DX < WIDTH)
						myBar.x += DX;
				}
			}
			/////////////////////////////////////////////
			boolean keyReleased = false;
			if(keysReleased[KeyEvent.VK_RIGHT]){
				keysReleased[KeyEvent.VK_RIGHT] = false;
				keyReleased = true;
			}
			if(keysReleased[KeyEvent.VK_LEFT]){
				keysReleased[KeyEvent.VK_LEFT] = false;
				keyReleased = true;
			}
			if(keysReleased[KeyEvent.VK_UP]){
				keysReleased[KeyEvent.VK_UP] = false;
				keyReleased = true;
			}
			if(keysReleased[KeyEvent.VK_DOWN]){
				keysReleased[KeyEvent.VK_DOWN] = false;
				keyReleased = true;
			}
			if(keyReleased)
				sendMyBarPos();
			
			if(keysReleased[KeyEvent.VK_SPACE]){
				keysReleased[KeyEvent.VK_SPACE] = false;
				askStartPause();
			}
         	/*algo();*/
         	break;
         case Player.GAME_OVER:
        	 if(keysPressed[KeyEvent.VK_Q]){
         		
  				suicide = true;
         	 }
         	/*algo();*/
         	break;
         case Player.SHOW_MATCH_RESULTS:
        	 if(keysPressed[KeyEvent.VK_Q] || keysPressed[KeyEvent.VK_N]){
        		
        		 myPlayer.setGameState(Player.GAME_OVER);
        	 }
        	 if(keysPressed[KeyEvent.VK_Y]){
        		 oneMoreMatchPlease();
        	 }
         	break;
          case Player.PAUSE:
        	  if(keysReleased[KeyEvent.VK_SPACE]){
        		  keysReleased[KeyEvent.VK_SPACE] = false;
        		  askEndPause();
           	 }
          	break;
         default:
         	/*algoDefault();*/
         	break;
		 }
	 }
	 
	 private void oneMoreMatchPlease(){
		 myPlayer.setGameState(Player.WAITING_NEW_MATCH);
		 
		 //////////////////////////////////////////////////////////77 TODO: refactoring
		 ISServer sServer;
		 String ipSServer = myPlayer.sServerIp;
		 String serverIp = "";
		//contactar al SServer y obtener la ip del servidor activo
			try {
				sServer = (ISServer) Naming.lookup("//"+ipSServer+":1099/SServer");
				serverIp = sServer.whoIstheServer();
			} catch (MalformedURLException e) {
				myPlayer.setGameState(Player.GAME_OVER);
			} catch (RemoteException e) {
				myPlayer.setGameState(Player.GAME_OVER);
			} catch (NotBoundException e) {
				myPlayer.setGameState(Player.GAME_OVER);
			}
			
			
			//contactar al server
			try {
				pongServer = (IPongServer) Naming.lookup("//"+serverIp+":1099/PongServer");
			} catch (MalformedURLException e) {
				myPlayer.setGameState(Player.GAME_OVER);
			} catch (RemoteException e) {
				myPlayer.setGameState(Player.GAME_OVER);
			} catch (NotBoundException e) {
				myPlayer.setGameState(Player.GAME_OVER);
			}
			
		 ////////////////////////////////////////////////////////////77
		//pedir jugar pong
		 try {
 			 if(!pongServer.iWantToPlay(myPlayer)){
 				 myPlayer.setGameState(Player.GAME_OVER);
 			 }
			} catch (RemoteException e) {
				myPlayer.setGameState(Player.GAME_OVER);
			}
	 }
	 
	 /**
	  * informa al servidor la nueva posicio de mi bar
	  * */
	 private void sendMyBarPos(){
		 try {
			pongServer.iMovedMyBar(myPlayer.getPlayerId(), myBar.x, myBar.y);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 /**
	  * actualiza las posiciones de los enemigos segun la info del myPlayer asociado.
	  * */
	 private void refreshEnemyDrawingPos(){
		 if(myPlayer.refreshEnemyBars){
			 myPlayer.refreshEnemyBars = false;
			 
			 for(int id = 0; id < bars.length; id++){
				 if(id != myPlayer.getPlayerId()){
					 Bar enemyBar = bars[id];
					 if(!myPlayer.activePlayers[id]){
						 enemyBar.hidden = true;
					 }else{
						 enemyBar.hidden = false;
						 enemyBar.x = myPlayer.barsPos[id][0];
						 enemyBar.y = myPlayer.barsPos[id][1];
					 }
				 }
			 }
		 }
	 }
	 
	 private void askStartPause(){
		if(myPlayer.getGameState() != Player.PLAYING_MATCH)
			return;
		
		try {
			pongServer.askStartPause();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	 private void askEndPause(){
			if(myPlayer.getGameState() != Player.PAUSE)
				return;
			
			try {
				pongServer.askEndPause();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
}
