/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util;

import com.blackrook.commons.list.List;
import com.blackrook.commons.spatialhash.SpatialHashable;
import com.blackrook.ogl.data.OGLColor;

/**
 * Encapsulated camera info for scenes. Comes with a way to detect changes in an event-model fashion.
 * @author Matthew Tropiano
 */
public class OGL2DCamera implements SpatialHashable
{
	/** Set of registered camera listeners. */
	protected List<OGL2DCameraListener> listeners;

	/** Camera X */
	protected float cameraX;
	/** Camera Y */
	protected float cameraY;
	/** Camera Width */
	protected float cameraWidth;
	/** Camera Height */
	protected float cameraHeight;

	/** Scissor X in camera length. */
	protected float scissorX;
	/** Scissor Y in camera height. */
	protected float scissorY;
	/** Scissor width in screen lengths. */
	protected float scissorWidth;
	/** Scissor height in screen lengths. */
	protected float scissorHeight;
	/** Scissor enabled. */
	protected boolean scissorEnabled;
	
	/** Camera's red color. */
	protected float red;
	/** Camera's green color. */
	protected float green;
	/** Camera's blue color. */
	protected float blue;
	/** Camera's alpha color. */
	protected float alpha;
	
	public OGL2DCamera()
	{
		listeners = new List<OGL2DCameraListener>(1,3);
		cameraX = 0f;
		cameraY = 0f;
		cameraWidth = 0f;
		cameraHeight = 0f;
		scissorX = 0f;
		scissorY = 0f;
		scissorWidth = 1f;
		scissorHeight = 1f;
		scissorEnabled = false;
		red = 1f;
		green = 1f;
		blue = 1f;
		alpha = 1f;
	}
	
	/**
	 * Adds a camera listener to this.
	 */
	public void addListener(OGL2DCameraListener listener)
	{
		listeners.add(listener);
	}
	
	/**
	 * Removes a camera listener from this.
	 */
	public boolean removeListener(OGL2DCameraListener listener)
	{
		return listeners.remove(listener);
	}
	
	/**
	 * Sets the camera bounds.
	 * @param x			the left corner of the camera.
	 * @param y			the top corner of the camera.
	 * @param width		the width of the camera.
	 * @param height	the height of the camera.
	 */
	public void setBounds(float x, float y, float width, float height)
	{
		if (height == 0) height = 1;
		float newCamWidth = width;
		float newCamHeight = height;
	
		float cameraChangeX = x - cameraX;
		float cameraChangeY = y - cameraY;
		float cameraChangeWidth = newCamWidth - cameraWidth;
		float cameraChangeHeight = newCamHeight - cameraHeight;
	
		cameraX = x;
		cameraY = y;
		cameraWidth = newCamWidth;
		cameraHeight = newCamHeight;
		
		if (cameraChangeX != 0 || cameraChangeY != 0 || cameraChangeWidth != 0 || cameraChangeHeight != 0)
			fireChange(cameraChangeX, cameraChangeY, cameraChangeWidth, cameraChangeHeight);
	}
	/**
	 * Translates the camera bounds.
	 * @param x		the amount to move the camera, X coordinate.
	 * @param y		the amount to move the camera, Y coordinate.
	 */
	public void translateBounds(float x, float y)
	{
		float cameraChangeX = x;
		float cameraChangeY = y;
	
		cameraX += x;
		cameraY += y;
	
		if (x != 0 || y != 0)
			fireChange(cameraChangeX, cameraChangeY, 0, 0);
	}
	/**
	 * Stretches the camera bounds.
	 * @param width	the amount to stretch the camera width.
	 * @param height the amount to stretch the camera height.
	 */
	public void stretchBounds(float width, float height)
	{
		float cameraChangeWidth = width;
		float cameraChangeHeight = height;
	
		cameraWidth += width;
		cameraHeight += height;
	
		if (width != 0 || height != 0)
			fireChange(0, 0, cameraChangeWidth, cameraChangeHeight);
	}
	/**
	 * Zooms the camera bounds in or out.
	 * @param factor the factor to zoom the camera.
	 */
	public void zoomBounds(float factor)
	{
		float w = 0f;
		float h = 0f;
		
		w = cameraWidth * factor;
		h = cameraHeight * factor; 
	
		float cameraChangeWidth = w - cameraWidth;
		float cameraChangeHeight = h - cameraHeight;
		float cameraChangeX = (cameraWidth - w) / 2;
		float cameraChangeY = (cameraHeight - h) / 2;
	
		cameraX += (cameraWidth - w) / 2;
		cameraY += (cameraHeight - h) / 2;
		cameraWidth = w;
		cameraHeight = h;
	
		if (factor != 1.0f)
			fireChange(cameraChangeX, cameraChangeY, cameraChangeWidth, cameraChangeHeight);
	}

