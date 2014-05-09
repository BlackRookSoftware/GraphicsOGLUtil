/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.overlay;

import com.blackrook.commons.math.RMath;
import com.blackrook.ogl.OGLGraphics;
import com.blackrook.ogl.OGLMesh;
import com.blackrook.ogl.data.OGLColor;
import com.blackrook.ogl.enums.AttribType;
import com.blackrook.ogl.enums.BlendFunc;
import com.blackrook.ogl.enums.FaceSide;
import com.blackrook.ogl.enums.GeometryType;
import com.blackrook.ogl.enums.MatrixType;
import com.blackrook.ogl.mesh.PolygonMesh;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture2D;
import com.blackrook.ogl.util.OGLSkin;
import com.blackrook.ogl.util.OGLResourceLoader;
import com.blackrook.ogl.util.OGLResourceLoaderUser;
import com.blackrook.ogl.util.OGLSkin.Step;
import com.blackrook.ogl.util.resource.OGLShaderResource;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * An overlay node that paints the entire canvas with a solid color
 * or with a skin. It can be blended in diferent ways with
 * the color buffer's contents, and does NOT do any depth testing.
 * It can absorb all input at this node, if you wish.
 * @author Matthew Tropiano
 */
public class OGLOverlayNode implements OGLResourceLoaderUser
{
	private static final OGLMesh QUAD = new PolygonMesh(GeometryType.QUADS, 4, 1)
	{{
		setVertex(0, -1, 1, 0);
		setTextureCoordinate(0, 0, 0);
		setVertex(1, -1, -1, 0);
		setTextureCoordinate(1, 0, 1);
		setVertex(2, 1, -1, 0);
		setTextureCoordinate(2, 1, 1);
		setVertex(3, 1, 1, 0);
		setTextureCoordinate(3, 1, 0);
	}};
	
	/** Reference to Resource loader. */
	private OGLResourceLoader loader;
	/** Is this layer enabled? */
	private boolean enabled;
	/** Is this layer enabled (responds to input)? */
	private boolean acceptsInput;

	/** Color matrix component red. */
	protected float colorRed;
	/** Color matrix component green. */
	protected float colorGreen;
	/** Color matrix component blue. */
	protected float colorBlue;
	/** Color matrix component alpha. */
	protected float colorAlpha;
	/** Blending function. */
	protected BlendFunc blendingFunction;
	/** The OGLSkin used for the overlay. */
	protected OGLSkin renderGroup;
	
	/** Render time in nanos. */
	protected long renderTimeNanos;
	/** Polygons Rendered */
	protected int polygonsRendered;
	
	/**
	 * Creates a new OGLOverlayNode that does not intercept
	 * input controls and is completely transparent and black
	 * (color is [0,0,0,0], RGBA), blend function is BlendFunc.ALPHA,
	 * and starts disabled (but visible).
	 * Requires a resource loader. 
	 */
	public OGLOverlayNode(OGLResourceLoader loader)
	{
		setResourceLoader(loader);
		setColor(0,0,0,0);
		setBlendingFunction(BlendFunc.ALPHA);
		setAcceptingInput(false);
		setEnabled(true);
	}
	
	@Override
	public OGLResourceLoader getResourceLoader()
	{
		return loader;
	}

	@Override
	public void setResourceLoader(OGLResourceLoader loader)
	{
		this.loader = loader;
	}

	/**
	 * Sets the entire overlay color.
	 * This color is mixed with all objects that are going to be rendered.
	 * The resultant values are clamped from 0 to 1.
	 */
	public void setColor(float red, float green, float blue, float alpha)
	{
		colorRed = RMath.clampValue(red, 0f, 1f);
		colorGreen = RMath.clampValue(green, 0f, 1f);
		colorBlue = RMath.clampValue(blue, 0f, 1f);
		colorAlpha = RMath.clampValue(alpha, 0f, 1f);
	}

	/**
	 * Sets the entire overlay color. 
	 * This color is mixed with all objects that are going to be rendered.
	 * The resultant values are clamped from 0 to 1.
	 */
	public void setColor(OGLColor c)
	{
		setColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
	}

	/**
	 * Adds a color to the current overlay color. 
	 * Negative values, of course, subtract from the current color.
	 * The resultant values are clamped from 0 to 1.
	 */
	public void addColor(float red, float green, float blue, float alpha)
	{
		setColor(colorRed + red, colorGreen + green, colorBlue + blue, colorAlpha + alpha);
	}

	/**
	 * Adds a color to the current overlay color.
	 * Negative values, of course, subtract from the current color.
	 * The resultant values are clamped from 0 to 1.
	 */
	public void addColor(OGLColor c)
	{
		addColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
	}

