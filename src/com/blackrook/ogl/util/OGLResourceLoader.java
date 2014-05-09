/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.blackrook.commons.bank.Bank;
import com.blackrook.commons.hash.Hash;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.list.List;
import com.blackrook.commons.math.RMath;
import com.blackrook.ogl.OGLGraphicUtils;
import com.blackrook.ogl.OGLGraphics;
import com.blackrook.ogl.enums.AttribType;
import com.blackrook.ogl.exception.GraphicsException;
import com.blackrook.ogl.node.OGLCanvasNodeAdapter;
import com.blackrook.ogl.object.framebuffer.OGLFrameBuffer;
import com.blackrook.ogl.object.framebuffer.OGLFrameRenderBuffer;
import com.blackrook.ogl.object.framebuffer.OGLFrameBuffer.AttachPoint;
import com.blackrook.ogl.object.framebuffer.OGLFrameRenderBuffer.Format;
import com.blackrook.ogl.object.shader.OGLShaderFragmentProgram;
import com.blackrook.ogl.object.shader.OGLShaderGeometryProgram;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.shader.OGLShaderVertexProgram;
import com.blackrook.ogl.object.shader.uniform.OGLUniform;
import com.blackrook.ogl.object.texture.OGLTexture;
import com.blackrook.ogl.object.texture.OGLTexture2D;
import com.blackrook.ogl.object.texture.OGLTexture.InternalFormat;
import com.blackrook.ogl.object.texture.OGLTexture.MagFilter;
import com.blackrook.ogl.object.texture.OGLTexture.MinFilter;
import com.blackrook.ogl.util.resource.OGLShaderResource;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * This is an OGLSystemListener that automatically loads textures and shaders
 * within a frame. This should be placed at the beginning of the listener list
 * for best results.
 * @author Matthew Tropiano
 */
public class OGLResourceLoader extends OGLCanvasNodeAdapter
{
	/** List of resource loader listeners. */
	private List<OGLResourceLoaderListener> resourceListeners;
	
	/** Texture bank. */
	private Bank<OGLTextureResource, OGLTexture2D> textureBank;
	/** Queue of texture objects that need caching this frame. */
	private Queue<OGLTextureResource> textureCacheQueue;
	/** Hash of texture objects that need caching this frame. */
	private Hash<OGLTextureResource> textureCacheQueueNameList;
	/** Queue of OpenGL texture objects that need destroying this frame. */
	private Queue<OGLTextureResource> textureDestroyQueue;
	
	/** Special bank for render target objects. */
	private Bank<OGLTextureResource, RenderTarget> targetBank;
	/** Special bank for render target depth buffer objects. */
	private Bank<Integer, DepthBuffer> depthBufferBank;
	
	/** Shader program bank. */
	private Bank<OGLShaderResource, OGLShaderProgram> shaderBank;
	/** Queue of shader objects that need caching this frame. */
	private Queue<OGLShaderResource> shaderCacheQueue;
	/** Hash of shader objects that need caching this frame. */
	private Hash<OGLShaderResource> shaderCacheQueueNameList;
	/** Queue of OpenGL shader objects that need destroying this frame. */
	private Queue<OGLShaderResource> shaderDestroyQueue;
	
	/** Do textures need to be reloaded this frame? */
	private boolean textureReloadTrigger;
	/** Do shaders need to be reloaded this frame? */
	private boolean shaderReloadTrigger;

	/** Are we forcing no mipmapping? */
	private boolean forceNoMipmap;
	/** Are we forcing 32 bit textures? */
	private boolean force32BitTextures;
	/** Are we forcing nearest filtering? */
	private boolean forceNearest;
	/** Are we compressing textures? */
	private boolean compressTextures;
	/** What is the texture anisotropy level? */
	private float textureAnisotropy;
	/** Are we using the best filtering possible for texture minification? */
	private boolean textureBestFiltering;
	
