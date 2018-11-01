package com.energyxxer.xswing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ComponentResizer {
	
	private static final int DIST = 8;
	
	private JComponent resizable = null;
	
	private boolean edges[] = {
	   	false, false, false, false
	};
	
	public ComponentResizer() {
		
	}
	
	public ComponentResizer(JComponent component) {
		addResizable(component);
	}
	
	/**
	 * Gives the specified component a resizable functionality.
	 * */
	public void addResizable(JComponent component) {
		resizable = component;
		
		MouseAdapter adapter = new MouseAdapter() {

			//Status status = new Status();
			//String[] types = new String[] {Status.INFO, Status.WARNING, Status.ERROR};

			@Override
	        public void mouseMoved(MouseEvent me) {
				//status.setMessage(getCursor(me).toString());
				//status.setType(types[ThreadLocalRandom.current().nextInt(0, 3)]);
                component.setCursor(getCursor(me));
	        }

	        @Override
	        public void mouseExited(MouseEvent me) {
				component.setCursor(Cursor.getDefaultCursor());
	        }
		};

		component.addMouseListener(adapter);
		component.addMouseMotionListener(adapter);
		
	}
	
	public void removeResizable() {
		resizable = null;
	}
	
	/**
	 * Allows/disallows component resizing in all directions.
	 * */
	public void setResizable(boolean resizable) {
		edges[0] = edges[1] = edges[2] = edges[3] = resizable;
	}
	
	/**
	 * Allows/disallows component resizing in given directions.
	 * */
	public void setResizable(boolean top, boolean left, boolean bottom, boolean right) {
		edges[0] = top;
		edges[1] = left;
		edges[2] = bottom;
		edges[3] = right;
	}
	
	/**
	 * Returns an array of rectangles which contains the sections the mouse must be inside to resize.
	 * */
	private Rectangle[] getEdgeAreas() {
		if(resizable != null) {
			int w = resizable.getWidth();
			int h = resizable.getHeight();
			return new Rectangle[] {
				(edges[0]) ? new Rectangle(0, 0, w, DIST) : new Rectangle(),
				(edges[1]) ? new Rectangle(0, 0, DIST, h) : new Rectangle(),
				(edges[2]) ? new Rectangle(0, h-DIST, w, DIST) : new Rectangle(),
				(edges[3]) ? new Rectangle(w-DIST, 0, DIST, h) : new Rectangle()
			};
		} else return null;
	}
	
	private Cursor getCursor(MouseEvent me) {
        
        int[] edges = new int[] { SwingConstants.TOP, SwingConstants.LEFT, SwingConstants.BOTTOM, SwingConstants.RIGHT };
        Rectangle[] areas = getEdgeAreas();
        Boolean[] intersections = new Boolean[] { false, false, false, false };

        for(int i = 0; i < edges.length; i++) {
        	if(areas[i].contains(me.getPoint())) {
        		intersections[i] = true;
        	}
        }
        
        int direction = -1;

        if(intersections[SwingConstants.TOP-1]) direction = SwingConstants.NORTH;
        if(intersections[SwingConstants.LEFT-1]) direction = SwingConstants.WEST;
        if(intersections[SwingConstants.BOTTOM-1]) direction = SwingConstants.SOUTH;
        if(intersections[SwingConstants.RIGHT-1]) direction = SwingConstants.EAST;
        if(intersections[SwingConstants.TOP-1] && intersections[SwingConstants.LEFT-1]) direction = SwingConstants.NORTH_WEST;
        if(intersections[SwingConstants.TOP-1] && intersections[SwingConstants.RIGHT-1]) direction = SwingConstants.NORTH_EAST;
        if(intersections[SwingConstants.BOTTOM-1] && intersections[SwingConstants.LEFT-1]) direction = SwingConstants.SOUTH_WEST;
        if(intersections[SwingConstants.BOTTOM-1] && intersections[SwingConstants.RIGHT-1]) direction = SwingConstants.SOUTH_EAST;
        
        int cursors[] = {
        	Cursor.N_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR, Cursor.E_RESIZE_CURSOR,
        	Cursor.SE_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR,
        	Cursor.W_RESIZE_CURSOR, Cursor.NW_RESIZE_CURSOR
        };
        
        if(direction < 0) {
        	return Cursor.getDefaultCursor();
        }
        return Cursor.getPredefinedCursor(cursors[direction-1]);
    }
}
