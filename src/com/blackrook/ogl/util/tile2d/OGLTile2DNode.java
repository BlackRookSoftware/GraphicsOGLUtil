/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.tile2d;

import java.nio.FloatBuffer;

import com.blackrook.commons.Common;
import com.blackrook.commons.list.List;
import com.blackrook.ogl.OGLGeometryUtils;
import com.blackrook.ogl.OGLGraphics;
import com.blackrook.ogl.OGLGeometryUtils.GeometryInfo;
import com.blackrook.ogl.enums.AttribType;
import com.blackrook.ogl.enums.BlendFunc;
import com.blackrook.ogl.enums.BufferType;
import com.blackrook.ogl.enums.CachingHint;
import com.blackrook.ogl.enums.FaceSide;
import com.blackrook.ogl.enums.GeometryType;
import com.blackrook.ogl.enums.LogicFunc;
import com.blackrook.ogl.enums.MatrixType;
import com.blackrook.ogl.mesh.PolygonMesh;
import com.blackrook.ogl.object.buffer.OGLFloatBuffer;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture2D;
import com.blackrook.ogl.util.OGL2DCamera;
import com.blackrook.ogl.util.OGLSkin;
import com.blackrook.ogl.util.OGLResourceLoader;
import com.blackrook.ogl.util.OGLResourceLoaderUser;
import com.blackrook.ogl.util.OGLSkin.BlendType;
import com.blackrook.ogl.util.OGLSkin.Step;
import com.blackrook.ogl.util.resource.OGLShaderResource;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * An assisting class for drawing a tile-based whatever.
 * NOTE: This does not pay attention to object rotation from render steps. 
 * @author Matthew Tropiano
 */
public class OGLTile2DNode implements OGLResourceLoaderUser
{
	/** Reference to resource loader. */
	private OGLResourceLoader loader;
	/** The list of bound listeners. */
	private List<OGLTile2DListener> listenerList;
	/** Tile model. */
	private OGLTile2DModel tileModel;

	/** Is this enabled? */
	private boolean enabled;
	/** Does this accept input? */
	private boolean acceptsInput;
	/** Is this focused, so that it can accept key input? */
	private boolean focused;

	/** Default tile width in units. */
	protected float defaultTileWidth;
	/** Default tile height in units. */
	protected float defaultTileHeight;

	/* ============= Data ============= */
	
	/** Depth test? */
	protected boolean depthTest;
	/** Grid Z coordinate for depth buffer. */
	protected float depth;

	/** Camera instance. */
	protected OGL2DCamera camera;
	/** Flip Y? */
	protected boolean flipY;
	
	/* ============= Canvas listener variables ============= */

	/** Canvas width. */
	protected int canvasWidth;
	/** Canvas height. */
	protected int canvasHeight;
	/** Last mouse position seen, X coordinate. */
	protected int canvasMouseX;
	/** Last mouse position seen, Y coordinate. */
	protected int canvasMouseY;
	/** Last mouse position seen, X. 0 is farthest left, 1 is farthest right. */
	protected float canvasMouseDegreeX;
	/** Last mouse position seen, Y. 0 is farthest up, 1 is farthest down. */
	protected float canvasMouseDegreeY;
	/** Current moused-over grid tile X. */
	protected int mouseGridX;
	/** Current moused-over grid tile Y. */
	protected int mouseGridY;
	
	/** Grid position the the mouse was pressed on X. */
	protected int mouseDownX; 
	/** Grid position the the mouse was pressed on Y. */
	protected int mouseDownY; 
	/** Mouse button. */
	protected int mouseDownButton; 

	/* ============= Per frame variables ============= */
	
	/** The list of what to render. */
	protected List<Node> renderList;
	/** Render list end index. */
	protected int renderListSize;
	/** Context for rendering.  */
	protected Context context;
	/** Last step instance. */
	protected StepInstance currentStepInstance; 
	/** VBO Context. */
	protected VBOContext vboContext;
	/** Mesh for non-VBO. */
	protected PolygonMesh mesh;
	/** Temp texture coordinates. */
	protected float[] textureTemp;
	
	/* ============ Benchmarking time ================ */
	
	/** Time to build the scene in nanoseconds. */
	protected long timeBuildScene;
	/** Time to sort the scene in nanoseconds. */
	protected long timeSortScene;
	/** Time to render the scene in nanoseconds. */
	protected long timeRenderScene;

	/** Render time in nanos. */
	protected long renderTimeNanos;
	/** Polygons Rendered */
	protected int polygonsRendered;

	/* =============================================== */

	protected GeometryInfo[] geometryInfo = new GeometryInfo[]
	{
		OGLGeometryUtils.vertices(3, 9, 0),
		OGLGeometryUtils.texCoords(0, 2, 9, 3),
		OGLGeometryUtils.color(4, 9, 5)
	};
	
	/**
	 * Creates a new tile grid.
	 * @param loader the resource loader to use for texture and shader lookup.
	 * @param tileWidth the width of each tile in units.
	 * @param tileHeight the height of the tile in units.
	 */
	public OGLTile2DNode(OGLResourceLoader loader, OGLTile2DModel model, float tileWidth, float tileHeight)
	{
		this(loader, model, new OGL2DCamera(), tileWidth, tileHeight);
	}