	/**
	 * Creates a new resource loader node.
	 */
	public OGLResourceLoader()
	{
		resourceListeners = new List<OGLResourceLoaderListener>(4);
		textureBank = new Bank<OGLTextureResource,OGLTexture2D>();
		textureCacheQueue = new Queue<OGLTextureResource>();
		textureCacheQueueNameList = new Hash<OGLTextureResource>();
		textureDestroyQueue = new Queue<OGLTextureResource>();
		targetBank = new Bank<OGLTextureResource,RenderTarget>();
		depthBufferBank = new Bank<Integer,DepthBuffer>();
		shaderBank = new Bank<OGLShaderResource,OGLShaderProgram>();
		shaderCacheQueue = new Queue<OGLShaderResource>();
		shaderCacheQueueNameList = new Hash<OGLShaderResource>();
		shaderDestroyQueue = new Queue<OGLShaderResource>();
		forceNoMipmap = false;
		force32BitTextures = false;
		forceNearest = false;
		compressTextures = false;
		textureAnisotropy = 0f;
		textureBestFiltering = false;
	}
	
	/**
	 * Adds an OGLResourceLoaderListener to this system.
	 */
	public void addLoaderListener(OGLResourceLoaderListener l)
	{
		resourceListeners.add(l);
	}

	/**
	 * Removes an OGLResourceLoaderListener from this system.
	 */
	public boolean removeLoaderListener(OGLResourceLoaderListener l)
	{
		return resourceListeners.remove(l);
	}

	@Override
	public void display(OGLGraphics g)
	{
		if (textureReloadTrigger)
		{
			reloadTextures(g);
			textureReloadTrigger = false;
		}
		
		if (shaderReloadTrigger)
		{
			reloadShaders(g);
			shaderReloadTrigger = false;
		}
		
		cycleShaders(g);
		cycleTextures(g);
	}

	/**
	 * Triggers a texture reload for the next frame. 
	 * May consume quite a bit of time depending on how many textures
	 * are present in memory.
	 */
	public void triggerTextureReload()
	{
		textureReloadTrigger = true;
	}

	/**
	 * Triggers a shader reload for the next frame.
	 * May consume quite a bit of time depending on how many shaders
	 * are present in memory.
	 */
	public void triggerShaderReload()
	{
		shaderReloadTrigger = true;
	}

	/**
	 * Queues a bunch of textures for loading each frame.
	 * All textures queued are not guaranteed to be loaded the next frame.
	 */
	public void queueTextures(OGLTextureResource ... textures)
	{
		for (OGLTextureResource textureDef : textures)
		{
			textureCacheQueue.enqueue(textureDef);
			fireTextureAddedEvent(textureDef);
			textureCacheQueueNameList.put(textureDef);
		}
	}

	/**
	 * Queues a bunch of shaders for loading each frame.
	 * All shaders queued are not guaranteed to be loaded the next frame.
	 */
	public void queueShaders(OGLShaderResource ... shaders)
	{
		for (OGLShaderResource shaderDef : shaders)
		{
			shaderCacheQueue.enqueue(shaderDef);
			fireShaderAddedEvent(shaderDef);
			shaderCacheQueueNameList.put(shaderDef);
		}
	}

	/**
	 * Caches a texture into OpenGL. This will do nothing if the texture
	 * is already added to the internal bank (except clear its flag).
	 * This is an inline call to the loader and will return once the texture is loaded.
	 */
	public void cacheTexture(OGLGraphics g, OGLTextureResource textureDef)
	{
		if (textureBank.getByKey(textureDef) != null)
		{
			textureBank.clearFlag(textureDef);
			fireTextureTouchedEvent(textureDef);
		}
		else
		{
			synchronized (textureCacheQueue)
			{
				try{
					OGLTexture2D texture = loadTexture(g, textureDef);
	
					if (texture != null)
					{
						if (textureDef.isRenderTarget())
						{
							RenderTarget rt = new RenderTarget(g, texture);
							DepthBuffer db = depthBufferBank.getByKey(textureDef.getDepthId()); 
							if (db == null)
							{
								db = new DepthBuffer(g, texture.getWidth(), texture.getHeight());
								depthBufferBank.add(textureDef.getDepthId(), db);
							}
							rt.depthBuffer = db;
							db.renderTargetList.add(rt);
							rt.frameBufferObject.attachToRenderBuffer(g, AttachPoint.DEPTH, db.depthBuffer);
							g.checkFrameBufferStatus();
							targetBank.add(textureDef, rt);
						}
						textureBank.add(textureDef, texture);
						fireTextureCachedEvent(textureDef, texture);
					}
				} catch (GraphicsException e) {
					fireTextureCacheGraphicErrorEvent(textureDef, e);
				}
				textureCacheQueueNameList.remove(textureDef);
				textureCacheQueue.notify();
			}
		}
	}

