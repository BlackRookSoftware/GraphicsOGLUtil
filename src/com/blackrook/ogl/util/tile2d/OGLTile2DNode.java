/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.tile2d;

import com.blackrook.commons.list.List;
import com.blackrook.ogl.OGLGeometryUtils;
import com.blackrook.ogl.OGLGraphics;
import com.blackrook.ogl.OGLMesh;
import com.blackrook.ogl.OGLGeometryUtils.GeometryInfo;
import com.blackrook.ogl.data.OGLColor;
import com.blackrook.ogl.enums.AttribType;
import com.blackrook.ogl.enums.FaceSide;
import com.blackrook.ogl.enums.GeometryType;
import com.blackrook.ogl.enums.MatrixType;
import com.blackrook.ogl.mesh.MeshView;
import com.blackrook.ogl.mesh.PolygonMesh;
import com.blackrook.ogl.node.OGLCanvasNodeAdapter;
import com.blackrook.ogl.util.OGL2DCamera;
import com.blackrook.ogl.util.OGLDrawContext;
import com.blackrook.ogl.util.OGLResourceLoader;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * An assisting class for drawing a tile-based whatever.
 * NOTE: This does not pay attention to object rotation from render steps. 
 * @author Matthew Tropiano
 */
public class OGLTile2DNode extends OGLCanvasNodeAdapter
{
	/** Rectangular mesh used by all tiles. */
	protected static final OGLMesh TILE = new PolygonMesh(GeometryType.QUADS, 4, 1)
	{{
		setVertex(0, 0, 1, 0);
		setTextureCoordinate(0, 0, 1);
		setVertex(1, 0, 0, 0);
		setTextureCoordinate(1, 0, 0);
		setVertex(2, 1, 0, 0);
		setTextureCoordinate(2, 1, 0);
		setVertex(3, 1, 1, 0);
		setTextureCoordinate(3, 1, 1);
	}};

	/** View for tile. */
	protected static final MeshView TILE_VIEW = TILE.getView();

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
	
	/** Context for rendering.  */
	protected OGLDrawContext context;
	/** Color storage. */
	protected OGLColor color;
	/** Temp texture resources. */
	protected OGLTextureResource[] textureRes;
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
		listenerList = new List<OGLTile2DListener>(2);
		tileModel = model;
		mouseGridX = -1;
		mouseGridY = -1;
		this.loader = loader;
		this.textureRes = new OGLTextureResource[16];
		this.color = new OGLColor();
		
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
			AttribType.SCISSOR
		);
		
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
				-1, 1
			);
		else
			g.matrixOrtho(
				(float)(camera.getObjectCenterX() - camera.getObjectHalfWidth()), 
				(float)(camera.getObjectCenterX() + camera.getObjectHalfWidth()), 
				(float)(camera.getObjectCenterY() - camera.getObjectHalfHeight()), 
				(float)(camera.getObjectCenterY() + camera.getObjectHalfHeight()), 
				-1, 1
			);

		timeRenderScene = System.nanoTime();

		g.setTexture2DEnabled(true);
		
		if (context == null)
			context = new OGLDrawContext();
		else
			context.reset(g, loader);

		// draw shit here.
		drawTiles(g);
	
		timeRenderScene = System.nanoTime() - timeRenderScene;

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
	 * @param outTextures the output array for the textures.
	 * @return the amount of textures to bind.
	 */
	public int getTextures(int x, int y, OGLTextureResource[] outTextures)
	{
		return tileModel != null ? tileModel.getTextures(x, y, outTextures) : 0; 
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
	 * Draws the tiles (lazily for now - no sort).
	 */
	protected void drawTiles(OGLGraphics g)
	{
		timeBuildScene = 0L;
		timeSortScene = 0L;
	
		g.matrixMode(MatrixType.MODELVIEW);
		
		if (camera != null)
		{
			float endX = camera.getX() + camera.getWidth() + defaultTileWidth;
			float endY = camera.getY() + camera.getHeight() + defaultTileHeight;
			if (defaultTileWidth > 0f && defaultTileHeight > 0f) for (int ix = (int)(camera.getX()/defaultTileWidth)-1; ix*defaultTileWidth < endX; ix++)
				for (int iy = (int)(camera.getY()/defaultTileHeight)-1; iy*defaultTileHeight < endY; iy++)
				{
					if (!getVisible(ix, iy))
						continue;
					
					context.setBlending(g, tileModel.getBlendingFunction(ix, iy));
					int units = tileModel.getTextures(ix, iy, textureRes);
					context.setTextureEnvironment(g, tileModel.getTextureMode(ix, iy));
					context.setTextures(g, loader, textureRes, units);
					context.setShader(g, loader, tileModel.getShader(ix, iy));
					
					tileModel.getColor(ix, iy, color);
					g.setColor(color);
					
					g.matrixPush();
					g.matrixTranslate(ix * defaultTileWidth, iy * defaultTileHeight, depth);
					g.matrixScale(defaultTileWidth, defaultTileHeight, 1f);
					TILE_VIEW.drawUsing(g);
					g.matrixPop();
				}
		}
		
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
	protected void getColor(int x, int y, OGLColor out)
	{
		if (tileModel != null)
			tileModel.getColor(x, y, out);
		else
			out.set(0);
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
	
}
