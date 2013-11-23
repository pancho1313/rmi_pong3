package dev;

import java.awt.Color;
import java.awt.Graphics;

public class Bar {

	double x, y, vx, vy;
	double w, h;
	Color color;
	boolean hidden;

	public Bar(double x, double y, double w, double h, Color color) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.color = color;
		this.hidden = false;
		vx = vy = 0;
	}

	public void draw(Graphics g) {
		if(!hidden){
			Color prevColor = g.getColor();
			g.setColor(color);
			g.fillRect(left(), top(), (int) w, (int) h);
			g.setColor(prevColor);
		}
	}

	public int top() {
		return (int) (y - h * 0.5);
	}

	public int left() {
		return (int) (x - w * 0.5);
	}

	public int bottom() {
		return (int) (y + h * 0.5);
	}

	public int right() {
		return (int) (x + w * 0.5);
	}

	public void getPos(double x, double y){
		x = this.x;
		y = this.y;
	}
	
	public void setPos(double x, double y){
		this.x = x;
		this.y = y;
	}
}
