/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.tile2d;

import com.blackrook.ogl.data.OGLColor;
import com.blackrook.ogl.enums.BlendFunc;
import com.blackrook.ogl.enums.TextureMode;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture2D;

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
	 * Gets if a tile is visible for a set of coordinates and the
	 * current wrapping types.
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 */
	public abstract boolean getVisible(int x, int y);

	/**
	 * Gets the appropriate shader program for rendering this tile.
	 * Null should imply that the default shader should be used. 
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @return the shader program to use.
	 */
	public abstract OGLShaderProgram getShader(int x, int y);

	/**
	 * Gets the appropriate amount of texture units for a set of coordinates. 
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @return the amount of textures to bind.
	 */
	public abstract int getTextureCount(int x, int y);

	/**
	 * Gets the appropriate texture unit for a set of coordinates. 
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @param unit the unit number.
	 * @return the texture to use.
	 */
	public abstract OGLTexture2D getTexture(int x, int y, int unit);

	/**
	 * Gets the appropriate texture offsets for a set of coordinates. 
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @param out the output array, must be length 4 or greater. 
	 * Assumed order is S0, T0, S1, T1.
	 */
	public abstract void getTextureOffsets(int x, int y, float[] out);

	/**
	 * Gets the appropriate texture mode for rendering this tile.
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @return the texture mode to use.
	 */
	public abstract TextureMode getTextureMode(int x, int y);

	/**
	 * Gets the appropriate blending mode for rendering this tile.
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @return the blending function to use.
	 */
	public abstract BlendFunc getBlendingFunction(int x, int y);

	/**
	 * Gets the appropriate ARGB color integer for a set of coordinates and the
	 * current wrapping types.
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @param outColor the output color.
	 */
	public abstract void getColor(int x, int y, OGLColor outColor);
	
}