	/**
	 * Caches a shader program into OpenGL. This will do nothing if the shader
	 * is already added to the internal bank (except clear its flag).
	 * This is an inline call to the loader and will return once the shader is loaded.
	 */
	public void cacheShader(OGLGraphics g, OGLShaderResource shaderDef)
	{
		if (shaderBank.getByKey(shaderDef) != null)
		{
			shaderBank.clearFlag(shaderDef);
			fireShaderTouchedEvent(shaderDef);
		}
		else
		{
			synchronized (shaderCacheQueue)
			{
				try{
					OGLShaderProgram shader = loadShader(g, shaderDef);
					if (shader != null)
					{
						shaderBank.add(shaderDef, shader);
						fireShaderCachedEvent(shaderDef, shader);
					}
				} catch (GraphicsException e) {
					fireShaderCacheGraphicErrorEvent(shaderDef, e);
				}
				shaderCacheQueueNameList.remove(shaderDef);
				shaderCacheQueue.notify();
			}
		}
	}

	/**
	 * Queues a bunch of textures for destruction each frame.
	 * All textures queued are guaranteed to be destroyed the next frame.
	 */
	public void destroyTextures(OGLTextureResource ... textures)
	{
		for (OGLTextureResource textureDef : textures)
			textureDestroyQueue.enqueue(textureDef);
	}

	/**
	 * Queues a bunch of shaders for destruction each frame.
	 * All shaders queued are guaranteed to be destroyed the next frame.
	 */
	public void destroyShaders(OGLShaderResource ... shaders)
	{
		for (OGLShaderResource shaderDef : shaders)
			shaderDestroyQueue.enqueue(shaderDef);
	}

	/**
	 * Returns true if the loader contains a cached texture.
	 */
	public boolean containsTexture(OGLTextureResource handle)
	{
		return textureBank.containsKey(handle);
	}

	/**
	 * Returns true if the loader contains a cached shader.
	 */
	public boolean containsShader(OGLShaderResource handle)
	{
		return shaderBank.containsKey(handle);
	}

	/**
	 * Retrieves a texture that has been cached by this.
	 * If it is in the middle of being cached, and the caching has not been
	 * suspended, the current thread calling the method will wait until it
	 * finishes.
	 * @param handle the texture resource to use for texture acquisition.
	 * @return an OGLTexture2D handle to the texture, now cached.
	 */
	public OGLTexture2D getTexture(OGLTextureResource handle)
	{
		synchronized (textureCacheQueue)
		{
			while (textureCacheQueueNameList.contains(handle))
				try {textureCacheQueue.wait();	} catch (InterruptedException e) {}
		}
		return textureBank.getByKey(handle);
	}
	
	/**
	 * Retrieves a shader that has been cached by this.
	 * If it is in the middle of being cached, and the caching has not been
	 * suspended, the current thread calling the method will wait until it
	 * finishes.
	 * @param handle	the shader resource to use for shader acquisition.
	 * @return			the shader object cached by 
	 */
	public OGLShaderProgram getShader(OGLShaderResource handle)
	{
		synchronized (shaderCacheQueue)
		{
			while (shaderCacheQueueNameList.contains(handle))
				try {shaderCacheQueue.wait();	} catch (InterruptedException e) {}
		}
		return shaderBank.getByKey(handle);
	}
	
	/**
	 * Sets if texture compression is turned on.
	 * Setting this to a different value will trigger a texture reload,
	 * and may slow down the system.
	 */
	public void setCompressTextures(boolean value)
	{
		boolean old = compressTextures;
		compressTextures = value;
		if (value != old)
			triggerTextureReload();
	}
	
	/**
	 * Sets if all textures are forced to be loaded as 32-bit textures.
	 * Setting this to a different value will trigger a texture reload,
	 * and may slow down the system.
	 */
	public void setForce32BitTextures(boolean value)
	{
		boolean old = force32BitTextures;
		force32BitTextures = value;
		if (value != old)
			triggerTextureReload();
	}
	
	/**
	 * Sets if all textures will have mipmaps built on load if they are not filtered
	 * using NEAREST filtering.
	 * Setting this to a different value will trigger a texture reload,
	 * and may slow down the system.
	 */
	public void setForceNoMipMaps(boolean value)
	{
		boolean old = forceNoMipmap;
		forceNoMipmap = value;
		if (value != old)
			triggerTextureReload();
	}
	
