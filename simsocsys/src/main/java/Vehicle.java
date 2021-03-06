/* *********************************************************************** *
 * project: simsocsys
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : gregor dot laemmel at gmail dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import processing.core.PVector;

/**
 * Created by laemmel on 18/04/16.
 */
public class Vehicle {


    private final List<Link> route;
    private final int id;
    private double vx = 0;
    private double vy = 0;
    private double speed = 1.34;
    private double tau = 0.5;
    private double weight = 80.0;
    private boolean waiting = false;
    private boolean entering;

	//    absto�ende Kr�fte
    private double r = 0.3;

	private double x;
    private double y;
    private double phi = 0;//radian!!


    private int routeIndex = 0;

    public Vehicle(double x, double y, List<Link> route, int id) {
        this.id = id;
    	this.x = x;
        this.y = y;
        this.route = route;
    }

    public void update(List<Vehicle> vehs) {
    	
    	if (this.waiting){
    		this.speed = 0.1;
    	} else {
    		this.speed = 1.34;
    	}
    	
    	Link currentLink;
    	currentLink = route.get(this.routeIndex);

        double dx = currentLink.getTo().getX() - this.x;
        double dy = currentLink.getTo().getY() - this.y;

        double dist = Math.sqrt(dx*dx+dy*dy);
        dx /= dist;
        dy /= dist;
        
//        dx *= this.speed;
//        dy *= this.speed;
        
        dx -= vx;
        dy -= vy;
        
        Force repellingForceAgents = new Force();
        repellingForceAgents = repellingForceAgents(vehs);
        
        Force repellingForceWalls = new Force();
        repellingForceWalls = repellingForceWalls();
        
        double fx = (this.weight * dx / this.tau) + repellingForceAgents.getFx() + repellingForceWalls.getFx();
        double fy = (this.weight * dy / this.tau) + repellingForceAgents.getFy() + repellingForceWalls.getFy();

        double ax = fx / this.weight;
        double ay = fy / this.weight;
        
        this.vx += ax * Simulation.H;
        this.vy += ay * Simulation.H;
        
        double speed = Math.sqrt(this.vx*this.vx+this.vy*this.vy);
        if(speed>this.speed){
        	this.vx /= speed;
        	this.vy /= speed;
        }

        this.phi = Math.atan2(vy,vx);

    }
    
    public Force repellingForceAgents(List<Vehicle> vehs){
    	
    	Force force = new Force();
    	
    	double fx = 0;
    	double fy = 0;
    	
    	for(Vehicle veh : vehs){
    		
    		if ( veh != this){
    			    			
    			double dist =  vehicleDistance(veh);
    			double distR = (this.r + veh.r - dist);
    			
    			double f = Simulation.A * Math.pow(Math.E, (distR/Simulation.B));

    			if (this.r + veh.r >= dist)	{
    				
    				f = f + Simulation.K * distR;
    				
    			}
    			
    	        double dx = (this.x - veh.getX())/dist;
    	        double dy = (this.y - veh.getY())/dist;
    	        
    			fx += dx*f;
    			fy += dy*f;
    			
    			if (this.r + veh.r >= dist)	{
    				
    				double nx = this.x - veh.getX();
    				double ny = this.y - veh.getY();
    				double tx = -ny;
    				double ty = nx;
    				
    				double dvx = (veh.getVX()-this.vx)*tx;
    				double dvy = (veh.getVY()-this.vy)*ty;
    				
    				fx += Simulation.KAPPA * distR * dvx;
    				fy += Simulation.KAPPA * distR * dvy;
    				
    			}
    			
    		} 
    		
    	}
    	
    	force.setFx(fx);
    	force.setFy(fy);
    	
    	return force;
    	
    }
    
    public double vehicleDistance(Vehicle v1){
    	
        double dx = v1.getX() - this.x;
        double dy = v1.getY() - this.y;

        double dist = Math.sqrt(dx*dx+dy*dy);
    	
    	return dist;
    	
    }
    
    public Force repellingForceWalls(){
    	
    	Force force 	= new Force(0, 0);
    	
    	PVector veh 	= new PVector((float)this.x, (float) this.y);
    	
    	for(Wall wall : Simulation.walls.getWalls().values()){
    		
    		PVector wFrom 	= new PVector((float)wall.getxFrom(), (float) wall.getyFrom());
    		PVector wTo 	= new PVector((float)wall.getxTo(), (float) wall.getyTo());
    		PVector closest = new PVector();    		
    		
    		double distance ;
    		
			float vx = veh.x-wFrom.x; 
			float vy = veh.y-wFrom.y;   // v = wFrom->veh
			float ux = wTo.x-wFrom.x;
			float uy = wTo.y-wFrom.y;   // u = wFrom->wTo
			float det = vx*ux + vy*uy; 
			float len = ux*ux + uy*uy;    // len = u^2
			
			if (det <= 0){ 	// its outside the line segment near wFrom
			  closest.set(wFrom); 
			} else if (det >= len){ // its outside the line segment near wTo
			  closest.set(wTo);  
			} else {// its near line segment between wFrom and wTo
				float ex = (float) (ux / Math.sqrt(len));    	// e = u / |u^2|
				float ey = (float) (uy / Math.sqrt(len));
				float f = ex * vx + ey * vy;  					// f = e . v
				closest.set(wFrom.x + f * ex, wFrom.y + f * ey);           				// S = wFrom + f * e
			}
			distance = Math.sqrt(Math.pow(closest.x-veh.x, 2) + Math.pow(closest.y-veh.y, 2));
			DecimalFormat df = new DecimalFormat("#.##");
//	    	System.out.println(this.getId() + " " + wall.getId() + " " + df.format(distance) + " " + closest.x + " " + closest.y);
			double distR = this.r - distance;
			double dx = (veh.x - closest.x) / distance;
			double dy = (veh.y - closest.y) / distance;
			double f = Simulation.A * Math.pow(Math.E, (distR / Simulation.B));
			if (distance <= this.r) {
				f = f + (Simulation.K * distR);
			}

			double fx = f * (dx);
			double fy = f * (dy);
			if (distance <= this.r) {
				double tx = -dy;
				double ty = dx; 
				fx = fx - Simulation.KAPPA * distR * this.vx * tx * tx;
				fy = fy - Simulation.KAPPA * distR * this.vy * ty * ty;
			}
			
			force.setFx(force.getFx()+fx);
			force.setFy(force.getFy()+fy);
    	}	    	
    	
    	return force;
    }

    public boolean move() {
        this.x = this.x + Simulation.H * this.vx;
        this.y = this.y + Simulation.H * this.vy;


        Link currentLink = this.route.get(this.routeIndex);
        if (currentLink.hasVehicleReachedEndOfLink(this)) {
            this.routeIndex++;
            if (this.routeIndex == this.route.size() ){
            	return false;
            }
        }
        return true;
    }

    public double getX() {
        return x;
    }
    
    public void setX(double x){
    	this.x = x;
    }
    
    public void setY(double y){
    	this.y = y;
    }

    public double getY() {
        return y;
    }
    
    public double getVX() {
        return vx;
    }

    public double getVY() {
        return vy;
    }

    public double getPhi() {
        return phi;
    }

	public int getId() {
		return id;
	}

	public boolean isWaiting() {
		
		return this.waiting;
	}
    
    public boolean isEntering() {
		return entering;
	}

	public void setEntering(boolean entering) {
		this.entering = entering;
	}
    
    public double getR() {
		return r;
	}

	public void setR(double r) {
		this.r = r;
	}
}
