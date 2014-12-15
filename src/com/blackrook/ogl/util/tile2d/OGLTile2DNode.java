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
import com.blackrook.ogl.OGLCanvasNode;
import com.blackrook.ogl.OGLGeometryUtils;
import com.blackrook.ogl.OGLGraphics;
import com.blackrook.ogl.OGLGeometryUtils.GeometryInfo;
import com.blackrook.ogl.data.OGLColor;
import com.blackrook.ogl.enums.AttribType;
import com.blackrook.ogl.enums.BlendFunc;
import com.blackrook.ogl.enums.BufferType;
import com.blackrook.ogl.enums.CachingHint;
import com.blackrook.ogl.enums.FaceSide;
import com.blackrook.ogl.enums.GeometryType;
import com.blackrook.ogl.enums.MatrixType;
import com.blackrook.ogl.mesh.PolygonMesh;
import com.blackrook.ogl.object.buffer.OGLFloatBuffer;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture2D;
import com.blackrook.ogl.util.OGL2DCamera;
import com.blackrook.ogl.util.OGLResourceLoader;
import com.blackrook.ogl.util.resource.OGLShaderResource;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * An assisting class for drawing a tile-based whatever.
 * NOTE: This does not pay attention to object rotation from render steps. 
 * @author Matthew Tropiano
 */
