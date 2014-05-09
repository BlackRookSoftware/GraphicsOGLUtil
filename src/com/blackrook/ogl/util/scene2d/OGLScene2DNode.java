/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.scene2d;

import java.nio.FloatBuffer;
import java.util.Arrays;

import com.blackrook.commons.Common;
import com.blackrook.commons.ResettableIterator;
import com.blackrook.commons.hash.Hash;
import com.blackrook.commons.list.List;
import com.blackrook.commons.math.RMath;
import com.blackrook.commons.math.geometry.Point2F;
import com.blackrook.commons.spatialhash.SpatialHashable;
import com.blackrook.ogl.OGLGeometryUtils;
import com.blackrook.ogl.OGLGeometryUtils.GeometryInfo;
import com.blackrook.ogl.OGLGraphics;
import com.blackrook.ogl.enums.AttribType;
import com.blackrook.ogl.enums.BlendFunc;
import com.blackrook.ogl.enums.BufferType;
import com.blackrook.ogl.enums.CachingHint;
import com.blackrook.ogl.enums.FaceSide;
import com.blackrook.ogl.enums.GeometryType;
import com.blackrook.ogl.enums.MatrixType;
import com.blackrook.ogl.enums.TextureCoordType;
import com.blackrook.ogl.enums.TextureGenMode;
import com.blackrook.ogl.mesh.MeshView;
import com.blackrook.ogl.object.buffer.OGLFloatBuffer;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture2D;
import com.blackrook.ogl.util.OGL2DCamera;
import com.blackrook.ogl.util.OGL2DCameraListener;
import com.blackrook.ogl.util.OGLSkin;
import com.blackrook.ogl.util.OGLSkin.BlendType;
import com.blackrook.ogl.util.OGLSkin.Step;
import com.blackrook.ogl.util.OGLResourceLoader;
import com.blackrook.ogl.util.OGLResourceLoaderUser;
import com.blackrook.ogl.util.resource.OGLShaderResource;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * OGL Node that does 2D scene rendering via a multipass method.
 * @author Matthew Tropiano
 */
public class OGLScene2DNode<T extends OGLScene2DElement> implements OGLResourceLoaderUser
{
	protected static final Step DEFAULT_STEP = new Step();
	
	/** Is this layer enabled? */
	private boolean enabled;

	/* ===================================================== */

	/** Reference to Resource loader. */
	protected OGLResourceLoader loader;

	/** Camera instance. */
	protected OGL2DCamera camera;
	
	/** Camera changed bit. */
	protected boolean cameraChanged;
	/** Camera Change X */
	protected float cameraChangeX;
	/** Camera Change Y */
	protected float cameraChangeY;
	/** Camera Change Width */
	protected float cameraChangeWidth;
	/** Camera Change Height */
	protected float cameraChangeHeight;
	/** Camera listener. */
	protected OGL2DCameraListener cameraListener = new OGL2DCameraListener()
	{
		@Override
		public void onCameraChange(float changeX, float changeY, float changeWidth, float changeHeight)
		{
			cameraChanged = true;
			cameraChangeX = changeX;
			cameraChangeY = changeY;
			cameraChangeWidth = changeWidth;
			cameraChangeHeight = changeHeight;
		}
	};

	/** Flip Y? */
	protected boolean flipY;
	
	/** Scene objects. */
	protected Hash<T> sceneObjects;
	/** Scene object iterator. */
	protected ResettableIterator<T> sceneObjectIterator;
	/** Count on screen. */
	protected int countOnCamera;
	/** Count off screen. */
	protected int countOffCamera;
	
	/** Canvas width. */
	protected int canvasWidth;
	/** Canvas height. */
	protected int canvasHeight;
	
	/** The list of what to render. */
	protected List<Node> renderList;
	/** Render list end index. */
	protected int renderListSize;
	/** Render list object count. */
	protected int renderListObjects;
	/** Sorting context. */
	protected Context context;
	/** VBO Object */
	protected VBOContext vertexBuffer;

	/** Camera-relative mouse position. */
	protected Point2F mousePoint;
	/** Last mouse position seen, X coordinate. */
	protected int canvasMouseX;
	/** Last mouse position seen, Y coordinate. */
	protected int canvasMouseY;
	/** Last mouse position seen, X. 0 is farthest left, 1 is farthest right. */
	protected float canvasMouseDegreeX;
	/** Last mouse position seen, Y. 0 is farthest up, 1 is farthest down. */
	protected float canvasMouseDegreeY;

	/** Time to build scene in nanoseconds. */
	protected long timeBuildScene;
	/** Time to sort scene in nanoseconds. */
	protected long timeSortScene;
	/** Time to render scene in nanoseconds. */
	protected long timeRenderScene;
	/** Total render time in nanoseconds. */
	protected long renderTimeNanos;
	/** Polygons rendered after render pass. */
	protected int polygonsRendered;