	/**
	 * Sets if all textures will have will be forced to use NEAREST filtering.
	 * Setting this to a different value will trigger a texture reload,
	 * and may slow down the system.
	 */
	public void setForceNearest(boolean value)
	{
		boolean old = forceNearest;
		forceNearest = value;
		if (value != old)
			triggerTextureReload();
	}

	/**
	 * Sets the texture anisotropy for all textures.
	 * Setting this to a different value will trigger a texture reload,
	 * and may slow down the system.
	 */
	public void setTextureAnisotropy(float value)
	{
		float old = textureAnisotropy;
		textureAnisotropy = value;
		if (value != old)
			triggerTextureReload();
	}

	/**
	 * Sets if we are using the best minification filtering for all textures (trilinear).
	 * A texture will be set to use bilinear filtering if "force nearest" and "no mip maps"
	 * are both set to false. If this is set, it will be trilinear that is used instead of bilinear.
	 * Setting this to a different value will trigger a texture reload,
	 * and may slow down the system.
	 */
	public void setTextureBestFiltering(boolean value)
	{
		boolean old = textureBestFiltering;
		textureBestFiltering = value;
		if (value != old)
			triggerTextureReload();
	}

	/**
	 * Starts the render target rendering for a particular target.
	 * Does nothing if the resource is not a render target.
	 */
	public void startRenderTarget(OGLGraphics g, OGLTextureResource resource)
	{
		RenderTarget rt = null;
		if (!resource.isRenderTarget())
		{
			return;
		}
		else if (!containsTexture(resource))
		{
			cacheTexture(g, resource);
		}
		
		if ((rt = targetBank.getByKey(resource)) != null)
		{
			g.attribPush(AttribType.ENABLE, AttribType.VIEWPORT);
			rt.getFrameBufferObject().bindTo(g);
			g.setViewportToTexture(rt.getColorBufferTexture());
		}
	}
	
	/**
	 * Ends the render target rendering for a particular target.
	 */
	public void endRenderTarget(OGLGraphics g)
	{
		g.unbindFrameBuffer();
		g.attribPop();
	}
	
	/**
	 * Loads a texture into OpenGL. 
	 * Returns an OGLTexture handle of the loaded texture, or null if a problem occurred.
	 */
	public OGLTexture2D loadTexture(OGLGraphics g, OGLTextureResource textureDef)
	{
		OGLTexture2D texture = null;
		try{
			BufferedImage bi = getTextureImage(g, textureDef);
			if (bi != null)
			{
				InternalFormat informat = decideInternalFormat(textureDef);
				MinFilter min_f = decideMinificationFilter(textureDef);
				MagFilter mag_f = decideMagnificationFilter(textureDef);
	
				texture = new OGLTexture2D(g, informat, min_f, mag_f, textureAnisotropy, 
						textureDef.getBorder(), 
						(!textureDef.isNotMipmapped() && min_f != MinFilter.NEAREST && min_f != MinFilter.LINEAR), 
						textureDef.getWrappingModeS(), textureDef.getWrappingModeT());
				texture.sendData(g, bi);
			}
			else
				fireTextureCacheLoadErrorEvent(textureDef, 
						new IOException("Could not open stream for resource '"+textureDef.getPath()+"'."));
		} catch (IOException e) {
			if (texture != null)
			{
				texture.destroy(g);
				texture = null;
			}
			fireTextureCacheLoadErrorEvent(textureDef, e);
		} 
		
		return texture;
	}

