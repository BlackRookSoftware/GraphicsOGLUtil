/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.tile2d;

/**
 * Listener interface for OGLGridViewers.
 * Contains methods for intercepting mouse and keyboard input.
 * @author Matthew Tropiano
 */
public interface OGLTile2DListener
{
	/**
	 * This method is called when a mouse moves over a grid space.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	public void gridMouseOver(int gridX, int gridY);

	/**
	 * This method is called when a mouse leaves a grid space.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	public void gridMouseLeave(int gridX, int gridY);

	/**
	 * This method is called when a mouse button is pressed on this object.
	 * @param button the java.awt.MouseEvent button constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	public void gridMousePress(int button, int gridX, int gridY);

	/**
	 * This method is called when a mouse button is released on this object.
	 * @param button the java.awt.MouseEvent button constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	public void gridMouseRelease(int button, int gridX, int gridY);

	/**
	 * This method is called when a mouse is dragged on this object.
	 * This event may be sent MANY times over the course of a single drag.
	 * @param button the java.awt.MouseEvent button constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 * @param unitsX how many pixels of movement in the drag.
	 * @param unitsY how many pixels of movement in the drag.
	 */
	public void gridMouseDrag(int button, int gridX, int gridY, float unitsX, float positionX, float unitsY, float positionY);

	/**
	 * This method is called when a mouse button is clicked (down and up on the same grid space).
	 * @param button the java.awt.MouseEvent button constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	public void gridMouseClick(int button, int gridX, int gridY);

	/**
	 * This method is called when the mouse wheel is scrolled.
	 * @param units the amount of units that the wheel was scrolled - can be negative.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	public void gridMouseWheel(int units, int gridX, int gridY);

	/**
	 * This method is called when a key is pressed while a grid space is moused-over.
	 * @param keycode the java.awt.KeyEvent VK constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	public void gridKeyPress(int keycode, int gridX, int gridY);

	/**
	 * This method is called when a key is released while a grid space is moused-over.
	 * @param keycode the java.awt.KeyEvent VK constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	public void gridKeyRelease(int keycode, int gridX, int gridY);
	
	/**
	 * This method is called when a key is typed while a grid space is moused-over.
	 * @param keycode the java.awt.KeyEvent VK constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	public void gridKeyTyped(int keycode, int gridX, int gridY);
	
	/**
	 * This method is called when a grid listener gains focus.
	 */
	public void gridFocus();

	/**
	 * This method is called when a grid listener loses focus.
	 */
	public void gridUnfocus();

	/**
	 * This method is called when the mouse enters the canvas.
	 */
	public void gridMouseEnter();

	/**
	 * This method is called when the mouse exits the canvas.
	 */
	public void gridMouseExit();

}