public class OGLTile2DNode implements OGLCanvasNode
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
	/** Color for color changes. */
	protected OGLColor colorContext;
	/** Context for rendering.  */
	protected Context context;
	/** VBO Context. */
	protected VBOContext vboContext;
	/** Mesh for non-VBO. */
	protected PolygonMesh mesh;
	/** Temp texture resources. */
	protected OGLTextureResource[] textureTemp;
	/** Temp texture coordinates. */
	protected float[] textureCoordTemp;
	
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
		this.loader = loader;
		renderList = new List<Node>(200);
		listenerList = new List<OGLTile2DListener>(2);
		colorContext = new OGLColor();
		textureTemp = new OGLTextureResource[16];
		textureCoordTemp = new float[4];
		mesh = new PolygonMesh(GeometryType.QUADS, 4, 1);
		mouseGridX = -1;
		mouseGridY = -1;
		tileModel = model;

		setCamera(camera);
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
		g.setDepthTestEnabled(false);
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
				(float)(camera.getCenterX() - camera.getHalfWidth()), 
				(float)(camera.getCenterX() + camera.getHalfWidth()), 
				(float)(camera.getCenterY() + camera.getHalfHeight()), 
				(float)(camera.getCenterY() - camera.getHalfHeight()), 
				-1, 1);
		else
			g.matrixOrtho(
				(float)(camera.getCenterX() - camera.getHalfWidth()), 
				(float)(camera.getCenterX() + camera.getHalfWidth()), 
				(float)(camera.getCenterY() - camera.getHalfHeight()), 
				(float)(camera.getCenterY() + camera.getHalfHeight()), 
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
			if (context.textures != null) for (int i = 0; i < context.textures.length; i++)
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
	 * Performs a shader break if necessary.
	 */
	protected void displayContextShaderBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.shader != n.shader)
		{
			displayVBOBreak(g);
			context.shader = n.shader;
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
		if (!getTextureEquality(context.textures, n.textures))
		{
			int unitMax = Math.max(context.texturesCount, n.textureCount);
			
			displayVBOBreak(g);
			
			int i = 0;
			for (; i < n.textureCount; i++)
			{
				context.textures[i] = n.textures[i];
				g.setTextureUnit(i);
				context.textures[i].bindTo(g);
			}
			
			for (; i < unitMax; i++)
			{
				g.setTextureUnit(i);
				g.unbindTexture2D();
			}
		}
	}

	/**
	 * Performs a blend mode break if necessary.
	 */
	protected void displayContextBlendBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.blendMode != n.blendMode)
		{
			displayVBOBreak(g);
			context.blendMode = n.blendMode;
			g.setBlendingFunc(BlendFunc.REPLACE);
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
			vboContext.addTileCoords(n);
		}
		else
		{
			g.setColor(n.r,	n.g, n.b, n.a);
			
			mesh.setTextureCoordinate(0, n.s0, n.t0);
			mesh.setTextureCoordinate(1, n.s0, n.t1);
			mesh.setTextureCoordinate(2, n.s1, n.t1);
			mesh.setTextureCoordinate(3, n.s1, n.t0);
			mesh.setVertex(0, x, y+height, 0f);
			mesh.setVertex(1, x, y, 0f);
			mesh.setVertex(2, x+width, y, 0f);
			mesh.setVertex(3, x+width, y+height, 0f);
			mesh.getView().drawUsing(g);
		}
	}

	/**
	 * Creates the render list.
	 */
	protected void createRenderList(OGLGraphics g)
	{
		renderListSize = 0;
		
		timeBuildScene = System.nanoTime();

		if (camera != null)
		{
			float endX = (camera.getCenterX() - camera.getHalfWidth()) + (camera.getHalfWidth() * 2) + defaultTileWidth;
			float endY = (camera.getCenterY() - camera.getHalfHeight()) + (camera.getHalfHeight() * 2) + defaultTileHeight;
			if (defaultTileWidth > 0f && defaultTileHeight > 0f) for (int ix = (int)((camera.getCenterX() - camera.getHalfWidth())/defaultTileWidth)-1; ix*defaultTileWidth < endX; ix++)
				for (int iy = (int)((camera.getCenterY() - camera.getHalfHeight())/defaultTileHeight)-1; iy*defaultTileHeight < endY; iy++)
				{
					if (!getVisible(ix, iy))
						continue;
					
					Node n = getRenderNode(g);
					setNodeCoords(n, ix, iy);
				}
		}
		timeBuildScene = System.nanoTime() - timeBuildScene;
		
		timeSortScene = System.nanoTime();
		renderList.sort(0, renderListSize);
		timeSortScene = System.nanoTime() - timeSortScene;
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
	 * Gets the appropriate color for a set of coordinates.
	 * @param x	the grid x-coordinate.
	 * @param y the grid y-coordinate.
	 */
	protected void getColor(int x, int y, OGLColor color)
	{
		if (tileModel != null) 
			tileModel.getColor(x, y, color); 
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

	private Node getRenderNode(OGLGraphics g)
	{
		Node n = null;
		if (renderList.size() == renderListSize)
		{
			n = new Node();
			renderList.add(n);
		}
		else
		{
			n = renderList.getByIndex(renderListSize);
		}

		return n;
	}

	private void setNodeCoords(Node n, int ix, int iy)
	{
		n.ix = ix;
		n.iy = iy;
		
		n.x = ix * defaultTileWidth;
		n.y = iy * defaultTileHeight;
		n.width = defaultTileWidth;
		n.height = defaultTileHeight;
		
		getTextureOffsets(ix, iy, textureCoordTemp);
		n.s0 = textureCoordTemp[0];
		n.t0 = textureCoordTemp[1];
		n.s1 = textureCoordTemp[2];
		n.t1 = textureCoordTemp[3];
		
		tileModel.getColor(ix, iy, colorContext);
		n.r = colorContext.getRed();
		n.g = colorContext.getGreen();
		n.b = colorContext.getBlue();
		n.a = colorContext.getAlpha();
		
		OGLShaderResource shadres = tileModel.getShader(ix, iy);
		if (shadres != null)
			n.shader = loader.getShader(shadres);
		else
			n.shader = null;
		
		int units = tileModel.getTextures(ix, iy, textureTemp);
		for (int i = 0; i < units; i++)
		{
			if (textureTemp[i] != null)
				n.textures[i] = loader.getTexture(textureTemp[i]);
			else
				n.textures[i] = null;
			n.textureCount = units;
		}
		
		renderListSize++;
	}

	private void displayVBOBreak(OGLGraphics g)
	{
		if (vboContext != null)
			vboContext.flush(g);
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
		
		mouseGridX = (int)(((camera.getCenterX() - camera.getHalfWidth()) + ((camera.getHalfWidth() * 2) * canvasMouseDegreeX)) / defaultTileWidth); 
		mouseGridY = getFlipY() 
			? (int)(((camera.getCenterY() - camera.getHalfHeight()) + ((camera.getHalfHeight() * 2) * canvasMouseDegreeY)) / defaultTileHeight)
			: (int)(((camera.getCenterY() - camera.getHalfHeight()) + ((camera.getHalfHeight() * 2) * (1f - canvasMouseDegreeY))) / defaultTileHeight);
		
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
	 * Get the texture id of a texture object used for sorting.
	 */
	protected boolean getTextureEquality(OGLTexture2D[] texturesA, OGLTexture2D[] texturesB)
	{
		if (texturesA == null && texturesB == null)
			return true;
		
		if (texturesA != null ^ texturesB != null)
			return false;

		if (texturesA.length != texturesB.length)
			return false;
		
		for (int i = 0; i < texturesA.length; i++)
		{
			int ta = texturesA[i] != null ? texturesA[i].getGLId() : 0;
			int tb = texturesB[i] != null ? texturesB[i].getGLId() : 0;
			if (ta != tb)
				return false;
		}
		
		return true;
	}

	/**
	 * Holds a renderer node.
	 */
	protected class Node implements Comparable<Node>
	{
		/** The shader program object to use. */
		public OGLShaderProgram shader;
		/** The texture object to use. */
		public OGLTexture2D[] textures;
		public int textureCount;
		/** 
		 * The blending type for this node. 
		 * Corresponds to OGLRenderStep's BLEND constants. 
		 */
		public BlendFunc blendMode;
	
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
	
		Node()
		{
			textures = new OGLTexture2D[16];
			textureCount = 0;
		}
		
		@Override
		public int compareTo(Node n)
		{
			if (blendMode != n.blendMode)
				return blendMode.ordinal() - n.blendMode.ordinal();

			int shadId = shader == null ? 0 : shader.getGLId();
			int nshadId = n.shader == null ? 0 : n.shader.getGLId();
			
			if (shadId != nshadId)
				return shadId - nshadId;
			
			if (!getTextureEquality(textures, n.textures))
				return -1;
			else
				return 0;
		}
	
	}

	/** Context for object switching. */
	protected class Context
	{
		public BlendFunc blendMode;
		public OGLShaderProgram shader;
		public OGLTexture2D[] textures;
		public int texturesCount;
		
		Context()
		{
			clear();
		}
		
		void clear()
		{
			blendMode = null;
			shader = null;
			textures = new OGLTexture2D[16];
			texturesCount = 0;
		}
	}

	/** Vertex Buffer rendering context. */
	protected class VBOContext
	{
		/** Geometry Float Buffer */
		protected OGLFloatBuffer geometryBufferA;
		/** Geometry Float Buffer */
		protected OGLFloatBuffer geometryBufferB;
		/** Current Geometry Float Buffer */
		protected OGLFloatBuffer currentGeometryBuffer;
		
		
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
			geometryBufferA = new OGLFloatBuffer(g, BufferType.GEOMETRY);
			geometryBufferB = new OGLFloatBuffer(g, BufferType.GEOMETRY);
			currentGeometryBuffer = geometryBufferA;
		}
		
		/**
		 * Draws and resets the buffer.
		 */
		public void flush(OGLGraphics g)
		{
			if (vboFlush)
				return;
			
			currentGeometryBuffer.setCapacity(g, CachingHint.STATIC_DRAW, geometryListIndex);
			currentGeometryBuffer.sendSubData(g, geometryFloatBuffer, geometryListIndex, 0);
			
			OGLGeometryUtils.drawInterleavedGeometry(g, currentGeometryBuffer, GeometryType.QUADS, vboElements * 4, geometryInfo);
			currentGeometryBuffer = currentGeometryBuffer == geometryBufferA ? geometryBufferB : geometryBufferA;
			
			polygonsRendered += vboElements;
			
			geometryFloatBuffer.rewind();
			geometryListIndex = 0;
			vboElements = 0;
			vboFlush = true;
		}
		
		/**
		 * Adds a set of coordinates to the buffer.
		 * @param node the node being drawn.
		 */
		public void addTileCoords(Node node)
		{
			int vertThreshold = ((int)(camera.getHalfWidth() * 2 / defaultTileWidth) + 1) * ((int)(camera.getHalfHeight() * 2 / defaultTileHeight) + 1) * 360;
			if (geometryFloatBuffer == null || vertThreshold > geometryFloatBuffer.capacity())
				geometryFloatBuffer = Common.allocDirectFloatBuffer(vertThreshold);
			
			int n = geometryListIndex;

			geometryFloatBuffer.put(n+0, node.x);
			geometryFloatBuffer.put(n+1, node.y + node.height);
			geometryFloatBuffer.put(n+2, 0f);
			geometryFloatBuffer.put(n+3, node.s0);
			geometryFloatBuffer.put(n+4, getFlipY() ? node.t1 : node.t0);
			geometryFloatBuffer.put(n+5, node.r);
			geometryFloatBuffer.put(n+6, node.g);
			geometryFloatBuffer.put(n+7, node.b);
			geometryFloatBuffer.put(n+8, node.a);
			// vertex 2
			geometryFloatBuffer.put(n+9, node.x);
			geometryFloatBuffer.put(n+10, node.y);
			geometryFloatBuffer.put(n+11, 0f);
			geometryFloatBuffer.put(n+12, node.s0);
			geometryFloatBuffer.put(n+13, getFlipY() ? node.t0 : node.t1);
			geometryFloatBuffer.put(n+14, node.r);
			geometryFloatBuffer.put(n+15, node.g);
			geometryFloatBuffer.put(n+16, node.b);
			geometryFloatBuffer.put(n+17, node.a);
			// vertex 3
			geometryFloatBuffer.put(n+18, node.x + node.width);
			geometryFloatBuffer.put(n+19, node.y);
			geometryFloatBuffer.put(n+20, 0f);
			geometryFloatBuffer.put(n+21, node.s1);
			geometryFloatBuffer.put(n+22, getFlipY() ? node.t0 : node.t1);
			geometryFloatBuffer.put(n+23, node.r);
			geometryFloatBuffer.put(n+24, node.g);
			geometryFloatBuffer.put(n+25, node.b);
			geometryFloatBuffer.put(n+26, node.a);
			// vertex 4
			geometryFloatBuffer.put(n+27, node.x + node.width);
			geometryFloatBuffer.put(n+28, node.y + node.height);
			geometryFloatBuffer.put(n+29, 0f);
			geometryFloatBuffer.put(n+30, node.s1);
			geometryFloatBuffer.put(n+31, getFlipY() ? node.t1 : node.t0);
			geometryFloatBuffer.put(n+32, node.r);
			geometryFloatBuffer.put(n+33, node.g);
			geometryFloatBuffer.put(n+34, node.b);
			geometryFloatBuffer.put(n+35, node.a);

			geometryListIndex += 36;
			
			vboFlush = false;
			vboElements++;
		}
		
	}
	
}
