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
 * Adapter class for OGLResourceLoaderListener.
 * All methods do nothing unless overridden.
 * @author Matthew Tropiano
 */
public class OGLResourceLoaderAdapter implements OGLResourceLoaderListener
{

	@Override
	public void shaderAdded(OGLShaderResource handle)
	{
	}

	@Override
	public void shaderCacheGraphicError(OGLShaderResource handle, GraphicsException exception)
	{
	}

	@Override
	public void shaderCacheLoadError(OGLShaderResource handle, IOException exception)
	{
	}

	@Override
	public void shaderCached(OGLShaderResource handle, OGLShaderProgram shader)
	{
	}

	@Override
	public void shaderTouched(OGLShaderResource handle)
	{
	}

	@Override
	public void shaderDestroyed(OGLShaderResource handle)
	{
	}

	@Override
	public void textureAdded(OGLTextureResource handle)
	{
	}

	@Override
	public void textureCacheGraphicError(OGLTextureResource handle, GraphicsException exception)
	{
	}

	@Override
	public void textureCacheLoadError(OGLTextureResource handle, IOException exception)
	{
	}

	@Override
	public void textureCached(OGLTextureResource handle, OGLTexture texture)
	{
	}

	@Override
	public void textureDestroyed(OGLTextureResource handle)
	{
	}

	@Override
	public void textureTouched(OGLTextureResource handle)
	{
	}

	@Override
	public void otherLoaderGraphicError(GraphicsException exception)
	{
	}

	@Override
	public void otherLoaderIOError(IOException exception)
	{
	}

}
