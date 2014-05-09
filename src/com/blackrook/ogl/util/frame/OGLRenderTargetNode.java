/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.frame;

import com.blackrook.ogl.OGLGraphics;
import com.blackrook.ogl.node.OGLMultiNode;
import com.blackrook.ogl.util.OGLResourceLoader;
import com.blackrook.ogl.util.OGLResourceLoaderUser;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * A special multi layer node that encapsulates a series of drawable layers
 * for drawing into a render target.
 * @author Matthew Tropiano
 */
public class OGLRenderTargetNode extends OGLMultiNode implements OGLResourceLoaderUser
{
	/** Reference to Resource loader. */
	private OGLResourceLoader loader;
	/** The render target. */
	protected OGLTextureResource renderTarget;
	
	/**
	 * The default constructor.
	 * @param loader the {@link OGLResourceLoader} that contains the cached render target resource.
	 * @param renderTarget the texture target to use (its isRenderTarget() method must return TRUE).
	 */
	public OGLRenderTargetNode(OGLResourceLoader loader, OGLTextureResource renderTarget)
	{
		super();
		setResourceLoader(loader);
		this.renderTarget = renderTarget;
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
	public void preNodeDisplay(OGLGraphics g)
	{
		loader.startRenderTarget(g, renderTarget);
	}
	
	@Override
	public void postNodeDisplay(OGLGraphics g)
	{
		loader.endRenderTarget(g);
	}
	

}
