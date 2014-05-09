/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.resource;

import java.awt.Dimension;

import com.blackrook.ogl.object.texture.OGLTexture;

/**
 * A texture resource handle for OGLResourceLoader.
 * @author Matthew Tropiano
 */
public interface OGLTextureResource
{
	/**
	 * Gets the name of this resource.
	 */
	public String getName();

	/**
	 * Gets the locator path of this handle.
	 * Usually this is a file path or URI or classpath or something.
	 */
	public String getPath();
	
	/**
	 * Gets the dimensions of the texture definition.
	 * If the path is specified, the image will be scaled to the specified
	 * pixel dimensions before being uploaded to texture memory.
	 * If the path is unspecified, a blank image will be uploaded using 
	 * the specified pixel dimensions.
	 * A null dimension will not utilize scaling.
	 */
	public Dimension getDimension();
	
	/**
	 * Is this texture not meant to be compressed at all, even when it is turned on?
	 */
	public boolean isNotCompressable();

	/**
	 * If this texture is to be filtered somehow, should mipmaps NOT be made for this texture?
	 */
	public boolean isNotMipmapped();

	/**
	 * Is this texture a normal map, subject to either a different compression method or none at all
	 * depending on available hardware extensions (has no impact on it being rendered differently,
	 * this is just a hint for the engine)?
	 */
	public boolean isNormalMap();

	/**
	 * Is this texture a height map, able to be stored as a luminance/intensity map? 
	 * This has no impact on it being rendered differently other than as grayscale.
	 */
	public boolean isHeightMap();

	/**
	 * Does this texture not have an alpha channel?
	 */
	public boolean isNotAlpha();

	/**
	 * Is this texture not supposed to be filtered in any way (kept NEAREST filtered)?
	 */
	public boolean isForcedNearest();
	
	/**
	 * Is this texture a surrogate for a render buffer?
	 * Render Target resources go through a different process for caching,
	 * and are viable resources to use for an alternate rendering target.
	 */
	public boolean isRenderTarget();
	
	/**
	 * If this is a render target surrogate, is this using a shared
	 * depth buffer with another target? If so, those targets should share
	 * the same id.
	 */
	public int getDepthId();
	
	/**
	 * Gets the border thickness in texels.
	 */
	public int getBorder();

	/**
	 * Gets the initial wrapping mode for this texture's S-axis.
	 */
	public OGLTexture.WrapType getWrappingModeS();

	/**
	 * Gets the initial wrapping mode for this texture's T-axis.
	 */
	public OGLTexture.WrapType getWrappingModeT();

}