	/**
	 * Caches a shader into OpenGL.
	 */
	public OGLShaderProgram loadShader(OGLGraphics g, OGLShaderResource shaderDef)
	{
		OGLShaderProgram shader = null;
		OGLShaderVertexProgram vertShader = null;
		OGLShaderGeometryProgram geomShader = null;
		OGLShaderFragmentProgram fragShader = null;
		
		try{
			InputStream vin = null;
			InputStream gin = null;
			InputStream fin = null;
			
			vin = openStreamForVertexProgramData(shaderDef);
			gin = openStreamForGeometryProgramData(shaderDef);
			fin = openStreamForFragmentProgramData(shaderDef);
			
			if (vin != null)
				vertShader = new OGLShaderVertexProgram(g, shaderDef.getVertexPath(), vin);
			
			if (gin != null)
				geomShader = new OGLShaderGeometryProgram(g, shaderDef.getGeometryPath(), gin);

			if (fin != null)
				fragShader = new OGLShaderFragmentProgram(g, shaderDef.getFragmentPath(), fin);

			shader = new OGLShaderProgram(g, vertShader, geomShader, fragShader);
			
			Queue<OGLUniform> uniformList = shaderDef.getUniforms();
			OGLUniform[] uniforms = new OGLUniform[uniformList.size()];
			uniformList.toArray(uniforms);
			shader.setUniforms(uniforms);
			
		} catch (GraphicsException e) {
			if (vertShader != null)
				vertShader.destroy(g);
			if (geomShader != null)
				geomShader.destroy(g);
			if (fragShader != null)
				fragShader.destroy(g);
			if (shader != null)
				shader.destroy(g);
			fireShaderCacheGraphicErrorEvent(shaderDef, e);
		} catch (IOException e) {
			if (vertShader != null)
				vertShader.destroy(g);
			if (geomShader != null)
				geomShader.destroy(g);
			if (fragShader != null)
				fragShader.destroy(g);
			if (shader != null)
				shader.destroy(g);
			fireShaderCacheLoadErrorEvent(shaderDef, e);
		}
		
		return shader;
	}

	/**
	 * Opens a stream using a texture resource's path.
	 * This assumes that the path is a file path (this should be
	 * overridden if this is not the case).
	 * @return	an open stream for reading the resource.
	 */
	protected InputStream openStreamForTextureData(OGLTextureResource resource) throws IOException
	{
		return new FileInputStream(new File(resource.getPath()));
	}
	
	/**
	 * Opens a stream using a shader resource's vertex program path.
	 * This assumes that the path is a file path (this should be
	 * overridden if this is not the case).
	 * @return	an open stream for reading the resource.
	 */
	protected InputStream openStreamForVertexProgramData(OGLShaderResource resource) throws IOException
	{
		if (resource.getVertexPath() != null)
			return new FileInputStream(new File(resource.getVertexPath()));
		return null;
	}
	
	/**
	 * Opens a stream using a shader resource's geometry program path.
	 * This assumes that the path is a file path (this should be
	 * overridden if this is not the case).
	 * @return	an open stream for reading the resource.
	 */
	protected InputStream openStreamForGeometryProgramData(OGLShaderResource resource) throws IOException
	{
		if (resource.getGeometryPath() != null)
			return new FileInputStream(new File(resource.getGeometryPath()));
		return null;
	}
	
	/**
	 * Opens a stream using a shader resource's fragment program path.
	 * This assumes that the path is a file path (this should be
	 * overridden if this is not the case).
	 * @return	an open stream for reading the resource.
	 */
	protected InputStream openStreamForFragmentProgramData(OGLShaderResource resource) throws IOException
	{
		if (resource.getFragmentPath() != null)
			return new FileInputStream(new File(resource.getFragmentPath()));
		return null;
	}
	
	/**
	 * Reloads all textures cached in this renderer.
	 */
	protected void reloadTextures(OGLGraphics g)
	{
		OGLTextureResource[] allRes = new OGLTextureResource[textureBank.size()];
		textureBank.getAllKeys(allRes);
		destroyTextures(allRes);
		queueTextures(allRes);
	}

	/**
	 * Reloads all textures cached in this renderer.
	 */
	protected void reloadShaders(OGLGraphics g)
	{
		OGLShaderResource[] allRes = new OGLShaderResource[shaderBank.size()];
		shaderBank.getAllKeys(allRes);
		destroyShaders(allRes);
		queueShaders(allRes);
	}

	/**
	 * Destroys textures that need to be destroyed and loads textures
	 * that need to be loaded.
	 */
	protected void cycleTextures(OGLGraphics g)
	{
		while (!textureDestroyQueue.isEmpty())
			purgeTexture(g, textureDestroyQueue.dequeue());
	
		while (!textureCacheQueue.isEmpty())
			cacheTexture(g, textureCacheQueue.dequeue());
	}

	/**
	 * Destroys shaders that need to be destroyed and loads shaders
	 * that need to be loaded.
	 */
	protected void cycleShaders(OGLGraphics g)
	{
		while (!shaderDestroyQueue.isEmpty())
			purgeShader(g, shaderDestroyQueue.dequeue());
	
		while (!shaderCacheQueue.isEmpty())
			cacheShader(g,shaderCacheQueue.dequeue());
	}