	/**
	 * Creates a new tile grid.
	 * @param loader the resource loader to use for texture and shader lookup.
	 * @param camera the camera to use for defining visible bounds.
	 * @param tileWidth the width of each tile in units.
	 * @param tileHeight the height of the tile in units.
	 */
	public OGLTile2DNode(OGLResourceLoader loader, OGLTile2DModel model, OGL2DCamera camera, float tileWidth, float tileHeight)
	{
		renderList = new List<Node>(200);
		listenerList = new List<OGLTile2DListener>(2);
		tileModel = model;
		mesh = new PolygonMesh(GeometryType.QUADS, 4, 1);
		mouseGridX = -1;
		mouseGridY = -1;

		setResourceLoader(loader);
		setCamera(camera);
		setDepthTest(false);
		setDefaultTileWidth(tileWidth);
		setDefaultTileHeight(tileHeight);
		setEnabled(true);
		setAcceptingInput(true);
	}
	
	/**
	 * Adds an OGLGridViewer to this system.
	 */
	public void addGridListener(OGLTile2DListener g)
	{
		listenerList.add(g);
	}

	/**
	 * Removes an OGLGridViewer from this system.
	 */
	public boolean removeGridListener(OGLTile2DListener g)
	{
		return listenerList.remove(g);
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

	@Override
	public void onCanvasResize(int new_width, int new_height)
	{
		canvasWidth = new_width;
		canvasHeight = new_height;
	}

	@Override
	public void display(OGLGraphics g)
	{
		polygonsRendered = 0;
		
		g.attribPush(
				AttribType.LIGHTING, 		// light
				AttribType.ENABLE, 			// "enable"
				AttribType.DEPTH_BUFFER, 	// depth func/mask
				AttribType.COLOR_BUFFER,	// blend/color
				AttribType.POLYGON,			// face cull
				AttribType.SCISSOR);
		
		g.setBlendingEnabled(true);
		g.setDepthTestEnabled(depthTest);
		g.setDepthMask(depthTest);
		g.setLightingEnabled(false);
		g.setFaceCullingEnabled(true);
		if (getFlipY())
			g.setFaceCullingSide(FaceSide.FRONT);
		else
			g.setFaceCullingSide(FaceSide.BACK);
		if (camera.getScissorEnabled())
		{
			float cWidth = g.getCanvasWidth();
			float cHeight = g.getCanvasHeight();
			g.setScissorBounds(
				(int)(camera.getScissorX() * cWidth), 
				(int)(camera.getScissorY() * cHeight), 
				(int)(camera.getScissorWidth() * cWidth), 
				(int)(camera.getScissorHeight() * cHeight));
			g.setScissorTestEnabled(true);
		}

		g.matrixMode(MatrixType.MODELVIEW);
		g.matrixPush();
		g.matrixReset();
		
		g.matrixMode(MatrixType.PROJECTION);
		g.matrixPush();
		g.matrixReset();
		
		if (getFlipY())
			g.matrixOrtho(
				(float)(camera.getObjectCenterX() - camera.getObjectHalfWidth()), 
				(float)(camera.getObjectCenterX() + camera.getObjectHalfWidth()), 
				(float)(camera.getObjectCenterY() + camera.getObjectHalfHeight()), 
				(float)(camera.getObjectCenterY() - camera.getObjectHalfHeight()), 
				-1, 1);
		else
			g.matrixOrtho(
				(float)(camera.getObjectCenterX() - camera.getObjectHalfWidth()), 
				(float)(camera.getObjectCenterX() + camera.getObjectHalfWidth()), 
				(float)(camera.getObjectCenterY() - camera.getObjectHalfHeight()), 
				(float)(camera.getObjectCenterY() + camera.getObjectHalfHeight()), 
				-1, 1);

		createRenderList(g);
		drawRenderList(g);

		g.matrixMode(MatrixType.PROJECTION);
		g.matrixPop();
		
		g.matrixMode(MatrixType.MODELVIEW);
		g.matrixPop();
		
		g.setTextureUnit(0);
		g.attribPop();
		renderTimeNanos = timeBuildScene + timeSortScene + timeRenderScene;
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

	/**
	 * Gets the screen depth for this grid (for depth buffer).
	 */
	public float getDepth()
	{
		return depth;
	}

	/**
	 * Sets the screen depth for this grid (for depth buffer).
	 */
	public void setDepth(float depth)
	{
		this.depth = depth;
	}

	/**
	 * Sets if the depth test is used.
	 */
	public void setDepthTest(boolean test)
	{
		this.depthTest = test;
	}

	/**
	 * Gets if the depth test is used.
	 */
	public boolean getDepthTest(boolean test)
	{
		return depthTest;
	}

	/**
	 * Sets the current camera. 
	 */
	public void setCamera(OGL2DCamera newCamera)
	{
		camera = newCamera;
	}

	/**
	 * Gets the current camera instance.
	 */
	public OGL2DCamera getCamera()
	{
		return camera;
	}

	/**
	 * Gets if the Y-coordinates are flipped vertically (0 is top, not bottom).
	 * @return true if Y is the top, false if bottom.
	 */
	public boolean getFlipY()
	{
		return flipY;
	}

	/**
	 * Sets if the Y-coordinates are flipped vertically (0 is top, not bottom).
	 * True if Y is the top, false if bottom.
	 */
	public void setFlipY(boolean flipY)
	{
		this.flipY = flipY;
	}

	/**
	 * Gets the individual tile width in units for a specific tile.
	 * Calls {@link #getScaleX(int, int)}.
	 */
	public float getTileWidth(int x, int y)
	{
		return defaultTileWidth * getScaleX(x, y);
	}

	/**
	 * Gets the individual tile height in units for a specific tile.
	 * Calls {@link #getScaleY(int, int)}.
	 */
	public float getTileHeight(int x, int y)
	{
		return defaultTileHeight * getScaleY(x, y);
	}

	@Override
	public boolean isEnabled()
	{
		return enabled;
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
	 * If this is true, then this viewer accepts input from the keyboard (if focused)
	 * or mouse unless the mouse is not on the grid at all.
	 */
	public boolean isAcceptingInput()
	{
		return acceptsInput;
	}

	/**
	 * Sets if this node is accepting input.
	 * If this is true, then this viewer accepts input from the keyboard (if focused)
	 * or mouse unless the mouse is not on the grid at all.
	 */
	public void setAcceptingInput(boolean enabled)
	{
		this.acceptsInput = enabled;
	}

	/**
	 * Gets if this node is focused.
	 * If this is true, then this viewer accepts input from the keyboard.
	 */
	public boolean isFocused()
	{
		return focused;
	}

	/**
	 * Sets if this node is focused.
	 * If this is true, then this viewer accepts input from the keyboard.
	 */
	public void setFocused(boolean focused)
	{
		if (focused)
			fireFocus();
		else
			fireUnfocus();
		this.focused = focused;
	}

	/**
	 * Sets the individual tile width in units.
	 */
	public void setDefaultTileWidth(float tileWidth)
	{
		this.defaultTileWidth = tileWidth;
	}

	/**
	 * Sets the individual tile height in units.
	 */
	public void setDefaultTileHeight(float tileHeight)
	{
		this.defaultTileHeight = tileHeight;
	}

	/**
	 * Gets what grid tile (X coordinate) the mouse is pointing at.
	 * Returns -1 if outside of the grid on the screen.
	 */
	public int getMouseGridX()
	{
		return mouseGridX;
	}

	/**
	 * Gets what grid tile (Y coordinate) the mouse is pointing at.
	 * Returns -1 if outside of the grid on the screen.
	 */
	public int getMouseGridY()
	{
		return mouseGridY;
	}

	/**
	 * Gets the current mouse position on the canvas, x-coordinate.
	 */
	public int getCanvasMouseX()
	{
		return canvasMouseX;
	}

	/**
	 * Gets the current mouse position on the canvas, y-coordinate.
	 */
	public int getCanvasMouseY()
	{
		return canvasMouseY;
	}

	/**
	 * Gets the appropriate skin set for a set of coordinates. 
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @return the OGLSkin, or null if no OGLSkin for the position on the grid.
	 */
	public OGLSkin getSkin(int x, int y)
	{
		return tileModel != null ? tileModel.getSkin(x, y) : null; 
	}

	@Override
	public void glMouseMove(int unitsX, int coordinateX, int unitsY, int coordinateY)
	{
		canvasMouseX = coordinateX;
		canvasMouseY = coordinateY;

		setMouseGridCoordinates();
		
		if (mouseDownX >= 0)
		{
			if (mouseDownX == mouseGridX && mouseDownY == mouseGridY)
				fireMouseDrag(mouseDownButton, mouseGridX, mouseGridY, unitsX, coordinateX, unitsY, coordinateY);
		}
	}

	@Override
	public boolean glKeyPress(int keycode)
	{
		if (!acceptsInput || !focused)
			return false;
		
		if (mouseGridX < 0 || mouseGridY < 0)
			return false;
		
		fireKeyPress(keycode, mouseGridX, mouseGridY);
		return true;
	}

	@Override
	public boolean glKeyRelease(int keycode)
	{
		if (!acceptsInput || !focused)
			return false;
		
		if (mouseGridX < 0 || mouseGridY < 0)
			return false;

		fireKeyRelease(keycode, mouseGridX, mouseGridY);
		return true;
	}

	@Override
	public boolean glKeyTyped(int keycode)
	{
		if (!acceptsInput || !focused)
			return false;
		
		if (mouseGridX < 0 || mouseGridY < 0)
			return false;

		fireKeyTyped(keycode, mouseGridX, mouseGridY);
		return true;
	}

	@Override
	public boolean glMousePress(int mousebutton)
	{
		if (!acceptsInput)
			return false;
		
		if (mouseGridX < 0 || mouseGridY < 0)
			return false;

		mouseDownX = mouseGridX;
		mouseDownY = mouseGridY;
		
		fireMousePress(mousebutton, mouseGridX, mouseGridY);
		return true;
	}

	@Override
	public boolean glMouseRelease(int mousebutton)
	{
		if (!acceptsInput)
			return false;
		
		if (mouseDownX >= 0)
		{
			if (mouseDownX == mouseGridX && mouseDownY == mouseGridY && mouseDownButton == mousebutton)
				fireMouseClick(mousebutton, mouseGridX, mouseGridY);
		}
		
		mouseDownX = -1;
		mouseDownY = -1;

		if (mouseGridX < 0 || mouseGridY < 0)
			return false;

		fireMouseRelease(mousebutton, mouseGridX, mouseGridY);
		return true;
	}

	@Override
	public boolean glMouseWheel(int units)
	{
		if (!acceptsInput)
			return false;
		
		if (mouseGridX < 0 || mouseGridY < 0)
			return false;

		fireMouseWheel(units, mouseGridX, mouseGridY);
		return true;
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
	public boolean glGamepadPress(int gamepadId, int gamepadButton)
	{
		return false;
	}

	@Override
	public boolean glGamepadRelease(int gamepadId, int gamepadButton)
	{
		return false;
	}

	@Override
	public boolean glGamepadAxisChange(int gamepadId, int gamepadAxisId, float value)
	{
		return false;
	}

	@Override
	public boolean glGamepadAxisTap(int gamepadId, int gamepadAxisId, boolean positive)
	{
		return false;
	}

	/**
	 * Draws the tiles.
	 */
	protected void drawRenderList(OGLGraphics g)
	{
		timeRenderScene = System.nanoTime();
		
		g.setTexture2DEnabled(true);
		
		if (context == null)
		{
			// reset context
			g.setTextureUnit(0);
			g.unbindTexture2D();
			g.unbindShaderProgram();
			g.setBlendingFunc(BlendFunc.REPLACE);
			context = new Context();
		}
		else
		{
			// reset context
			if (context.multitexture != null) for (int i = 0; i < context.multitexture.length; i++)
			{
				g.setTextureUnit(i);
				g.unbindTexture2D();
			}
			else
			{
				g.setTextureUnit(0);
				g.unbindTexture2D();
			}
			
			g.unbindShaderProgram();
			g.setBlendingFunc(BlendFunc.REPLACE);
			context.clear();
		}
	
		for (int i = 0; i < renderListSize; i++)
		{
			Node n = renderList.getByIndex(i);
			displayContextStepBreak(g, context, n);
			displayContextPassBreak(g, context, n);
			displayContextShaderBreak(g, context, n);
			displayContextTextureBreak(g, context, n);
			displayContextBlendBreak(g, context, n);
			displayObject(g, n);
		}
		
		if (g.supportsVertexBuffers())
			displayVBOBreak(g);
	
		timeRenderScene = System.nanoTime() - timeRenderScene;
	}

	/**
	 * Performs a pass break if necessary.
	 */
	protected void displayContextStepBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.step != n.nodeStepRef)
		{
			displayVBOBreak(g);
			context.step = n.nodeStepRef;
			setStepInstance(g, context.step); 
		}
	}

	/**
	 * Performs a pass break if necessary.
	 */
	protected void displayContextPassBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.pass != n.nodePass)
		{
			displayVBOBreak(g);
			context.pass = n.nodePass;
			if (depthTest)
			{
				if (context.pass == 0)
					g.setDepthFunc(LogicFunc.GREATER_OR_EQUAL);
				else
					g.setDepthFunc(LogicFunc.EQUAL);
			}
		}
	}

