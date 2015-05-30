/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.tile2d.model;

import com.blackrook.commons.math.RMath;
import com.blackrook.ogl.data.OGLColor;
import com.blackrook.ogl.enums.BlendFunc;
import com.blackrook.ogl.enums.TextureMode;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture2D;
import com.blackrook.ogl.util.resource.OGLTextureResource;
import com.blackrook.ogl.util.tile2d.OGLTile2DModel;
import com.blackrook.ogl.util.tile2d.OGLTile2DNode;

/**
 * A default rendering model to use for {@link OGLTile2DNode}.
 * You can set skins explicitly for each tile position.
 * @author Matthew Tropiano
 */
public class TileDefaultModel implements OGLTile2DModel
{
	/**
	 * Grid model wrapping.
	 */
	public enum Wrap
	{
		/** No wrapping. Coordinates outside of the grid are null. */
		NONE,
		/** Clamp coordinates. Coordinates outside of the grid are nearest edge. */
		CLAMP,
		/** Tile coordinates. Coordinates outside of the grid are into the next. */
		TILE;
	}
	
	/** OGL Skin map. */
	private OGLTextureResource[][][] textures;
	/** Wrapping model, X-coordinate. */
	private Wrap wrapX;
	/** Wrapping model, Y-coordinate. */
	private Wrap wrapY;

	/**
	 * Creates a new default model with no wrapping and a set width an height.
	 * @param width 
	 * @param height
	 * @throws IllegalArgumentException if width or height is 0 or less.
	 */
	public TileDefaultModel(int width, int height)
	{
		if (width <= 0 || height <= 0)
			throw new IllegalArgumentException("Neither width nor height can be null.");
		
		textures = new OGLTextureResource[width][height][0];
		wrapX = Wrap.NONE;
		wrapY = Wrap.NONE;
	}
	
	/**
	 * Gets the width of this model's grid.
	 */
	public int getWidth()
	{
		return textures.length;
	}
	
	/**
	 * Gets the height of this model's grid.
	 */
	public int getHeight()
	{
		return textures[0].length;
	}
	
	/**
	 * Sets the wrapping of the X-axis grid values.
	 */
	public void setWrapX(Wrap wrapX)
	{
		this.wrapX = wrapX;
	}
	
	/**
	 * Sets the wrapping of the Y-axis grid values.
	 */
	public void setWrapY(Wrap wrapY)
	{
		this.wrapY = wrapY;
	}
	
	/**
	 * Gets the wrapping of the X-axis grid values.
	 */
	public Wrap getWrapX()
	{
		return wrapX;
	}
	
	/**
	 * Gets the wrapping of the Y-axis grid values.
	 */
	public Wrap getWrapY()
	{
		return wrapY;
	}
	
	@Override
	public BlendFunc getBlendingFunction(int x, int y)
	{
		return BlendFunc.REPLACE;
	}

	/**
	 * Sets a textures at a particular coordinate.
	 * @param x the x-coordinate.
	 * @param y the y-coordinate.
	 */
	public void setTextures(int x, int y, OGLTextureResource ... textures)
	{
		this.textures[x][y] = textures;
	}
	
	@Override
	public int getTextures(int x, int y, OGLTexture2D[] outTextures)
	{
		x = correctX(x);
		y = correctY(y);
		if (!isOnGrid(x, y))
			return 0;
		
		int out = Math.min(textures[x][y].length, outTextures.length);
		System.arraycopy(textures[x][y], 0, outTextures, 0, out);
		return out;
	}

	@Override
	public TextureMode getTextureMode(int x, int y)
	{
		return TextureMode.REPLACE;
	}

	@Override
	public boolean getVisible(int x, int y)
	{
		x = correctX(x);
		y = correctY(y);
		return isOnGrid(x, y) && textures[x][y].length > 0; 
	}

	@Override
	public OGLShaderProgram getShader(int x, int y)
	{
		x = correctX(x);
		y = correctY(y);
		if (!isOnGrid(x, y))
			return null;
	
		return null;
	}

	@Override
	public void getColor(int x, int y, OGLColor outColor)
	{
		x = correctX(x);
		y = correctY(y);
		if (isOnGrid(x, y))
			outColor.set(OGLColor.WHITE);
	}

	@Override
	public void getTextureOffsets(int x, int y, float[] out)
	{
		out[0] = 0f;
		out[1] = 0f;
		out[2] = 1f;
		out[3] = 1f;
	}

	/**
	 * Checks if a set of coordinates are within the grid's bounds.
	 * @param x the x-coordinate.
	 * @param y the y-coordinate.
	 * @return true if so, false if not.
	 */
	public boolean isOnGrid(int x, int y)
	{
		return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
	}
	
	/**
	 * Returns the corrected X-coordinate based on wrapping.
	 */
	protected int correctX(int x)
	{
		switch (wrapX)
		{
			default:
			case NONE:
				return x;
			case CLAMP:
				return RMath.clampValue(x, 0, getWidth() - 1);
			case TILE:
				return RMath.wrapValue(x, 0, getWidth());
		}
	}
	/**
	 * Returns the corrected Y-coordinate based on wrapping.
	 */
	protected int correctY(int y)
	{
		switch (wrapY)
		{
			default:
			case NONE:
				return y;
			case CLAMP:
				return RMath.clampValue(y, 0, getHeight() - 1);
			case TILE:
				return RMath.wrapValue(y, 0, getHeight());
		}
	}

}