	protected GeometryInfo[] geometryInfo = new GeometryInfo[]
	{
		OGLGeometryUtils.vertices(3, 9, 0),
		OGLGeometryUtils.texCoords(0, 2, 9, 3),
		OGLGeometryUtils.color(4, 9, 5)
	};
	
	/**
	 * Creates a new OGLScene2D instance to be bound to a graphics system.
	 * Requires a resource loader.
	 */
	public OGLScene2DNode(OGLResourceLoader loader)
	{
		this(loader, new OGL2DCamera());
	}
	
	/**
	 * Creates a new OGLScene2D instance to be bound to a graphics system.
	 * Requires a resource loader and camera instance.
	 * This scene's camera listener is automatically added to the camera.
	 */
	public OGLScene2DNode(OGLResourceLoader loader, OGL2DCamera camera)
	{
		this.mousePoint = new Point2F();
		this.enabled = true;
		this.cameraChanged = true;
		setResourceLoader(loader);
		setCamera(camera);
		setBackingObjectHash(new Hash<T>());
	}

	/**
	 * Sets the current camera and adds the listener to it. 
	 */
	protected void setCamera(OGL2DCamera newCamera)
	{
		if (camera != null)
			camera.removeListener(cameraListener);
		camera = newCamera;
		camera.addListener(cameraListener);
	}