	/**
	 * Sets the alpha component of this overlay.
	 * This color is mixed with all objects that are going to be rendered.
	 * The resultant value is clamped from 0 to 1.
	 */
	public void setOpacity(float alpha)
	{
		colorAlpha = RMath.clampValue(alpha, 0f, 1f);
	}

	/**
	 * Adds an alpha component to the current overlay color.
	 * Negative values, of course, subtract from the current alpha.
	 * The resultant values are clamped from 0 to 1.
	 */
	public void addOpacity(float alpha)
	{
		setOpacity(colorAlpha + alpha);
	}

	/**
	 * Gets the color blending function to use for blending this overlay
	 * with the color buffer. 
	 * This gets overridden if a skin gets bound to this.
	 */
	public BlendFunc getBlendingFunction()
	{
		return blendingFunction;
	}

	/**
	 * Sets the color blending function to use for blending this overlay
	 * with the color buffer.
	 * This gets overridden if a skin gets bound to this.
	 */
	public void setBlendingFunction(BlendFunc blendingFunction)
	{
		this.blendingFunction = blendingFunction;
	}

	/**
	 * Gets the skin to used with this overlay node.
	 * This overrides the blending mode set on this overlay.
	 */
	public OGLSkin getRenderGroup()
	{
		return renderGroup;
	}

	/**
	 * Sets the skin to used with this overlay node.
	 * This overrides the blending mode set on this overlay.
	 */
	public void setRenderGroup(OGLSkin renderGroup)
	{
		this.renderGroup = renderGroup;
	}

	@Override
	public void onCanvasResize(int newWidth, int newHeight)
	{
		// Doesn't matter.
	}

	@Override
	public void display(OGLGraphics g)
	{
		long nanos = System.nanoTime();
		polygonsRendered = 0;
		g.attribPush(
				AttribType.ENABLE, 			// "enable"
				AttribType.LIGHTING, 		// light
				AttribType.DEPTH_BUFFER, 	// depth func/mask
				AttribType.COLOR_BUFFER,	// blend/color
				AttribType.POLYGON);		// face cull
		
		g.setBlendingEnabled(true);
		g.setDepthTestEnabled(false);
		g.setDepthMask(false);
		g.setLightingEnabled(false);
		g.setFaceCullingEnabled(true);
		g.setFaceCullingSide(FaceSide.BACK);
		
		if (renderGroup != null)
		{
			g.setTexture2DEnabled(true);
			long currentTime = g.currentTimeMillis();
			
			OGLShaderProgram shader = null; 
			OGLTexture2D texture = null;
			OGLTexture2D[] multitexture = null;

			for (Step step : renderGroup)
			{
				
				OGLShaderResource oglsr = step.getShaderProgram();
				if (oglsr != null)
				{
					if (!loader.containsShader(oglsr))
						loader.cacheShader(g, oglsr);
					shader = loader.getShader(oglsr);
				}
				else
					shader = null;

				if (shader != null)
					shader.bindTo(g);
				else
					g.unbindShaderProgram();

				if (!step.isMultitexture())
				{
					int ti = step.getTextureIndex(currentTime);
					if (ti >= 0)
					{
						OGLTextureResource ogltr = step.getTextureList()[ti];
						if (!loader.containsTexture(ogltr))
							loader.cacheTexture(g, ogltr);
						texture = loader.getTexture(ogltr);
					}
					else
						texture = null;

					g.setTextureUnit(0);
					if (texture != null)
						texture.bindTo(g);
					else
						g.unbindTexture2D();
				}
				else if (step.getTextureList().length > 0)
				{
					multitexture = new OGLTexture2D[step.getTextureList().length];
					int i = 0;
					for (OGLTextureResource ogltr : step.getTextureList())
					{
						if (!loader.containsTexture(ogltr))
							loader.cacheTexture(g, ogltr);
						multitexture[i] = loader.getTexture(ogltr);
						g.setTextureUnit(i);
						if (multitexture[i] != null)
							multitexture[i].bindTo(g);
						else
							g.unbindTexture2D();
						i++;
					}
				}
				else
				{
					g.setTextureUnit(0);
					g.unbindTexture2D();
				}

				switch (step.getBlendType())
				{
					case REPLACE:
						g.setBlendingFunc(BlendFunc.REPLACE);
						break;
					case ALPHA:
						g.setBlendingFunc(BlendFunc.ALPHA);
						break;
					case ADD:
						g.setBlendingFunc(BlendFunc.ADDITIVE);
						break;
					case MULTIPLY:
						g.setBlendingFunc(BlendFunc.MULTIPLICATIVE);
						break;
				}

				float texture_rot = step.getTextureRotation(currentTime);
				float texture_s0 = step.getTextureS0(currentTime);
				float texture_t0 = step.getTextureT0(currentTime);
				float texture_s1 = step.getTextureS1(currentTime);
				float texture_t1 = step.getTextureT1(currentTime);
				float pivot_s = step.getTextureRotationPivotS();
				float pivot_t = step.getTextureRotationPivotT();
				
				g.setTextureUnit(0);
				g.matrixMode(MatrixType.TEXTURE); 
				g.matrixPush();
				g.matrixTranslate(-pivot_s, -pivot_t, 0);
				g.matrixRotateZ(texture_rot);
				g.matrixTranslate(texture_s0-pivot_s, texture_t0-pivot_t, 0);
				g.matrixScale(texture_s1-texture_s0, texture_t1-texture_t0, 1);
				
				g.setColor(
						colorRed * step.getColorRed(currentTime),
						colorGreen * step.getColorGreen(currentTime),
						colorBlue * step.getColorBlue(currentTime),
						colorAlpha * step.getColorAlpha(currentTime)
						);
				
				drawOverlay(g);
				
				g.unbindShaderProgram();
				if (texture == null && multitexture != null)
				{
					for (int i = 0; i < multitexture.length; i++)
					{
						g.setTextureUnit(i);
						g.unbindTexture2D();
					}
				}
				else if (texture != null)
				{
					g.setTextureUnit(0);
					g.unbindTexture2D();
				}
				
				g.setTextureUnit(0);
				g.matrixMode(MatrixType.TEXTURE);
				g.matrixPop();
			}
		}
		else
		{
			g.setBlendingFunc(blendingFunction);
			g.setTexture2DEnabled(false);
			g.setColor(colorRed, colorGreen, colorBlue, colorAlpha);
			drawOverlay(g);
		}
		
		g.attribPop();
		renderTimeNanos = System.nanoTime() - nanos;
	}

