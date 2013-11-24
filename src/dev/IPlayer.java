package dev;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IPlayer extends Remote {

	public void messageFromServer(String message) throws RemoteException;
	public void setPlayerId(int id) throws RemoteException;
	public void closePlayer() throws RemoteException;
	public void preNewGame() throws RemoteException;
	public void startNewGame(double ballVX, double ballVY) throws RemoteException;
	public void refreshEnemyPos(int enemyId, double x, double y) throws RemoteException;
	public void refreshBall(int enemyId, boolean missedBall, double x, double y, double vx, double vy) throws RemoteException;
	public void refreshScores(int[] scores) throws RemoteException;
	public void showMatchResults() throws RemoteException;
	public void enemyGone(int enemyId) throws RemoteException;
	public void informPosition() throws RemoteException;
	public void refreshServerIp(String ip) throws RemoteException;
	public void startPause() throws RemoteException;
	public void endPause() throws RemoteException;

}