	/**
	 * Deletes a texture resource from OpenGL's memory and from the loader banks.
	 */
	protected void purgeTexture(OGLGraphics g, OGLTextureResource res)
	{
		if (res.isRenderTarget())
		{
			RenderTarget rt = targetBank.removeByKey(res);
			DepthBuffer db = rt.depthBuffer;
			db.renderTargetList.remove(rt);
			if (db.renderTargetList.size() == 0)
				db.destroy(g);
			rt.depthBuffer = null;
			textureBank.removeByKey(res);
			rt.destroy(g);
			fireTextureDestroyedEvent(res);
		}
		else
		{
			OGLTexture2D obj = textureBank.removeByKey(res);
			if (obj != null)
			{
				obj.destroy(g);
				fireTextureDestroyedEvent(res);
			}
		}
	}
	
	/**
	 * Deletes a shader resource from OpenGL's memory and from the loader banks.
	 */
	protected void purgeShader(OGLGraphics g, OGLShaderResource res)
	{
		OGLShaderProgram obj = shaderBank.removeByKey(res);
		if (obj != null)
		{
			obj.destroy(g);
			fireShaderDestroyedEvent(res);
		}
	}
	
	/**
	 * Figures out the texture's internal format.
	 */
	protected InternalFormat decideInternalFormat(OGLTextureResource textureDef)
	{
		if (textureDef.isHeightMap())
		{
			if (textureDef.isNotAlpha())
				return InternalFormat.LUMINANCE;
			else
				return InternalFormat.INTENSITY;
		}
		else if (textureDef.isNotCompressable() || textureDef.isRenderTarget() || !compressTextures)
		{
			if (force32BitTextures)
				return InternalFormat.RGBA8;
			else if (textureDef.isNotAlpha())
				return InternalFormat.RGB;
			else
				return InternalFormat.RGBA;
		}
		else
		{
			if (textureDef.isNotAlpha())
				return InternalFormat.COMPRESSED_RGB_DXT1;
			else
				return InternalFormat.COMPRESSED_RGBA_DXT5;
		}
	}

	/**
	 * Figures out the texture's minification filter.
	 */
	protected MinFilter decideMinificationFilter(OGLTextureResource textureDef)
	{
		if (forceNearest || textureDef.isForcedNearest())
			return MinFilter.NEAREST;
		else if (forceNoMipmap || textureDef.isNotMipmapped() || textureDef.isRenderTarget())
			return MinFilter.LINEAR;
		else if (!textureBestFiltering)
			return MinFilter.BILINEAR;
		else 
			return MinFilter.TRILINEAR;
	}

	/**
	 * Figures out the texture's magnification filter.
	 */
	protected MagFilter decideMagnificationFilter(OGLTextureResource textureDef)
	{
		if (forceNearest || textureDef.isForcedNearest())
			return MagFilter.NEAREST;
		else
			return MagFilter.LINEAR;
	}

	/**
	 * Reads the texture's data. Calls openStreamForTextureData()
	 * to get the necessary data.
	 */
	protected BufferedImage getTextureImage(OGLGraphics g, OGLTextureResource textureDef) throws IOException
	{
		String tpath = textureDef.getPath();
		Dimension dim = textureDef.getDimension();
		if (tpath == null || tpath.trim().length() == 0)
		{
			if (dim != null)
				return new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
			else if (textureDef.isRenderTarget()) // render target and no size specified.
			{
				int p2width = RMath.closestPowerOfTwo((int)g.getCanvasWidth());
				int p2height = RMath.closestPowerOfTwo((int)g.getCanvasHeight());
				return new BufferedImage(p2width, p2height, BufferedImage.TYPE_INT_ARGB);
			}
			else
				return new BufferedImage(0, 0, BufferedImage.TYPE_INT_ARGB);
		}
		
		InputStream in = openStreamForTextureData(textureDef);
		if (in == null)
			return null;
		try {
			BufferedImage inImage = ImageIO.read(in);
			
			if (textureDef.getDimension() == null)
				return inImage;
			
			if (textureDef.isForcedNearest() || forceNearest)
				return OGLGraphicUtils.performResize(inImage, dim.width, dim.height);
			else if (textureBestFiltering)
				return OGLGraphicUtils.performResizeTrilinear(inImage, dim.width, dim.height);
			else
				return OGLGraphicUtils.performResizeBilinear(inImage, dim.width, dim.height);
		} catch (IOException e) {
			throw e;
		} finally {
			in.close();
		}
	}

