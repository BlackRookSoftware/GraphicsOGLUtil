/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.tile2d;

import com.blackrook.ogl.util.OGLSkin;

/**
 * The rendering model to use for {@link OGLTile2DNode}.
 * Every tile model relies on a grid-like arrangement in order to  
 * This interface provides methods that OGLTile2DNode uses
 * in order to render tiles.
 * @author Matthew Tropiano
 */
public interface OGLTile2DModel
{
	/**
	 * Gets the appropriate skin set for a set of coordinates. 
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @return the OGLSkin, or null if no OGLSkin for the position on the grid.
	 */
	public abstract OGLSkin getSkin(int x, int y);

	/**
	 * Gets if a tile is visible for a set of coordinates and the
	 * current wrapping types.
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 */
	public abstract boolean getVisible(int x, int y);

	/**
	 * Gets the appropriate ARGB color integer for a set of coordinates and the
	 * current wrapping types.
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 */
	public abstract int getColorARGB(int x, int y);

	/**
	 * Gets the appropriate texture offsets for a set of coordinates. 
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @param out the output array, must be length 4 or greater. 
	 * Assumed order is S0, T0, S1, T1.
	 */
	public abstract void getTextureOffsets(int x, int y, float[] out);

	/**
	 * Returns the x-offset of a tile in units.
	 */
	public abstract float getOffsetX(int x, int y);

	/**
	 * Returns the y-offset of a tile in units.
	 */
	public abstract float getOffsetY(int x, int y);

	/**
	 * Gets the individual tile scale, x-axis, for a specific tile.
	 */
	public abstract float getScaleX(int x, int y);

	/**
	 * Gets the individual tile scale, y-axis, for a specific tile.
	 */
	public abstract float getScaleY(int x, int y);
	
}
