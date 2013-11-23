package dev;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.util.List;
import java.util.ArrayList;

public class SSCanvas extends Canvas {
	
	
	private static final long serialVersionUID = -1104510752775347794L;
	
	private BufferStrategy strategy;
	
	public String[] loadInfo;

	public SSCanvas(int WIDTH, int HEIGHT){
		super();
		this.setSize(WIDTH, HEIGHT);
		
		// Tell AWT not to bother repainting our canvas since we're
		// going to do that our self in accelerated mode
		setIgnoreRepaint(true);
		// create the buffering strategy which will allow AWT

		strategy = null;
		
		//default values
		reset();
	}
	
	public void reset(){
		String[] aux = {"---no data---"};
		loadInfo = aux;
	}
	
	public void repaintSSCanvas() {

		// to manage our accelerated graphics
		if(strategy == null){
			createBufferStrategy(2);
			strategy = getBufferStrategy();
		}
		
		// Get hold of a graphics context for the accelerated
		// surface and blank it out
		Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
		
		paintResults(g);
		
		g.dispose();
		strategy.show();

	}
	
	private void paintResults(Graphics g){
		
		//fondo
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		//texto
		g.setColor(Color.YELLOW);
		FontMetrics fm = g.getFontMetrics();
	    fm = g.getFontMetrics();
	    
	    for(int i = 0; i < loadInfo.length; i++){
	    	String s = loadInfo[i];
		    int w = fm.stringWidth(s);
		    int h = fm.getAscent();
		    g.drawString(s, (getWidth()/2) - (w / 2), 10 + (i*(h+3)));
	    }
	}
}