	/**
	 * Fires the "texture added" event to all bound listeners.
	 * Called when a texture was added to a queue (but not cached yet).
	 * @param resource	the handle to the resource that was added.
	 */
	protected void fireTextureAddedEvent(OGLTextureResource resource)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).textureAdded(resource);
	}
	
	/**
	 * Fires the "texture cached" event to all bound listeners.
	 * Called when a texture was cached by the loader.
	 * @param resource	the handle to the resource that was cached.
	 * @param texture the texture object that was cached successfully.
	 */
	protected void fireTextureCachedEvent(OGLTextureResource resource, OGLTexture texture)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).textureCached(resource, texture);
	}
	
	/**
	 * Fires the "texture touched" event to all bound listeners.
	 * Called when a cached texture was called for caching but already was in the loader.
	 * Touching a texture un-flags it in the bank.
	 * @param resource	the handle to the resource that was touched.
	 */
	protected void fireTextureTouchedEvent(OGLTextureResource resource)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).textureTouched(resource);
	}
	
	/**
	 * Fires the "texture cache load error" event to all bound listeners.
	 * Called when texture's data could not be loaded.
	 * @param resource	the handle to the resource that generated the error.
	 * @param exception	the exception that occurred.
	 */
	protected void fireTextureCacheLoadErrorEvent(OGLTextureResource resource, IOException exception)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).textureCacheLoadError(resource, exception);
	}
	
	/**
	 * Fires the "texture cache graphic error" event to all bound listeners.
	 * Called when texture's data could not be loaded into OpenGL for some reason.
	 * @param resource	the handle to the resource that generated the error.
	 * @param exception	the exception that occurred.
	 */
	protected void fireTextureCacheGraphicErrorEvent(OGLTextureResource resource, GraphicsException exception)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).textureCacheGraphicError(resource, exception);
	}
	
	/**
	 * Fires the "texture destroyed" event to all bound listeners.
	 * Called when a texture was destroyed (unloaded from OpenGL) by the loader.
	 * @param resource	the handle to the resource that was destroyed.
	 */
	protected void fireTextureDestroyedEvent(OGLTextureResource resource)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).textureDestroyed(resource);
	}
	
	/**
	 * Fires the "shader added" event to all bound listeners.
	 * Called when a shader was added to a queue (but not cached yet).
	 * @param resource	the handle to the resource that was added.
	 */
	protected void fireShaderAddedEvent(OGLShaderResource resource)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).shaderAdded(resource);
	}
	
	/**
	 * Fires the "shader cached" event to all bound listeners.
	 * Called when a shader was cached by the loader.
	 * @param resource	the handle to the resource that was cached.
	 * @param shader the shader that was successfully cached.
	 */
	protected void fireShaderCachedEvent(OGLShaderResource resource, OGLShaderProgram shader)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).shaderCached(resource, shader);
	}
	
	/**
	 * Fires the "shader touched" event to all bound listeners.
	 * Called when a cached shader was called for caching but already was in the loader.
	 * Touching a shader un-flags it in the bank.
	 * @param resource	the handle to the resource that was touched.
	 */
	protected void fireShaderTouchedEvent(OGLShaderResource resource)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).shaderTouched(resource);
	}
	
	/**
	 * Fires the "shader cache load error" event to all bound listeners.
	 * Called when shader's data could not be loaded.
	 * @param resource	the handle to the resource that generated the error.
	 * @param exception	the exception that occurred.
	 */
	protected void fireShaderCacheLoadErrorEvent(OGLShaderResource resource, IOException exception)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).shaderCacheLoadError(resource, exception);
	}
	
	/**
	 * Fires the "shader cache graphic error" event to all bound listeners.
	 * Called when shader's data could not be loaded into OpenGL for some reason.
	 * @param resource	the handle to the resource that generated the error.
	 * @param exception	the exception that occurred.
	 */
	protected void fireShaderCacheGraphicErrorEvent(OGLShaderResource resource, GraphicsException exception)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).shaderCacheGraphicError(resource, exception);
	}
	
	/**
	 * Fires the "shader destroyed" event to all bound listeners.
	 * Called when a shader was destroyed (unloaded from OpenGL) by the loader.
	 * @param resource	the handle to the resource that was destroyed.
	 */
	protected void fireShaderDestroyedEvent(OGLShaderResource resource)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).shaderDestroyed(resource);
	}
	
	/**
	 * Fires the "other graphic error" event to all bound listeners.
	 * Called when an any error error occurred when something was to be loaded into OpenGL.
	 * @param exception	the exception that occurred.
	 */
	protected void fireOtherGraphicErrorEvent(GraphicsException exception)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).otherLoaderGraphicError(exception);
	}

	/**
	 * Fires the "other I/O error" event to all bound listeners.
	 * Called when an any error error occurred when something was to be reloaded.
	 * @param exception	the exception that occurred.
	 */
	protected void fireOtherIOErrorEvent(IOException exception)
	{
		for (int i = 0; i < resourceListeners.size(); i++)
			resourceListeners.getByIndex(i).otherLoaderIOError(exception);
	}

	/**
	 * Framebuffer-and-depth buffer combo object that uses a texture
	 * as a target. Utility classes that have access to an OGLResourceLoader
	 * can potentially render their contents to a RenderTarget and use the
	 * result as a texture, seamlessly.
	 */
	protected class RenderTarget
	{
		/** Frame buffer objects for post-processing effects. */
		OGLFrameBuffer frameBufferObject;
		/** Frame buffer objects for depth buffer effects. */
		OGLTexture2D colorBufferTexture;
		/** The depth buffer attached to this. */
		DepthBuffer depthBuffer;
		
		/**
		 * Creates a new render target using an OGLGraphics context. 
		 */
		RenderTarget(OGLGraphics g, OGLTexture2D texture)
		{
			try {
				frameBufferObject = new OGLFrameBuffer(g);
				colorBufferTexture = texture;
				frameBufferObject.attachToTexture2D(g, AttachPoint.COLOR0, texture);
			} catch (GraphicsException e) {
				if (frameBufferObject != null)
				{
					frameBufferObject.destroy(g);
					frameBufferObject = null;
				}
				if (colorBufferTexture != null)
				{
					colorBufferTexture.destroy(g);
					colorBufferTexture = null;
				}
				throw e;
			}
		}

		/**
		 * Returns the reference to the frame buffer object used for binding
		 * the render target.
		 */
		public OGLFrameBuffer getFrameBufferObject()
		{
			return frameBufferObject;
		}

		/**
		 * Returns the reference to the texture object used for binding
		 * the render target's texture to a context.
		 */
		public OGLTexture2D getColorBufferTexture()
		{
			return colorBufferTexture;
		}

		/**
		 * Frees the memory used by this RenderTarget.
		 * THIS WILL NOT UNBIND THE CURRENT FRAMEBUFFER, 
		 * SHOULD IT BE THE ONE GETTING DESTROYED.
		 */
		public void destroy(OGLGraphics g)
		{
			g.unbindFrameBuffer();
			frameBufferObject.detachFromRenderBuffer(g, AttachPoint.DEPTH);
			frameBufferObject.detachFromTexture2D(g, AttachPoint.COLOR0);
			colorBufferTexture.destroy(g);
			frameBufferObject.destroy(g);
		}
		
	}
	
	/**
	 * Individual depth buffer binding for render targets.
	 */
	protected class DepthBuffer
	{
		/** List of render target that this belongs to. */
		Queue<RenderTarget> renderTargetList;
		/** Frame buffer objects for depth buffer effects. */
		OGLFrameRenderBuffer depthBuffer;

		DepthBuffer(OGLGraphics g, int width, int height)
		{
			try {
				renderTargetList = new Queue<RenderTarget>();
				depthBuffer = new OGLFrameRenderBuffer(g, Format.DEPTH, width, height);
				g.unbindFrameBuffer();
			} catch (GraphicsException e) {
				if (depthBuffer != null)
				{
					depthBuffer.destroy(g);
					depthBuffer = null;
				}
				throw e;
			}
		}

		/**
		 * Returns the reference to the depth buffer object used for
		 * depth tests.
		 */
		public OGLFrameRenderBuffer getDepthBuffer()
		{
			return depthBuffer;
		}
		
		/**
		 * Frees the memory used by this DepthBuffer.
		 * THIS WILL NOT UNBIND THE CURRENT FRAMEBUFFER, 
		 * SHOULD IT BE THE ONE GETTING DESTROYED.
		 */
		public void destroy(OGLGraphics g)
		{
			depthBuffer.destroy(g);
		}

	}
	
}
