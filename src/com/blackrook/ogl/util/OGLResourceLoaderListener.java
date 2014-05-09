/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util;

import java.io.IOException;
import com.blackrook.ogl.exception.GraphicsException;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture;
import com.blackrook.ogl.util.resource.OGLShaderResource;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * Listener interface for OGLResourceLoader events.
 * @author Matthew Tropiano
 */
public interface OGLResourceLoaderListener
{
	/**
	 * Called when a texture was added to a queue (but not cached yet).
	 * @param handle	the handle to the resource that was added.
	 */
	public void textureAdded(OGLTextureResource handle);

	/**
	 * Called when a texture was cached.
	 * @param handle	the handle to the texture that was loaded.
	 */
	public void textureCached(OGLTextureResource handle, OGLTexture texture);

	/**
	 * Called when a texture was called for caching, but it was already added.
	 * @param handle	the handle to the texture that was touched.
	 */
	public void textureTouched(OGLTextureResource handle);

	/**
	 * Called when an error occurred when a texture was about to be loaded.
	 * @param handle	the handle to the texture that was the source of the error.
	 * @param exception the exception that occurred.
	 */
	public void textureCacheLoadError(OGLTextureResource handle, IOException exception);

	/**
	 * Called when an error occurred when a texture was about to be added to OpenGL.
	 * @param handle	the handle to the texture that was the source of the error.
	 * @param exception the exception that occurred.
	 */
	public void textureCacheGraphicError(OGLTextureResource handle, GraphicsException exception);

	/**
	 * Called when a texture was destroyed.
	 * @param handle	the handle to the texture that was destroyed.
	 */
	public void textureDestroyed(OGLTextureResource handle);
	
	/**
	 * Called when a shader was added to a queue (but not cached yet).
	 * @param handle	the handle to the resource that was added.
	 */
	public void shaderAdded(OGLShaderResource handle);

	/**
	 * Called when a shader was cached.
	 * @param handle	the handle to the shader that was loaded.
	 */
	public void shaderCached(OGLShaderResource handle, OGLShaderProgram shader);

	/**
	 * Called when a shader was called for caching, but it was already added.
	 * @param handle	the handle to the shader that was touched.
	 */
	public void shaderTouched(OGLShaderResource handle);

	/**
	 * Called when an error occurred when a shader was about to be loaded.
	 * @param handle	the handle to the shader that was the source of the error.
	 * @param exception the exception that occurred.
	 */
	public void shaderCacheLoadError(OGLShaderResource handle, IOException exception);

	/**
	 * Called when an error occurred when a shader was about to be added to OpenGL.
	 * @param handle	the handle to the shader that was the source of the error.
	 * @param exception the exception that occurred.
	 */
	public void shaderCacheGraphicError(OGLShaderResource handle, GraphicsException exception);

	/**
	 * Called when a shader was destroyed.
	 * @param handle	the handle to the shader that was destroyed.
	 */
	public void shaderDestroyed(OGLShaderResource handle);
	
	/**
	 * Called when an any error error occurred when something was to be reloaded.
	 * @param exception the exception that occurred.
	 */
	public void otherLoaderIOError(IOException exception);

	/**
	 * Called when an any error error occurred when something was to be initialized in OpenGL.
	 * @param exception the exception that occurred.
	 */
	public void otherLoaderGraphicError(GraphicsException exception);

	
}
