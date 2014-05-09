/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util;

import java.io.IOException;
import java.io.PrintStream;

import com.blackrook.ogl.exception.GraphicsException;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture;
import com.blackrook.ogl.util.resource.OGLShaderResource;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * A resource loader listener that outputs information to print streams
 * depending on {@link OGLResourceLoader} events.
 * @author Matthew Tropiano
 */
public class OGLResourceLoaderDebugListener implements OGLResourceLoaderListener
{
	/** Output stream. */
	private PrintStream out;
	/** Error Output stream. */
	private PrintStream err;

	/**
	 * Creates a new listener that outputs to the system outputs
	 * for regular output and error output.
	 */
	public OGLResourceLoaderDebugListener()
	{
		this(System.out, System.err);
	}
	
	/**
	 * Creates a new listener that outputs to specific {@link PrintStream}s
	 * for regular output and error output.
	 * @param out the output stream.
	 * @param err the error stream.
	 */
	public OGLResourceLoaderDebugListener(PrintStream out, PrintStream err)
	{
		this.out = out;
		this.err = err;
	}

	@Override
	public void otherLoaderGraphicError(GraphicsException exception)
	{
		err.println("OtherLoaderGraphicError: A graphics exception has occurred.");
		exception.printStackTrace(err);
	}

	@Override
	public void otherLoaderIOError(IOException exception)
	{
		err.println("OtherLoaderIOError: An I/O exception has occurred.");
		exception.printStackTrace(err);
	}

	@Override
	public void shaderAdded(OGLShaderResource handle)
	{
		out.printf("ShaderAdded: Shader was queued for loading: \"%s\"\n", handle.getName());
	}

	@Override
	public void shaderCacheGraphicError(OGLShaderResource handle, GraphicsException exception)
	{
		err.printf("ShaderCacheGraphicError: Shader could not be stored: \"%s\"\n", handle.getName());
		exception.printStackTrace(err);
	}

	@Override
	public void shaderCacheLoadError(OGLShaderResource handle, IOException exception)
	{
		err.printf("ShaderCacheLoadError: Shader could not be stored: \"%s\"\n", handle.getName());
		exception.printStackTrace(err);
	}

	@Override
	public void shaderCached(OGLShaderResource handle, OGLShaderProgram shader)
	{
		out.printf("ShaderCached: Shader was cached: \"%s\"\n", handle.getName());
		if (shader.getLog().length() > 0)
			out.printf("%s\n", shader.getLog());
		out.printf("\tID %d\n", shader.getGLId());
	}

	@Override
	public void shaderDestroyed(OGLShaderResource handle)
	{
		out.printf("ShaderDestroyed: Shader was destroyed: \"%s\"\n", handle.getName());
	}

	@Override
	public void shaderTouched(OGLShaderResource handle)
	{
		out.printf("ShaderTouched: Shader was touched: \"%s\"\n", handle.getName());
	}

	@Override
	public void textureAdded(OGLTextureResource handle)
	{
		out.printf("TextureAdded: Texture was queued for loading: \"%s\"\n", handle.getName());
	}

	@Override
	public void textureCacheGraphicError(OGLTextureResource handle, GraphicsException exception)
	{
		err.printf("TextureCacheGraphicError: Texture could not be stored: \"%s\"\n", handle.getName());
		exception.printStackTrace(err);
	}

	@Override
	public void textureCacheLoadError(OGLTextureResource handle, IOException exception)
	{
		err.printf("TextureCacheLoadError: Texture could not be stored: \"%s\"\n", handle.getName());
		exception.printStackTrace(err);
	}

	@Override
	public void textureCached(OGLTextureResource handle, OGLTexture texture)
	{
		out.printf("TextureCached: Texture was cached: \"%s\"\n", handle.getName());
		out.printf("\tID %d [%dx%dx%d], %d faces. Format: %s\n", 
			texture.getGLId(), 
			texture.getWidth(), texture.getHeight(), 
			texture.getDepth(), texture.getFaces(),
			texture.getInternalFormat().name()
			);
		out.printf("\tMIN: %s, MAG: %s, Aniso: %f, Border: %d, MIPMAP: %b\n", 
			texture.getMinFilteringMode().name(),
			texture.getMagFilteringMode().name(),
			texture.getAnisotropy(),
			texture.getBorder(), texture.generatesMipmaps()
			);
		out.printf("\tSize: ~%d bytes\n", 
			texture.getEstimatedSize()
			);
	}

	@Override
	public void textureDestroyed(OGLTextureResource handle)
	{
		out.printf("TextureDestroyed: Texture was destroyed: \"%s\"\n", handle.getName());
	}

	@Override
	public void textureTouched(OGLTextureResource handle)
	{
		out.printf("TextureTouched: Texture was touched: \"%s\"\n", handle.getName());
	}

}