	/**
	 * Performs a shader break if necessary.
	 */
	protected void displayContextShaderBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.shader != n.nodeShader)
		{
			displayVBOBreak(g);
			context.shader = n.nodeShader;
			if (context.shader != null)
				context.shader.bindTo(g);
			else
				g.unbindShaderProgram();
		}
	}

	/**
	 * Performs a texture break if necessary.
	 */
	protected void displayContextTextureBreak(OGLGraphics g, Context context, Node n)
	{
		if (!n.nodeStepRef.isMultitexture())
		{
			if (context.texture != n.nodeTexture || context.multitexture != null)
			{
				displayVBOBreak(g);
				if (context.multitexture != null) for (int t = 0; t < context.multitexture.length; t++)
				{
					g.setTextureUnit(t);
					g.unbindTexture2D();
				}
					
				context.multitexture = null;
				context.texture = n.nodeTexture;
				g.setTextureUnit(0);
				if (context.texture != null)
					context.texture.bindTo(g);
				else
					g.unbindTexture2D();
			}
		}
		else if (n.nodeMultiTexture.length > 0)
		{
			if (context.multitexture != n.nodeMultiTexture || context.texture != null)
			{
				displayVBOBreak(g);
				context.texture = null;
				context.multitexture = n.nodeMultiTexture;
				for (int i = 0; i < context.multitexture.length; i++)
				{
					if (context.multitexture[i] != null)
					{
						g.setTextureUnit(i);
						context.multitexture[i].bindTo(g);
					}
					else
					{
						g.setTextureUnit(i);
						g.unbindTexture2D();
					}
				}
			}
		}
		else
		{
			g.setTextureUnit(0);
			g.unbindTexture2D();
		}
	
	}

	/**
	 * Performs a blend mode break if necessary.
	 */
	protected void displayContextBlendBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.blendMode != n.nodeBlendMode)
		{
			displayVBOBreak(g);
			context.blendMode = n.nodeBlendMode;
	
			switch (context.blendMode)
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
		}
	}

	/**
	 * Draws the current object.
	 */
	protected void displayObject(OGLGraphics g, Node n)
	{
		float x = n.x;
		float y = n.y;
		float width = n.width;
		float height = n.height;
		
		if (g.supportsVertexBuffers())
		{
			if (vboContext == null)
				vboContext = new VBOContext(g);
			vboContext.addTileCoords(currentStepInstance, n);
		}
		else
		{
			g.setTextureUnit(0);
			g.matrixMode(MatrixType.TEXTURE); g.matrixPush();
			g.matrixTranslate(-currentStepInstance.pivot_s, -currentStepInstance.pivot_t, 0);
			g.matrixRotateZ(currentStepInstance.texture_rot);
			g.matrixTranslate(currentStepInstance.texture_s0-currentStepInstance.pivot_s, 
					currentStepInstance.texture_t0-currentStepInstance.pivot_t, 0);
			g.matrixScale(currentStepInstance.texture_s1-currentStepInstance.texture_s0, 
					currentStepInstance.texture_t1-currentStepInstance.texture_t0, 1);
		
			g.setColor(
					camera.getRed() * n.r * currentStepInstance.color_r,
					camera.getGreen() * n.g * currentStepInstance.color_g,
					camera.getBlue() * n.b * currentStepInstance.color_b,
					camera.getAlpha() * n.a * currentStepInstance.color_a
					);
			
			mesh.setTextureCoordinate(0, n.s0, n.t0);
			mesh.setTextureCoordinate(1, n.s0, n.t1);
			mesh.setTextureCoordinate(2, n.s1, n.t1);
			mesh.setTextureCoordinate(3, n.s1, n.t0);
			mesh.setVertex(0, x, y+height, depth);
			mesh.setVertex(1, x, y, depth);
			mesh.setVertex(2, x+width, y, depth);
			mesh.setVertex(3, x+width, y+height, depth);
			mesh.getView().drawUsing(g);
			
			g.setTextureUnit(0);
			g.matrixMode(MatrixType.TEXTURE); g.matrixPop();
		}
	}

	/**
	 * Creates the render list.
	 */
	protected void createRenderList(OGLGraphics g)
	{
		renderListSize = 0;
		timeBuildScene = System.nanoTime();
		long currentTime = g.currentTimeMillis();
	
		if (camera != null)
		{
			float endX = camera.getX() + camera.getWidth() + defaultTileWidth;
			float endY = camera.getY() + camera.getHeight() + defaultTileHeight;
			if (defaultTileWidth > 0f && defaultTileHeight > 0f) for (int ix = (int)(camera.getX()/defaultTileWidth)-1; ix*defaultTileWidth < endX; ix++)
				for (int iy = (int)(camera.getY()/defaultTileHeight)-1; iy*defaultTileHeight < endY; iy++)
				{
					if (!getVisible(ix, iy))
						continue;
					
					OGLSkin skin = getSkin(ix, iy);
					if (skin != null)
					{
						for (int p = 0; p < skin.size(); p++)
						{
							Step step = skin.get(p);
							Node n = getRenderNode(g, currentTime, p, step);
							setNodeCoords(n, ix, iy);
						}
					}
				}
		}
		timeBuildScene = System.nanoTime() - timeBuildScene;
		
		timeSortScene = System.nanoTime();
		renderList.sort(0, renderListSize);
		timeSortScene = System.nanoTime() - timeSortScene;
	}

	/**
	 * Sets the current surface step.
	 */
	protected void setStepInstance(OGLGraphics g, Step step)
	{
		if (currentStepInstance == null) 
			currentStepInstance = new StepInstance();
		long currentTime = g.currentTimeMillis();
		currentStepInstance.texture_rot = step.getTextureRotation(currentTime);
		currentStepInstance.texture_s0 = step.getTextureS0(currentTime);
		currentStepInstance.texture_t0 = step.getTextureT0(currentTime);
		currentStepInstance.texture_s1 = step.getTextureS1(currentTime);
		currentStepInstance.texture_t1 = step.getTextureT1(currentTime);
		currentStepInstance.color_r = step.getColorRed(currentTime);
		currentStepInstance.color_g = step.getColorGreen(currentTime);
		currentStepInstance.color_b = step.getColorBlue(currentTime);
		currentStepInstance.color_a = step.getColorAlpha(currentTime);
		currentStepInstance.pivot_s = step.getTextureRotationPivotS();
		currentStepInstance.pivot_t = step.getTextureRotationPivotT();
	}

	/**
	 * Gets if a tile is visible for a set of coordinates.
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 */
	protected boolean getVisible(int x, int y)
	{
		return tileModel != null ? tileModel.getVisible(x, y) : false; 
	}

	/**
	 * Gets the appropriate ARGB color integer for a set of coordinates.
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 */
	protected int getColorARGB(int x, int y)
	{
		return tileModel != null ? tileModel.getColorARGB(x, y) : 0; 
	}

	/**
	 * Gets the appropriate texture offsets for a set of coordinates. 
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 * @param out the output array, must be length 4 or greater. 
	 * Assumed order is S0, T0, S1, T1.
	 */
	protected void getTextureOffsets(int x, int y, float[] out)
	{
		if (tileModel != null)
			tileModel.getTextureOffsets(x, y, out);
	}

	/**
	 * Returns the x-offset of a tile in units.
	 */
	protected float getOffsetX(int x, int y)
	{
		return tileModel != null ? tileModel.getOffsetX(x, y) : null; 
	}

	/**
	 * Returns the y-offset of a tile in units.
	 */
	protected float getOffsetY(int x, int y)
	{
		return tileModel != null ? tileModel.getOffsetY(x, y) : null; 
	}

	/**
	 * Gets the individual tile scale, x-axis, for a specific tile.
	 */
	protected float getScaleX(int x, int y)
	{
		return tileModel != null ? tileModel.getScaleX(x, y) : null; 
	}

	/**
	 * Gets the individual tile scale, y-axis, for a specific tile.
	 */
	protected float getScaleY(int x, int y)
	{
		return tileModel != null ? tileModel.getScaleY(x, y) : null; 
	}

	private Node getRenderNode(OGLGraphics g, long graphicTime, int skinStepIndex, Step step)
	{
		Node n = null;
		if (renderList.size() == renderListSize)
		{
			n = new Node(g, loader, graphicTime, step, skinStepIndex);
			renderList.add(n);
		}
		else
		{
			n = renderList.getByIndex(renderListSize);
			n.set(g, loader, graphicTime, step, skinStepIndex);
		}
		
		return n;
	}

	private void setNodeCoords(Node n, int ix, int iy)
	{
		n.ix = ix;
		n.iy = iy;
		n.x = ix * defaultTileWidth - getOffsetX(ix, iy);
		n.y = iy * defaultTileHeight - getOffsetY(ix, iy);
		n.width = defaultTileWidth * getScaleX(ix, iy);
		n.height = defaultTileHeight * getScaleY(ix, iy);
		
		if (textureTemp == null)
			textureTemp = new float[4];
		
		getTextureOffsets(ix, iy, textureTemp);
		
		n.s0 = textureTemp[0];
		n.t0 = textureTemp[1];
		n.s1 = textureTemp[2];
		n.t1 = textureTemp[3];
		int c = getColorARGB(ix, iy);
		n.r = (((c >> 16) & 0x0ff) / 255f) * camera.getRed();
		n.g = (((c >> 8) & 0x0ff) / 255f) * camera.getGreen();
		n.b = (((c >> 0) & 0x0ff) / 255f) * camera.getBlue();
		n.a = (((c >> 24) & 0x0ff) / 255f) * camera.getAlpha();
		
		renderListSize++;
	}

	private void displayVBOBreak(OGLGraphics g)
	{
		if (currentStepInstance == null)
			return;
		
		if (vboContext != null)
			vboContext.flush(g, currentStepInstance);
	}

	/** 
	 * Sets the moused-over grid coordinates.
	 * Converts canvas-coordinate feedback to grid tile.
	 */
	protected void setMouseGridCoordinates()
	{
		int lastX = mouseGridX;
		int lastY = mouseGridY;
		
		canvasMouseDegreeX = (float)canvasMouseX / canvasWidth; 
		canvasMouseDegreeY = (float)canvasMouseY / canvasHeight; 
		
		mouseGridX = (int)((camera.getX() + (camera.getWidth() * canvasMouseDegreeX)) / defaultTileWidth); 
		mouseGridY = getFlipY() 
			? (int)((camera.getY() + (camera.getHeight() * canvasMouseDegreeY)) / defaultTileHeight)
			: (int)((camera.getY() + (camera.getHeight() * (1f - canvasMouseDegreeY))) / defaultTileHeight);
		
		if (lastX != mouseGridX || lastY != mouseGridY)
		{
			if (lastX >= 0 && lastY >= 0)
				fireMouseLeave(lastX, lastY);
			if (mouseGridX >= 0 && mouseGridY >= 0)
				fireMouseOver(mouseGridX, mouseGridY);
		}
	}

	/**
	 * Fires a mouse over event to all bound listeners.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	protected void fireMouseOver(int gridX, int gridY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridMouseOver(gridX, gridY);
	}

	/**
	 * Fires a mouse leave event to all bound listeners.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	protected void fireMouseLeave(int gridX, int gridY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridMouseLeave(gridX, gridY);
	}

	/**
	 * Fires a mouse press event to all bound listeners.
	 * @param button the java.awt.MouseEvent button constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	protected void fireMousePress(int button, int gridX, int gridY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridMousePress(button, gridX, gridY);
	}

	/**
	 * Fires a mouse release event to all bound listeners.
	 * @param button the java.awt.MouseEvent button constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	protected void fireMouseRelease(int button, int gridX, int gridY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridMouseRelease(button, gridX, gridY);
	}

	/**
	 * Fires a mouse drag event to all bound listeners.
	 * @param button the java.awt.MouseEvent button constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 * @param unitsX how many pixels of movement in the drag.
	 * @param unitsY how many pixels of movement in the drag.
	 */
	protected void fireMouseDrag(int button, int gridX, int gridY, 
			float unitsX, float positionX, float unitsY, float positionY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridMouseDrag(button, gridX, gridY, unitsX, positionX, unitsY, positionY);
	}

	/**
	 * Fires a mouse click event to all bound listeners.
	 * @param button the java.awt.MouseEvent button constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	protected void fireMouseClick(int button, int gridX, int gridY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridMouseClick(button, gridX, gridY);
	}

	/**
	 * Fires a mouse wheel event to all bound listeners.
	 * @param units the amount of units that the wheel was scrolled - can be negative.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	protected void fireMouseWheel(int units, int gridX, int gridY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridMouseWheel(units, gridX, gridY);
	}

	/**
	 * Fires a key press event to all bound listeners.
	 * @param keycode the java.awt.KeyEvent VK constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	protected void fireKeyPress(int keycode, int gridX, int gridY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridKeyPress(keycode, gridX, gridY);
	}

	/**
	 * Fires a key release event to all bound listeners.
	 * @param keycode the java.awt.KeyEvent VK constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	protected void fireKeyRelease(int keycode, int gridX, int gridY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridKeyRelease(keycode, gridX, gridY);
	}

	/**
	 * Fires a key typed event to all bound listeners.
	 * @param keycode the java.awt.KeyEvent VK constant.
	 * @param gridX the grid coordinate that this happened on, x-axis.
	 * @param gridY the grid coordinate that this happened on, y-axis.
	 */
	protected void fireKeyTyped(int keycode, int gridX, int gridY)
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridKeyTyped(keycode, gridX, gridY);
	}

	/**
	 * Fires a focus event to all bound listeners.
	 */
	protected void fireFocus()
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridFocus();
	}

	/**
	 * Fires an unfocus event to all bound listeners.
	 */
	protected void fireUnfocus()
	{
		for (int i = 0; i < listenerList.size(); i++)
			listenerList.getByIndex(i).gridUnfocus();
	}

	/**
	 * Holds a renderer node.
	 */
	protected static class Node implements Comparable<Node>
	{
		/** The reference to the surface step that is used to create this node. */
		public Step nodeStepRef;
	
		/** The texture object to use. */
		public OGLTexture2D nodeTexture;
		/** Multiple texture unit combiner. */
		public OGLTexture2D[] nodeMultiTexture;
		/** The shader program object to use. */
		public OGLShaderProgram nodeShader;
	
		/** The pass target index/type for segmented rendering.*/
		public int nodeTarget;
		/** The pass number for rendering ordering. */
		public int nodePass;
		/** 
		 * The blending type for this node. 
		 * Corresponds to OGLRenderStep's BLEND constants. 
		 */
		public BlendType nodeBlendMode;
		/** Texture S-axis gentype. */
		public int nodeSTexGen;
		/** Texture T-axis gentype. */
		public int nodeTTexGen;

		public int ix;
		public int iy;
		
		public float x;
		public float y;
		public float width;
		public float height;
		public float s0;
		public float t0;
		public float s1;
		public float t1;
		public float r;
		public float g;
		public float b;
		public float a;

		/**
		 * Creates a new render node.
		 */
		public Node(OGLGraphics g, OGLResourceLoader loader, 
				long currentTime, Step step, int pass)
		{
			set(g, loader, currentTime, step, pass);
		}
		
		/**
		 * Creates a new render node.
		 */
		public void set(OGLGraphics g, OGLResourceLoader loader, 
				long currentTime, Step step, int pass)
		{
			nodeTarget = 0;
			nodePass = pass;
			nodeStepRef = step;
			nodeBlendMode = step.getBlendType();
			nodeSTexGen = step.getTexGenS();
			nodeTTexGen = step.getTexGenT();
	
			OGLShaderResource oglsr = step.getShaderProgram();
			if (oglsr != null)
			{
				if (!loader.containsShader(oglsr))
					loader.cacheShader(g, oglsr);
				nodeShader = loader.getShader(oglsr);
			}
			else
				nodeShader = null;
			
			if (!step.isMultitexture())
			{
				int ti = step.getTextureIndex(currentTime);
				if (ti >= 0)
				{
					OGLTextureResource ogltr = step.getTextureList()[ti];
					if (!loader.containsTexture(ogltr))
						loader.cacheTexture(g, ogltr);
					nodeTexture = loader.getTexture(ogltr);
				}
				else
					nodeTexture = null;
			}
			else 
			{
				nodeMultiTexture = new OGLTexture2D[step.getTextureList().length];
				int i = 0;
				OGLTextureResource[] trlist = step.getTextureList();
				for (int x = 0; x < trlist.length; x++)
				{
					OGLTextureResource ogltr = trlist[x];
					if (!loader.containsTexture(ogltr))
						loader.cacheTexture(g, ogltr);
					nodeMultiTexture[i++] = loader.getTexture(ogltr);
				}
			}
		}
		
		@Override
		public int compareTo(Node n)
		{
			return  
				nodeTarget == n.nodeTarget ?
				nodePass == n.nodePass ?
				getShaderId(nodeShader) == getShaderId(n.nodeShader) ?
				getTexId(nodeTexture) == getTexId(n.nodeTexture) ?
				nodeBlendMode == n.nodeBlendMode ?
				nodeSTexGen == n.nodeSTexGen ?
				nodeTTexGen == n.nodeTTexGen ?
					0 :
				nodeTTexGen - n.nodeTTexGen :
				nodeSTexGen - n.nodeSTexGen :
				nodeBlendMode.ordinal() - n.nodeBlendMode.ordinal() : 
				getTexId(nodeTexture) - getTexId(n.nodeTexture) :
				getShaderId(nodeShader) - getShaderId(n.nodeShader) :
				nodePass - n.nodePass :
				nodeTarget - n.nodeTarget;
		}
	
		// get the texture id of a texture object used for sorting.
		protected int getTexId(OGLTexture2D texture)
		{
			return texture == null ? 0 : texture.getGLId();
		}
	
		// get the shader id of a shader object used for sorting.
		protected int getShaderId(OGLShaderProgram shader)
		{
			return shader == null ? 0 : shader.getGLId();
		}
	}
	
	/** Vertex Buffer rendering context. */
	protected class VBOContext
	{
		/** Geometry Float Buffer */
		protected OGLFloatBuffer geometryBuffer;
		
		/** Has this already been flushed? */
		protected boolean vboFlush;
		
		/** Number of VBO Elements to draw. */
		protected int vboElements;

		/** Interleaved Geometry Info Buffer. */
		protected FloatBuffer geometryFloatBuffer;
		/** Interleaved Geometry Info Buffer Index */
		protected int geometryListIndex;
	
		public VBOContext(OGLGraphics g)
		{
			vboFlush = true;
			geometryBuffer = new OGLFloatBuffer(g, BufferType.GEOMETRY);
		}
		
		/**
		 * Draws and resets the buffer.
		 */
		public void flush(OGLGraphics g, StepInstance inst)
		{
			if (vboFlush)
				return;
			
			g.setTextureUnit(0);
			g.matrixMode(MatrixType.TEXTURE); 
			g.matrixPush();
			g.matrixTranslate(inst.pivot_s, inst.pivot_t, 0);
			g.matrixRotateZ(inst.texture_rot);
			g.matrixTranslate(-inst.pivot_s, -inst.pivot_t, 0);
			g.matrixTranslate(inst.texture_s0, inst.texture_t0, 0);
			g.matrixScale(inst.texture_s1-inst.texture_s0, 
					inst.texture_t1-inst.texture_t0, 1);
		
			geometryBuffer.setCapacity(g, CachingHint.STREAM_DRAW, geometryListIndex);
			geometryBuffer.sendSubData(g, geometryFloatBuffer, geometryListIndex, 0);
			
			OGLGeometryUtils.drawInterleavedGeometry(g, geometryBuffer, GeometryType.QUADS, vboElements * 4, geometryInfo);
			
			polygonsRendered += vboElements;
			
			geometryFloatBuffer.rewind();
			geometryListIndex = 0;
			vboElements = 0;

			g.setTextureUnit(0);
			g.matrixMode(MatrixType.TEXTURE); g.matrixPop();
			vboFlush = true;
		}
		
		/**
		 * Adds a set of coordinates to the buffer.
		 * @param inst the step instance to use for color information.
		 * @param node the node being drawn.
		 */
		public void addTileCoords(StepInstance inst, Node node)
		{
			int vertThreshold = ((int)(camera.getWidth()/defaultTileWidth)+1) * ((int)(camera.getHeight()/defaultTileHeight)+1) * 360;
			if (geometryFloatBuffer == null || vertThreshold > geometryFloatBuffer.capacity())
				geometryFloatBuffer = Common.allocDirectFloatBuffer(vertThreshold);
			
			float red = node.r * inst.color_r;
			float green = node.g * inst.color_g;
			float blue = node.b * inst.color_b;
			float alpha = node.a * inst.color_a;
			
			int n = geometryListIndex;

			geometryFloatBuffer.put(n+0, node.x);
			geometryFloatBuffer.put(n+1, node.y + node.height);
			geometryFloatBuffer.put(n+2, depth);
			geometryFloatBuffer.put(n+3, node.s0);
			geometryFloatBuffer.put(n+4, getFlipY() ? node.t1 : node.t0);
			geometryFloatBuffer.put(n+5, red);
			geometryFloatBuffer.put(n+6, green);
			geometryFloatBuffer.put(n+7, blue);
			geometryFloatBuffer.put(n+8, alpha);
			// vertex 2
			geometryFloatBuffer.put(n+9, node.x);
			geometryFloatBuffer.put(n+10, node.y);
			geometryFloatBuffer.put(n+11, depth);
			geometryFloatBuffer.put(n+12, node.s0);
			geometryFloatBuffer.put(n+13, getFlipY() ? node.t0 : node.t1);
			geometryFloatBuffer.put(n+14, red);
			geometryFloatBuffer.put(n+15, green);
			geometryFloatBuffer.put(n+16, blue);
			geometryFloatBuffer.put(n+17, alpha);
			// vertex 3
			geometryFloatBuffer.put(n+18, node.x + node.width);
			geometryFloatBuffer.put(n+19, node.y);
			geometryFloatBuffer.put(n+20, depth);
			geometryFloatBuffer.put(n+21, node.s1);
			geometryFloatBuffer.put(n+22, getFlipY() ? node.t0 : node.t1);
			geometryFloatBuffer.put(n+23, red);
			geometryFloatBuffer.put(n+24, green);
			geometryFloatBuffer.put(n+25, blue);
			geometryFloatBuffer.put(n+26, alpha);
			// vertex 4
			geometryFloatBuffer.put(n+27, node.x + node.width);
			geometryFloatBuffer.put(n+28, node.y + node.height);
			geometryFloatBuffer.put(n+29, depth);
			geometryFloatBuffer.put(n+30, node.s1);
			geometryFloatBuffer.put(n+31, getFlipY() ? node.t1 : node.t0);
			geometryFloatBuffer.put(n+32, red);
			geometryFloatBuffer.put(n+33, green);
			geometryFloatBuffer.put(n+34, blue);
			geometryFloatBuffer.put(n+35, alpha);

			geometryListIndex += 36;
			
			vboFlush = false;
			vboElements++;
		}
		
	}

	/**
	 * Holds calculated step information - lifetime of current info
	 * is the current frame. Used for reusing data that would be calculated
	 * many times.
	 */
	protected static class StepInstance
	{
		public float texture_rot;
		public float texture_s0;
		public float texture_t0;
		public float texture_s1;
		public float texture_t1;
		public float color_r;
		public float color_g;
		public float color_b;
		public float color_a;
		public float pivot_s;
		public float pivot_t;
	}

	/** Context for object switching. */
	protected static class Context
	{
		public Step step;
		public BlendType blendMode;
		public OGLTexture2D texture;
		public OGLTexture2D[] multitexture;
		public OGLShaderProgram shader;
		public int pass;
		
		Context()
		{
			clear();
		}
		
		void clear()
		{
			step = null;
			blendMode = null;
			texture = null;
			multitexture = null;
			shader = null;
			pass = 0;
		}
	}
	
}
