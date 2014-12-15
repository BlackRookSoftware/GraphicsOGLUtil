/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util;

/**
 * Encapsulated camera info for scenes. Comes with a way to detect changes in an event-model fashion.
 * @author Matthew Tropiano
 */
public class OGL2DCamera
{
	/** Camera viewport X start (in canvas widths, 0 to 1). */
	protected float viewportX;
	/** Camera viewport Y start (in canvas heights, 0 to 1). */
	protected float viewportY;
	/** Camera viewport width (in canvas widths, 0 to 1). */
	protected float viewportWidth;
	/** Camera viewport height (in canvas heights, 0 to 1). */
	protected float viewportHeight;

	/** Camera X */
	protected float centerX;
	/** Camera Y */
	protected float centerY;
	/** Camera Width */
	protected float halfWidth;
	/** Camera Height */
	protected float halfHeight;

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
	
	public OGL2DCamera()
	{
		viewportX = 0f;
		viewportY = 0f;
		viewportWidth = 1f;
		viewportHeight = 1f;
		centerX = 0f;
		centerY = 0f;
		halfWidth = 0f;
		halfHeight = 0f;
		scissorX = 0f;
		scissorY = 0f;
		scissorWidth = 1f;
		scissorHeight = 1f;
		scissorEnabled = false;
	}
	
	/**
	 * Sets the camera bounds.
	 * @param x	the left corner of the camera.
	 * @param y	the top/bottom corner of the camera (depending on view).
	 * @param width	the width of the camera.
	 * @param height the height of the camera.
	 */
	public void setBounds(float x, float y, float width, float height)
	{
		if (height == 0) height = 1;

		float hw = width / 2f;
		float hh = height / 2f;
		
		centerX = x + hw;
		centerY = y + hh;
		halfWidth = hw;
		halfHeight = hh;
	}
	
	/**
	 * Sets the camera centerpoint.
	 * @param x	the left corner of the camera.
	 * @param y	the top/bottom corner of the camera (depending on view).
	 */
	public void setCenter(float x, float y)
	{
		centerX = x;
		centerY = y;
	}
	
	/**
	 * Sets the camera dimensions.
	 * @param width	the width of the camera.
	 * @param height the height of the camera.
	 */
	public void setDimensions(float width, float height)
	{
		halfWidth = width / 2f;
		halfHeight = height / 2f;
	}
	
	/**
	 * Sets the camera dimensions in halves of a dimension.
	 * @param halfWidth	the half-width of the camera.
	 * @param halfHeight the half-height of the camera.
	 */
	public void setHalfWidths(float halfWidth, float halfHeight)
	{
		this.halfWidth = halfWidth;
		this.halfHeight = halfHeight;
	}
	
	/**
	 * Translates the camera bounds.
	 * @param x		the amount to move the camera, X coordinate.
	 * @param y		the amount to move the camera, Y coordinate.
	 */
	public void translateBounds(float x, float y)
	{
		centerX += x;
		centerY += y;
	}
	
	/**
	 * Stretches the camera bounds.
	 * @param width	the amount to stretch the camera width.
	 * @param height the amount to stretch the camera height.
	 */
	public void stretchBounds(float width, float height)
	{
		halfWidth += width / 2f;
		halfHeight += height / 2f;
	}
	/**
	 * Zooms the camera bounds in or out.
	 * @param factor the factor to zoom the camera.
	 */
	public void zoomBounds(float factor)
	{
		halfWidth = halfWidth * factor;
		halfHeight = halfHeight * factor;
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
		/*
		float ca = halfWidth / halfHeight;
		
		float sux = scissorX * halfWidth * 2;
		float suy = scissorY * halfHeight * 2;
		float suw = scissorWidth * halfWidth * 2;
		float suh = scissorHeight * halfHeight * 2;
		
		// make wider (height stays, width changes)
		if (aspectRatio > 1f)
		{
			halfWidth = aspectRatio * halfHeight;
		}
		// make skinnier (width stays, height changes)
		else if (aspectRatio < 1f)
		{
			halfHeight = aspectRatio * halfWidth;
		}
		*/
		
		float cx = centerX - halfWidth;
		float cy = centerY - halfHeight;
		float cw = halfWidth * 2;
		float ch = halfHeight * 2;
		
		float ca = halfWidth / halfHeight;
		
		float sux = scissorX * halfWidth * 2;
		float suy = scissorY * halfHeight * 2;
		float suw = scissorWidth * halfWidth * 2;
		float suh = scissorHeight * halfHeight * 2;
		
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
	 * Sets this camera's viewport in camera lengths.
	 * @param x x-axis viewport start (0 to 1).
	 * @param y y-axis viewport start (0 to 1).
	 * @param width the width of the viewport in camera lengths (0 to 1).
	 * @param height the height of the viewport in camera lengths (0 to 1).
	 */
	public void setViewport(float x, float y, float width, float height)
	{
		viewportX = x;
		viewportY = y;
		viewportWidth = width;
		viewportHeight = height;
	}
	
	/**
	 * Returns the radius of the camera view.
	 */
	public float getRadius()
	{
		return (float)Math.sqrt((halfWidth * halfWidth) + (halfHeight * halfHeight));
	}

	/**
	 * Returns the squared radius of the camera view.
	 */
	public float getSquaredRadius()
	{
		return (halfWidth * halfWidth) + (halfHeight * halfHeight);
	}

	public float getViewportX()
	{
		return viewportX;
	}

	public float getViewportY()
	{
		return viewportY;
	}

	public float getViewportWidth()
	{
		return viewportWidth;
	}

	public float getViewportHeight()
	{
		return viewportHeight;
	}

	public float getCenterX()
	{
		return centerX;
	}

	public float getCenterY()
	{
		return centerY;
	}

	public float getHalfHeight()
	{
		return halfHeight;
	}

	public float getHalfWidth()
	{
		return halfWidth;
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

}