	/**
	 * Sets the backing hash that contains all renderable objects in the scene. 
	 */
	protected void setBackingObjectHash(Hash<T> hash)
	{
		sceneObjects = hash;
		sceneObjectIterator = sceneObjects.iterator();
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
	 * Adds an object to the scene.
	 * @param object the object to add.
	 */
	public void addObject(T object)
	{
		synchronized (sceneObjects)
		{
			sceneObjects.put(object);
		}
	}

	/**
	 * Removes an object from the scene.
	 * @param object the object to remove.
	 */
	public boolean removeObject(T object)
	{
		boolean out = false;
		synchronized (sceneObjects)
		{
			out = sceneObjects.remove(object);
		}
		return out;
	}

	/**
	 * Removes all objects from this scene.
	 */
	public synchronized void clear()
	{
		synchronized (sceneObjects)
		{
			sceneObjects.clear();
		}
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
	public void onCanvasResize(int new_width, int new_height)
	{
		canvasWidth = new_width;
		canvasHeight = new_height;
	}

	@Override
	public void display(OGLGraphics g)
	{
		polygonsRendered = 0;
		
		timeBuildScene = System.nanoTime();
		displayRecreateRenderList(g);
		timeBuildScene = System.nanoTime() - timeBuildScene;

		timeSortScene = System.nanoTime();
		displaySortRenderList(g);
		timeSortScene = System.nanoTime() - timeSortScene;

		timeRenderScene = System.nanoTime();
		displayRenderList(g);
		timeRenderScene = System.nanoTime() - timeRenderScene;

		renderTimeNanos = timeBuildScene + timeSortScene + timeRenderScene;
	}

	/**
	 * Returns the amount of time that the scene took to build in nanoseconds.
	 */
	public long getSceneBuildTimeNanos()
	{
		return timeBuildScene;
	}
	
	/**
	 * Returns the amount of time that the scene took to sort in nanoseconds.
	 */
	public long getSceneSortTimeNanos()
	{
		return timeSortScene;
	}
	
	/**
	 * Returns the amount of time for the scene to send the rendering instructions in nanoseconds.
	 */
	public long getSceneRenderTimeNanos()
	{
		return timeRenderScene;
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
	 * Returns the number of objects within the camera's bounds.
	 */
	public int getOnCameraCount()
	{
		return countOnCamera;
	}
	
	/**
	 * Returns the number of objects outside the camera's bounds.
	 */
	public int getOffCameraCount()
	{
		return countOffCamera;
	}
	
	/**
	 * Resamples all layers and rebuilds the sorted render list.
	 */
	protected void displayRecreateRenderList(OGLGraphics g)
	{
		if (renderList == null)
			renderList = new List<Node>(20);

		renderListSize = 0;
		renderListObjects = 0;
		displayRecreateRenderListForObjects(g);
	}
	
	/**
	 * Creates the render list entries for an object.
	 * Calls {@link #excludeObjectFromVisibility(OGLScene2DElement)}.
	 */
	protected void displayRecreateRenderListForObjects(OGLGraphics g)
	{
		synchronized (sceneObjects)
		{
			if (sceneObjectIterator == null) 
				sceneObjectIterator = sceneObjects.iterator();
			ResettableIterator<T> rit = sceneObjectIterator;
			rit.reset();
			while (rit.hasNext())
			{
				T obj = rit.next();
				
				if (excludeObjectFromVisibility(obj))
					continue;
				
				OGLSkin group = obj.getSkin();
				if (group != null) 
				{
					for (int p = 0; p < group.size(); p++)
					{
						renderListAddNode(g, loader, obj, renderListObjects, group.get(p), p, obj.getRenderPositionZ());
					}
				}
				else
					renderListAddNode(g, loader, obj, renderListObjects, DEFAULT_STEP, 0, obj.getRenderPositionZ());
				renderListObjects++;
			}
		}
	}

	/**
	 * Checks if an object should be excluded from visibility.
	 * By default, this just checks {@link OGLScene2DElement#isVisible()} for visibility.
	 * If overridden, this can check for literally anything!
	 * @param object the object to check.
	 * @return true if so (should be excluded), false otherwise.
	 */
	protected boolean excludeObjectFromVisibility(T object)
	{
		return !object.isVisible() || !objectIsOnCamera(object);
	}
	
	/**
	 * Checks if an object is in the camera's view.
	 */
	protected boolean objectIsOnCamera(SpatialHashable e)
	{
		return
			e.getObjectCenterX() - e.getObjectHalfWidth() <= camera.getObjectCenterX() + camera.getObjectHalfWidth() &&
			e.getObjectCenterY() - e.getObjectHalfHeight() <= camera.getObjectCenterY() + camera.getObjectHalfHeight() &&
			e.getObjectCenterX() + e.getObjectHalfWidth() >= camera.getObjectCenterX() - camera.getObjectHalfWidth() &&
			e.getObjectCenterY() + e.getObjectHalfHeight() >= camera.getObjectCenterY() - camera.getObjectHalfHeight();
	}

	/**
	 * Checks if an object is in the camera's view.
	 */
	protected boolean objectIsOnCamera(OGLScene2DElement p)
	{
		return
			p.getRenderPositionX() - p.getRenderHalfWidth() <= camera.getObjectCenterX() + camera.getObjectHalfWidth() &&
			p.getRenderPositionY() - p.getRenderHalfHeight() <= camera.getObjectCenterY() + camera.getObjectHalfHeight() &&
			p.getRenderPositionX() + p.getRenderHalfWidth() >= camera.getObjectCenterX() - camera.getObjectHalfWidth() &&
			p.getRenderPositionY() + p.getRenderHalfHeight() >= camera.getObjectCenterY() - camera.getObjectHalfHeight();
	}

	protected void renderListAddNode(OGLGraphics g, OGLResourceLoader loader, 
			OGLScene2DElement element, int id, Step step, int pass, float zOrder)
	{
		Node n = null;
		if (renderListSize == renderList.size())
			renderList.add(n = new Node(g, loader, element, id, step, pass, zOrder));
		else
		{
			n = renderList.getByIndex(renderListSize);
			n.set(g, loader, element, id, step, pass, zOrder);
		}
		renderListSize++;
	}
	
	/**
	 * Displays the contents of the render list.
	 * Must be called AFTER displayRecreateRenderList() and displaySortRenderList()
	 * (but not immediately after).
	 */
	protected void displayRenderList(OGLGraphics g)
	{
		displayRenderListStartContext(g);
		displayRenderListContent(g);
		displayRenderListFinishContext(g);
	}

	/**
	 * Render list start context.
	 */
	protected void displayRenderListStartContext(OGLGraphics g)
	{
		g.attribPush(
				AttribType.ENABLE, 			// "enable"
				AttribType.LIGHTING, 		// light
				AttribType.DEPTH_BUFFER, 	// depth func/mask
				AttribType.COLOR_BUFFER,	// blend/color
				AttribType.POLYGON,			// face cull
				AttribType.SCISSOR);		// scissor
		
		g.setTexture2DEnabled(true);
		g.setBlendingEnabled(true);
		g.setDepthTestEnabled(false);
		g.setDepthMask(false);
		g.setLightingEnabled(false);
		g.setFaceCullingEnabled(true);
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
		
		if (getFlipY())
			g.setFaceCullingSide(FaceSide.FRONT);
		else
			g.setFaceCullingSide(FaceSide.BACK);

		if (context == null)
		{
			// reset context
			g.setTextureUnit(0);
			g.unbindTexture2D();
			g.unbindShaderProgram();
			g.setBlendingFunc(BlendFunc.REPLACE);
			g.setTexGenSEnabled(false);
			g.setTexGenTEnabled(false);
			context = new Context();
		}
		else
		{
			// reset context
			for (int i = 0; i < context.textureCount; i++)
			{
				g.setTextureUnit(i);
				g.unbindTexture2D();
			}
			
			g.unbindShaderProgram();
			g.setBlendingFunc(BlendFunc.REPLACE);
			g.setTexGenSEnabled(false);
			g.setTexGenTEnabled(false);
			context.clear();
		}
	}
	
	/**
	 * Displays the actual contents of the render list.
	 */
	protected void displayRenderListContent(OGLGraphics g)
	{
		if (vertexBuffer == null)
			vertexBuffer = new VBOContext(g);

		g.matrixMode(MatrixType.MODELVIEW);
		g.matrixPush();
		g.matrixReset();
		
		g.matrixMode(MatrixType.PROJECTION);
		g.matrixPush();
		g.matrixReset();
		
		// can't set orthographic projection if either axis is completely collapsed.
		// it causes a GL error.
		if (camera.getObjectHalfWidth() != 0 && camera.getObjectHalfHeight() != 0)
		{
			if (getFlipY())
				g.matrixOrtho(
					0f,
					camera.getWidth(),
					camera.getHeight(),
					0f,
					1,
					-1);
			else
				g.matrixOrtho(
					0f,
					camera.getWidth(),
					0f,
					camera.getHeight(),
					1,
					-1);
		}

		for (int i = 0; i < renderListSize; i++)
		{
			Node n = renderList.getByIndex(i);
			displayContextShaderBreak(g, context, n);
			displayContextTextureBreak(g, context, n);
			displayContextBlendBreak(g, context, n);
			displayContextTexGenSBreak(g, context, n);
			displayContextTexGenTBreak(g, context, n);
			displayContextPassBreak(g, context, n);
			displayContextStepBreak(g, context, n);
			displayContextMeshBreak(g, context, n);
			batchObject(g, n);
		}
		
		if (!vertexBuffer.flushed)
			vertexBuffer.flush(g, context);
		
		g.matrixMode(MatrixType.PROJECTION);
		g.matrixPop();
		
		g.matrixMode(MatrixType.MODELVIEW);
		g.matrixPop();
	}

	/**
	 * Render list finish context.
	 */
	protected void displayRenderListFinishContext(OGLGraphics g)
	{
		// reset context
		for (int i = 0; i < context.textureCount; i++)
		{
			g.setTextureUnit(i);
			g.unbindTexture2D();
		}
		
		g.unbindShaderProgram();
		context.clear();
		g.attribPop();
	}
	
	/**
	 * Sorts the render list.
	 */
	protected void displaySortRenderList(OGLGraphics g)
	{
		renderList.sort(0, renderListSize);
	}

	/**
	 * Performs a shader break if necessary.
	 */
	protected void displayContextShaderBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.shader == n.nodeShader)
			return;
		
		vertexBuffer.flush(g, context);
		
		context.shader = n.nodeShader;
		if (context.shader != null)
			context.shader.bindTo(g);
		else
			g.unbindShaderProgram();
	}

	/**
	 * Performs a texture break if necessary.
	 */
	protected void displayContextTextureBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.textureHash == n.nodeTextureHash)
			return;
		