	/**
	 * Draws the overlay.
	 * @param g the graphics context to use.
	 */
	protected void drawOverlay(OGLGraphics g)
	{
		g.matrixMode(MatrixType.MODELVIEW);
		g.matrixPush();
		g.matrixReset();
		
		g.matrixMode(MatrixType.PROJECTION);
		g.matrixPush();
		g.matrixReset();

		g.matrixOrtho(-1, 1, -1, 1, -1, 1);
		QUAD.getView().drawUsing(g);
		polygonsRendered++;
		
		g.matrixMode(MatrixType.PROJECTION);
		g.matrixPop();
		
		g.matrixMode(MatrixType.MODELVIEW);
		g.matrixPop();
	}

	@Override
	public int getPolygonsRendered()
	{
		return polygonsRendered;
	}

	@Override
	public long getRenderTimeNanos()
	{
		return renderTimeNanos;
	}

	@Override
	public boolean glKeyPress(int keycode)
	{
		return acceptsInput;
	}

	@Override
	public boolean glKeyRelease(int keycode)
	{
		return acceptsInput;
	}

	@Override
	public boolean glKeyTyped(int keycode)
	{
		return acceptsInput;
	}

	@Override
	public void glMouseMove(int unitsX, int coordinateX, int units, int coordinate)
	{
	}

	@Override
	public void glMouseEnter()
	{
	}

	@Override
	public void glMouseExit()
	{
	}

	@Override
	public boolean glMousePress(int mousebutton)
	{
		return acceptsInput;
	}

	@Override
	public boolean glMouseRelease(int mousebutton)
	{
		return acceptsInput;
	}

	@Override
	public boolean glMouseWheel(int units)
	{
		return acceptsInput;
	}

	@Override
	public boolean glGamepadPress(int gamepadId, int gamepadButton)
	{
		return acceptsInput;
	}

	@Override
	public boolean glGamepadRelease(int gamepadId, int gamepadButton)
	{
		return acceptsInput;
	}

	@Override
	public boolean glGamepadAxisChange(int gamepadId, int gamepadAxisId, float value)
	{
		return acceptsInput;
	}

	@Override
	public boolean glGamepadAxisTap(int gamepadId, int gamepadAxisId, boolean positive)
	{
		return acceptsInput;
	}

	@Override
	public boolean isEnabled()
	{
		return enabled && colorAlpha > 0f;
	}
	
	/**
	 * Sets if this node is enabled.
	 * @see #isEnabled()
	 */
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	/**
	 * Gets if this node is accepting input.
	 * If this is true, then this node will absorb 
	 * input from the keyboard and mouse.
	 */
	public boolean isAcceptingInput()
	{
		return acceptsInput;
	}
	
	/**
	 * Sets if this node is accepting input.
	 * If this is true, then this node will absorb 
	 * input from the keyboard and mouse.
	 */
	public void setAcceptingInput(boolean acceptsInput)
	{
		this.acceptsInput = acceptsInput;
	}
	
	

}