	/**
	 * Changes this camera's bounds according to a new aspect ratio, centering the original bounds. 
	 * Also affects scissor bounds, regardless of whether it is enabled or not.
	 * If you plan to use this with subsequent calls, resubmit the correct bounds before
	 * calling this again.
	 * @param aspectRatio the new target aspect ratio.
	 */
	public void correctBoundsUsingAspect(float aspectRatio)
	{
		float cx = cameraX;
		float cy = cameraY;
		float cw = cameraWidth;
		float ch = cameraHeight;
		
		float ca = cameraWidth / cameraHeight;
		
		float sux = scissorX * cameraWidth;
		float suy = scissorY * cameraHeight;
		float suw = scissorWidth * cameraWidth;
		float suh = scissorHeight * cameraHeight;
		
		// make wider (height stays, width changes)
		if (aspectRatio > ca)
		{
			float w = ch * aspectRatio;
			float wh = (w - cw) / 2;
			cw = w;
			cx -= wh;

			sux += wh;
		}
		// make skinnier (width stays, height changes)
		else if (aspectRatio < ca)
		{
			float h = cw / aspectRatio;
			float hh = (h - ch) / 2;
			ch = h;
			cy -= hh;
			suy += hh;
		}
		
		setBounds(cx, cy, cw, ch);
		setScissorBounds(sux / cw, suy / ch, suw / cw, suh / ch);
	}
	
	/**
	 * Sets this scene's scissor bounds in camera lengths.
	 * Only used if scissor enabled is true.
	 * @param x x-axis scissor box start (0 to 1).
	 * @param y y-axis scissor box start (0 to 1).
	 * @param width the width of the scissor box in camera lengths (0 to 1).
	 * @param height the height of the scissor box in camera lengths (0 to 1).
	 */
	public void setScissorBounds(float x, float y, float width, float height)
	{
		scissorX = x;
		scissorY = y;
		scissorWidth = width;
		scissorHeight = height;
	}
	
	/**
	 * Sets if the scissor box is enabled.
	 * See {@link OGL2DCamera#setScissorBounds(float, float, float, float)}.
	 * @param enabled
	 */
	public void setScissorEnabled(boolean enabled)
	{
		scissorEnabled = enabled;
	}
	
	/**
	 * A method that gets called whenever this camera gets changed somehow.
	 */
	protected void fireChange(float changeX, float changeY, float changeWidth, float changeHeight)
	{
		for (OGL2DCameraListener listener : listeners)
			listener.onCameraChange(changeX, changeY, changeWidth, changeHeight);
	}

	public float getX()
	{
		return cameraX;
	}

	public float getY()
	{
		return cameraY;
	}

	public float getHeight()
	{
		return cameraHeight;
	}

	public float getWidth()
	{
		return cameraWidth;
	}

	@Override
	public float getObjectHalfDepth()
	{
		return 0;
	}

	@Override
	public float getObjectHalfHeight()
	{
		return cameraHeight/2f;
	}

	@Override
	public float getObjectHalfWidth()
	{
		return cameraWidth/2f;
	}

	@Override
	public float getObjectCenterX()
	{
		return cameraX + (cameraWidth/2f);
	}

	@Override
	public float getObjectCenterY()
	{
		return cameraY + (cameraHeight/2f);
	}

	@Override
	public float getObjectCenterZ()
	{
		return 0;
	}

	@Override
	public float getObjectRadius()
	{
		return (float)Math.sqrt((cameraWidth/2f)*(cameraWidth/2f) + (cameraHeight/2f)*(cameraHeight/2f));
	}

	@Override
	public boolean useObjectRadius()
	{
		return false;
	}

	public float getScissorX()
	{
		return scissorX;
	}

	public float getScissorY()
	{
		return scissorY;
	}

	public float getScissorWidth()
	{
		return scissorWidth;
	}

	public float getScissorHeight()
	{
		return scissorHeight;
	}
	
	public boolean getScissorEnabled() 
	{
		return scissorEnabled;
	}

	@Override
	public float getObjectSweepX()
	{
		return 0;
	}

	@Override
	public float getObjectSweepY()
	{
		return 0;
	}

	@Override
	public float getObjectSweepZ()
	{
		return 0;
	}

	/**
	 * Sets the camera's color.
	 * The final color of objects "seen" by this camera are multiplied by this color.
	 * @param red the red channel color.
	 * @param green the green channel color.
	 * @param blue the blue channel color.
	 * @param alpha the alpha channel color.
	 */
	public void setColor(float red, float green, float blue, float alpha)
	{
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
	}
	
	/**
	 * Sets the camera's color.
	 * The final color of objects "seen" by this camera are multiplied by this color.
	 * @param color
	 */
	public void setColor(OGLColor color)
	{
		setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}
	
	/**
	 * Gets this camera's red color scalar.
	 */
	public float getRed()
	{
		return red;
	}

	/**
	 * Gets this camera's green color scalar.
	 */
	public float getGreen()
	{
		return green;
	}

	/**
	 * Gets this camera's blue color scalar.
	 */
	public float getBlue()
	{
		return blue;
	}

	/**
	 * Gets this camera's alpha color scalar.
	 */
	public float getAlpha()
	{
		return alpha;
	}

}