		vertexBuffer.flush(g, context);
		
		for (int t = 0; t < context.textureCount; t++)
		{
			g.setTextureUnit(t);
			g.unbindTexture2D();
		}
			
		for (int t = 0; t < n.nodeTextureLength; t++)
		{
			g.setTextureUnit(t);
			if (n.nodeTexture[t] != null)
				n.nodeTexture[t].bindTo(g);
			else
				g.unbindTexture2D();
		}

		context.textures = n.nodeTexture;
		context.textureHash = n.nodeTextureHash;
		context.textureCount = n.nodeTextureLength;
	}

	/**
	 * Performs a blend mode break if necessary.
	 */
	protected void displayContextBlendBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.blendMode == n.nodeBlendMode)
			return;
		
		vertexBuffer.flush(g, context);
		
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

	/**
	 * Performs a texture generation S-axis break if necessary.
	 */
	protected void displayContextTexGenSBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.texGenS == n.nodeSTexGen)
			return;
		
		vertexBuffer.flush(g, context);
		
		context.texGenS = n.nodeSTexGen;

		switch (context.texGenS)
		{
			case Step.TEXGEN_NONE:
				g.setTexGenSEnabled(false);
				break;
			case Step.TEXGEN_EYE:
			case Step.TEXGEN_OBJECT:
				g.setTexGenSEnabled(true);
				break;
		}
	}

	/**
	 * Performs a texture generation S-axis break if necessary.
	 */
	protected void displayContextTexGenTBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.texGenT == n.nodeTTexGen)
			return;
		
		vertexBuffer.flush(g, context);
		
		context.texGenT = n.nodeTTexGen;

		switch (context.texGenT)
		{
			case Step.TEXGEN_NONE:
				g.setTexGenTEnabled(false);
				break;
			case Step.TEXGEN_EYE:
			case Step.TEXGEN_OBJECT:
				g.setTexGenTEnabled(true);
				break;
		}
	}

	/**
	 * Performs a pass break if necessary.
	 */
	protected void displayContextPassBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.pass == n.nodePass)
			return;
		
		vertexBuffer.flush(g, context);
		
		context.pass = n.nodePass;
	}
	
	/**
	 * Performs a render step break for object.
	 */
	protected void displayContextStepBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.step == n.nodeStepRef)
			return;

		long currentMillis = g.currentTimeMillis();
		vertexBuffer.flush(g, context);
		context.step = n.nodeStepRef;
		context.stepInst.texture_rot = context.step.getTextureRotation(currentMillis);
		context.stepInst.texture_s0 = context.step.getTextureS0(currentMillis);
		context.stepInst.texture_t0 = context.step.getTextureT0(currentMillis);
		context.stepInst.texture_s1 = context.step.getTextureS1(currentMillis);
		context.stepInst.texture_t1 = context.step.getTextureT1(currentMillis);
		context.stepInst.color_r = context.step.getColorRed(currentMillis);
		context.stepInst.color_g = context.step.getColorGreen(currentMillis);
		context.stepInst.color_b = context.step.getColorBlue(currentMillis);
		context.stepInst.color_a = context.step.getColorAlpha(currentMillis);
		context.stepInst.pivot_s = context.step.getTextureRotationPivotS();
		context.stepInst.pivot_t = context.step.getTextureRotationPivotT();
	}

	/**
	 * Performs a mesh break for object.
	 */
	protected void displayContextMeshBreak(OGLGraphics g, Context context, Node n)
	{
		if (context.mesh == n.objRef.getMeshView())
			return;

		vertexBuffer.flush(g, context);

		context.mesh = n.objRef.getMeshView();
	}
	

	protected void displaySetColorForObject(OGLGraphics g, Node n)
	{
		g.setColor(
				camera.getRed() * n.objRef.getRed() * context.stepInst.color_r,
				camera.getGreen() * n.objRef.getGreen() * context.stepInst.color_g,
				camera.getBlue() * n.objRef.getBlue() * context.stepInst.color_b,
				camera.getAlpha() * n.objRef.getAlpha() * context.stepInst.color_a
				);
	}

	/**
	 * Stores the geometry in a buffer to be drawn later.
	 */
	protected void batchObject(OGLGraphics g, Node n)
	{
		MeshView m = n.objRef.getMeshView();
		if (m != null && m.getGeometryType().isBatchable())
			vertexBuffer.addMeshCoords(context.stepInst, n);
		else
			displayObject(g, n);
	}
	
	/**
	 * Draws the current object.
	 */
	protected void displayObject(OGLGraphics g, Node n)
	{
		OGLScene2DElement obj = n.objRef;
		
		float pos_x = obj.getRenderPositionX();
		float pos_y = obj.getRenderPositionY();
		float pos_halfwidth = obj.getRenderHalfWidth();
		float pos_halfheight = obj.getRenderHalfHeight();
		float rotz = obj.getRenderRotationZ();
		
		g.matrixMode(MatrixType.MODELVIEW); g.matrixPush();
		g.matrixTranslate(
				pos_x - (camera.getObjectCenterX() - camera.getObjectHalfWidth()), 
				pos_y - (camera.getObjectCenterY() - camera.getObjectHalfHeight()), 
				0);
		g.matrixRotateZ(getFlipY() ? -rotz : rotz);
		g.matrixScale(pos_halfwidth, pos_halfheight, 1);
		
		g.setTextureUnit(0);
		g.matrixMode(MatrixType.TEXTURE); g.matrixPush();
		g.matrixTranslate(-context.stepInst.pivot_s, -context.stepInst.pivot_t, 0);
		g.matrixRotateZ(context.stepInst.texture_rot);
		g.matrixTranslate(context.stepInst.texture_s0-context.stepInst.pivot_s, 
				context.stepInst.texture_t0-context.stepInst.pivot_t, 0);
		g.matrixScale((context.stepInst.texture_s1-context.stepInst.texture_s0) * obj.getSkinScaleS(), 
				(context.stepInst.texture_t1-context.stepInst.texture_t0) * obj.getSkinScaleT(), 1);

		switch (context.texGenS)
		{
			case Step.TEXGEN_EYE:
			{
				float[] f = n.nodeStepRef.getTextureSPlane();
				g.setTexGenMode(TextureCoordType.S, TextureGenMode.EYE);
				g.setTexGenEyePlane(TextureCoordType.S, f[0], f[1], f[2], f[3]);
			}
				break;
			case Step.TEXGEN_OBJECT:
			{
				float[] f = n.nodeStepRef.getTextureSPlane();
				g.setTexGenMode(TextureCoordType.S, TextureGenMode.OBJECT);
				g.setTexGenObjectPlane(TextureCoordType.S, f[0], f[1], f[2], f[3]);
			}
				break;
		}
		
		switch (context.texGenT)
		{
			case Step.TEXGEN_EYE:
			{
				float[] f = n.nodeStepRef.getTextureTPlane();
				g.setTexGenMode(TextureCoordType.T, TextureGenMode.EYE);
				g.setTexGenEyePlane(TextureCoordType.T, f[0], f[1], f[2], f[3]);
			}
				break;
			case Step.TEXGEN_OBJECT:
			{
				float[] f = n.nodeStepRef.getTextureTPlane();
				g.setTexGenMode(TextureCoordType.T, TextureGenMode.OBJECT);
				g.setTexGenObjectPlane(TextureCoordType.T, f[0], f[1], f[2], f[3]);
			}
				break;
		}

		displaySetColorForObject(g, n);
		MeshView draw = n.objRef.getMeshView();
		if (draw != null)
		{
			g.draw(draw);
			polygonsRendered += draw.getGeometryType().calculatePolygonCount(draw.getElementCount());
		}
		
		g.setTextureUnit(0);
		g.matrixMode(MatrixType.TEXTURE); g.matrixPop();
		g.matrixMode(MatrixType.MODELVIEW); g.matrixPop();
	}

	/**
	 * Returns this layer's native mouse position, x-axis.
	 */
	public float getMousePositionX()
	{
		return mousePoint.x;
	}

	/**
	 * Returns this layer's native mouse position, y-axis.
	 */
	public float getMousePositionY()
	{
		return mousePoint.y;
	}

	/**
	 * Sets the mouse coordinates.
	 */
	protected void setMouseCoordinates()
	{
		canvasMouseDegreeX = (float)canvasMouseX / canvasWidth;
		canvasMouseDegreeY = 1.0f - ((float)canvasMouseY / canvasHeight);
		updateMouseCoordinates(canvasMouseDegreeX, canvasMouseDegreeY);
	}

	/**
	 * Updates this layer's internal coordinates.
	 * @param canvasX	the canvas X degree coordinate.
	 * @param canvasY	the canvas Y degree coordinate.
	 */
	protected void updateMouseCoordinates(float canvasX, float canvasY)
	{
		mousePoint.x = (float)RMath.linearInterpolate(canvasX, 
				camera.getObjectCenterX() - camera.getObjectHalfWidth(), 
				camera.getObjectCenterX() + camera.getObjectHalfWidth());
		if (getFlipY())
			mousePoint.y = (float)RMath.linearInterpolate(canvasY, 
					camera.getObjectCenterY() + camera.getObjectHalfHeight(), 
					camera.getObjectCenterY() - camera.getObjectHalfHeight());
		else
			mousePoint.y = (float)RMath.linearInterpolate(canvasY, 
					camera.getObjectCenterY() - camera.getObjectHalfHeight(), 
					camera.getObjectCenterY() + camera.getObjectHalfHeight());
	}

	@Override
	public boolean glKeyPress(int keycode)
	{
		return false;
	}

	@Override
	public boolean glKeyRelease(int keycode)
	{
		return false;
	}

	@Override
	public boolean glKeyTyped(int keycode)
	{
		return false;
	}

	@Override
	public boolean glMousePress(int mousebutton)
	{
		return false;
	}

	@Override
	public boolean glMouseRelease(int mousebutton)
	{
		return false;
	}

	@Override
	public boolean glMouseWheel(int units)
	{
		return false;
	}

	@Override
	public void glMouseMove(int unitsX, int coordinateX, int unitsY, int coordinateY)
	{
		canvasMouseX = coordinateX;
		canvasMouseY = coordinateY;
		setMouseCoordinates();
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
	
	/** Vertex Buffer rendering context. */
	protected class VBOContext
	{
		/** Interleaved Geometry Info Buffer. */
		protected FloatBuffer geometryFloatBuffer;
		/** Interleaved Geometry Info Buffer Index */
		protected int geometryListIndex;
		/** Geometry Float Buffer */
		protected OGLFloatBuffer geometryBuffer;
		/** Has this already been flushed? */
		protected boolean flushed;
		/** Number of VBO Elements to draw. */
		protected int elements;
	
		public VBOContext(OGLGraphics g)
		{
			flushed = true;
			geometryBuffer = new OGLFloatBuffer(g, BufferType.GEOMETRY);
			geometryFloatBuffer = Common.allocDirectFloatBuffer(2000); // static value
		}
		
		/**
		 * Draws and resets the buffer.
		 */
		public void flush(OGLGraphics g, Context c)
		{
			if (flushed) 
				return;
			
			if (c.mesh == null)
				return;
			
			StepInstance inst = c.stepInst;
			GeometryType gtype = c.mesh.getGeometryType();
			
			g.matrixMode(MatrixType.MODELVIEW); 
			g.matrixPush();
			g.matrixReset();
			g.matrixTranslate(
					-(camera.getObjectCenterX() - camera.getObjectHalfWidth()), 
					-(camera.getObjectCenterY() - camera.getObjectHalfHeight()), 
					0);

			g.setTextureUnit(0);
			g.matrixMode(MatrixType.TEXTURE); 
			g.matrixPush();
			g.matrixReset();

			g.matrixTranslate(inst.pivot_s, inst.pivot_t, 0);
			g.matrixRotateZ(inst.texture_rot);
			g.matrixTranslate(-inst.pivot_s, -inst.pivot_t, 0);
			g.matrixTranslate(inst.texture_s0, inst.texture_t0, 0);
			g.matrixScale(inst.texture_s1-inst.texture_s0, inst.texture_t1-inst.texture_t0, 1);
		
			geometryBuffer.setCapacity(g, CachingHint.STREAM_DRAW, geometryListIndex);
			geometryBuffer.sendSubData(g, geometryFloatBuffer, geometryListIndex, 0);
			
			OGLGeometryUtils.drawInterleavedGeometry(g, geometryBuffer, gtype, elements, geometryInfo);
			
			polygonsRendered += gtype.calculatePolygonCount(elements);
			
			geometryFloatBuffer.rewind();
			geometryListIndex = 0;
			elements = 0;
	
			g.matrixMode(MatrixType.MODELVIEW); 
			g.matrixPop();

			g.setTextureUnit(0);
			g.matrixMode(MatrixType.TEXTURE); 
			g.matrixPop();
			
			flushed = true;
		}
		
		/**
		 * Adds a set of coordinates to the buffer.
		 * @param inst the step instance to use for color information.
		 * @param node the node being drawn.
		 */
		public void addMeshCoords(StepInstance inst, Node node)
		{
			float red = camera.getRed() * inst.color_r;
			float green = camera.getGreen() * inst.color_g;
			float blue = camera.getBlue() * inst.color_b;
			float alpha = camera.getAlpha() * inst.color_a;
			
			int n = geometryListIndex;
	
			OGLScene2DElement e = node.objRef;
			float x = e.getRenderPositionX() - e.getRenderHalfWidth();
			float y = e.getRenderPositionY() - e.getRenderHalfHeight();
			float sx = e.getRenderHalfWidth()*2f;
			float sy = e.getRenderHalfHeight()*2f;
			float r = e.getRenderRotationZ();
			MeshView m = e.getMeshView();

			int components = m.getElementCount()*9;
			
			if ((geometryListIndex + components) > geometryFloatBuffer.capacity())
			{
				FloatBuffer newbuf = Common.allocDirectFloatBuffer(geometryFloatBuffer.capacity() * 4);
				geometryFloatBuffer.rewind();
				newbuf.put(geometryFloatBuffer);
				newbuf.rewind();
				geometryFloatBuffer = newbuf;
			}

			for (int i = 0; i < m.getElementCount(); i++)
			{
				int idx = i*9;
				
				// apply scaling
				float px = ((m.getVertex(i, 0) + 1f) / 2f) * sx;
				float py = ((m.getVertex(i, 1) + 1f) / 2f) * sy;
				
				// apply rotation if any.
				if ((r % 360.0f) != 0.0f)
				{
					double rrads = getFlipY() ? -RMath.degToRad(r) : RMath.degToRad(r);
					double cosr = Math.cos(rrads);
					double sinr = Math.sin(rrads);
					px -= e.getRenderHalfWidth();
					py -= e.getRenderHalfHeight();
					double rx = px * cosr - py * sinr; 
					double ry = py * cosr + px * sinr;
					px = (float)rx + e.getRenderHalfWidth();
					py = (float)ry + e.getRenderHalfHeight();
				}
				
				geometryFloatBuffer.put(n+idx+0, x + px);
				geometryFloatBuffer.put(n+idx+1, y + py);
				geometryFloatBuffer.put(n+idx+2, 0);

				geometryFloatBuffer.put(n+idx+3, m.getTextureCoordinate(i, 0, 0) * node.objRef.getSkinScaleS());
				geometryFloatBuffer.put(n+idx+4, m.getTextureCoordinate(i, 0, 1) * node.objRef.getSkinScaleT());

				geometryFloatBuffer.put(n+idx+5, red * e.getRed());
				geometryFloatBuffer.put(n+idx+6, green * e.getGreen());
				geometryFloatBuffer.put(n+idx+7, blue * e.getBlue());
				geometryFloatBuffer.put(n+idx+8, alpha * e.getAlpha());
			}

			geometryListIndex += components;
			flushed = false;
			elements += m.getElementCount();
		}
		
	}

	/**
	 * Holds a renderer node.
	 */
	protected static class Node implements Comparable<Node>
	{
		/** Node id. */
		public int nodeId;
		/** The reference to the render step that is used to create this node. */
		public Step nodeStepRef;
		/** Reference to object. */
		public OGLScene2DElement objRef;

		/** The number of texture objects. */
		public int nodeTextureLength;
		/** Multiple texture unit combiner. */
		public OGLTexture2D[] nodeTexture;
		/** Texture hash. */
		public int nodeTextureHash;

		/** The shader program object to use. */
		public OGLShaderProgram nodeShader;

		/** Ordering for closeness to the camera. */
		public float nodeZOrder;
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

		/**
		 * Creates a new render node.
		 */
		public Node(OGLGraphics g, OGLResourceLoader loader, OGLScene2DElement element, 
				int id, Step step, int pass, float zOrder)
		{
			set(g, loader, element, id, step, pass, zOrder);
		}
		
		/**
		 * Creates a new render node.
		 */
		public void set(OGLGraphics g, OGLResourceLoader loader, OGLScene2DElement element, 
				int id, Step step, int pass, float zOrder)
		{
			nodeId = id;
			objRef = element;
			nodeStepRef = step;
			nodeZOrder = zOrder;
			nodePass = pass;
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
				int ti = step.getTextureIndex(g.currentTimeMillis());
				if (ti >= 0)
				{
					setTextureLen(1);
					OGLTextureResource ogltr = step.getTextureList()[ti];
					if (!loader.containsTexture(ogltr))
						loader.cacheTexture(g, ogltr);
					nodeTexture[0] = loader.getTexture(ogltr);
				}
				else
				{
					setTextureLen(1);
					nodeTexture[0] = null;
					setTextureLen(0);
				}
			}
			else 
			{
				int i = 0;
				OGLTextureResource[] trlist = step.getTextureList();
				setTextureLen(trlist.length);
				for (int x = 0; x < trlist.length; x++)
				{
					OGLTextureResource ogltr = trlist[x];
					if (!loader.containsTexture(ogltr))
						loader.cacheTexture(g, ogltr);
					nodeTexture[i++] = loader.getTexture(ogltr);
				}
			}
			nodeTextureHash = Arrays.hashCode(nodeTexture);
			
		}
		
		/** Set texture unit array length. */
		public void setTextureLen(int len)
		{
			nodeTextureLength = len;
			if (nodeTexture == null || nodeTextureLength > nodeTexture.length)
				nodeTexture = new OGLTexture2D[nodeTextureLength];
		}
		
		@Override
		public int compareTo(Node n)
		{
			return  
				nodeZOrder == n.nodeZOrder ?
				nodeId == n.nodeId ?
				nodePass == n.nodePass ?
				nodeShader == n.nodeShader ?
				nodeTextureHash == n.nodeTextureHash ?
				nodeBlendMode == n.nodeBlendMode ?
				nodeSTexGen == n.nodeSTexGen ?
				nodeTTexGen == n.nodeTTexGen ?
					0 :
				nodeTTexGen - n.nodeTTexGen :
				nodeSTexGen - n.nodeSTexGen :
				nodeBlendMode.ordinal() - n.nodeBlendMode.ordinal() : 
				-1 :
				(nodeShader != null ? nodeShader.getGLId() : 0) - (n.nodeShader != null ? n.nodeShader.getGLId() : 0):
				nodePass - n.nodePass :
				nodeId - n.nodeId :
				(nodeZOrder < n.nodeZOrder ? -1 : 1);
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			sb.append(nodeZOrder).append(", ");
			sb.append(nodeId).append(", ");
			sb.append(nodePass).append(", ");
			sb.append(nodeShader).append(", ");
			sb.append(String.format("%08x", nodeTextureHash)).append(", ");
			sb.append(String.format("%8s",nodeBlendMode)).append(", ");
			sb.append(nodeSTexGen).append(", ");
			sb.append(nodeTTexGen);
			sb.append(']');
			return sb.toString();
		}

	}
	
	/**
	 * Holds the renderer list context.
	 */
	protected static class Context
	{
		public BlendType blendMode;
		public int pass;
		public int texGenS;
		public int texGenT;
		public int textureCount;
		public OGLTexture2D[] textures;
		public int textureHash;
		public OGLShaderProgram shader;
		public Step step;
		public StepInstance stepInst;
		public MeshView mesh; 

		public Context()
		{
			stepInst = new StepInstance();
			clear();
		}
		
		public void clear()
		{
			pass = -1;
			texGenS = -1;
			texGenT = -1;
			blendMode = null;
			textureCount = 0;
			textures = null;
			textureHash = 0;
			shader = null;
			step = null;
			mesh = null;
		}
	}
	
}
