/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.resource;

import java.awt.Dimension;

import com.blackrook.ogl.object.texture.OGLTexture.WrapType;

/**
 * Abstract texture resource with methods to set attributes.
 * @author Matthew Tropiano
 */
public abstract class OGLTextureResourceAbstract implements OGLTextureResource
{
	private Dimension dimension;
	private boolean heightMap;
	private boolean notMipmapped;
	private boolean normalMap;
	private boolean notAlpha;
	private boolean notCompressable;
	private boolean forcedNearest;
	private boolean renderTarget;
	private int depthId;
	private int border;
	private WrapType wrappingTypeS;
	private WrapType wrappingTypeT;
	
	protected OGLTextureResourceAbstract()
	{
		setDimension(null);
		setHeightMap(false);
		setNotMipmapped(false);
		setNormalMap(false);
		setNotAlpha(false);
		setNotCompressable(false);
		setForcedNearest(false);
		setDepthId(0);
		setBorder(0);
		setWrappingModeS(WrapType.TILE);
		setWrappingModeT(WrapType.TILE);
	}
	
	/**
	 * Sets the dimensions of the texture.
	 * If the path is specified, the image will be scaled to the specified
	 * pixel dimensions before being uploaded to texture memory.
	 * If the path is unspecified, a blank image will be uploaded using 
	 * the specified pixel dimensions.
	 */
	public void setDimension(Dimension dimension)
	{
		this.dimension = dimension;
	}

	/**
	 * Sets if this texture is a height map, able to be stored as a luminance map (has no impact on it being rendered differently,
	 * this is just a hint for the loader).
	 */
	public void setHeightMap(boolean heightMap)
	{
		this.heightMap = heightMap;
	}

	/**
	 * Sets if this texture is to be filtered somehow, should mipmaps NOT be made for this texture.
	 */
	public void setNotMipmapped(boolean notMipmapped)
	{
		this.notMipmapped = notMipmapped;
	}

	/**
	 * Sets if this texture is a normal map, subject to either a different compression method or none at all
	 * depending on available hardware extensions (has no impact on it being rendered differently,
	 * this is just a hint for the loader).
	 */
	public void setNormalMap(boolean normalMap)
	{
		this.normalMap = normalMap;
	}

	/**
	 * Sets if this texture does not have an alpha channel.
	 * This affects its storage as well as how it is potentially compressed.
	 */
	public void setNotAlpha(boolean notAlpha)
	{
		this.notAlpha = notAlpha;
	}

	/**
	 * Sets if this texture is not meant to be compressed at all, even when it is turned on.
	 */
	public void setNotCompressable(boolean notCompressable)
	{
		this.notCompressable = notCompressable;
	}

	/**
	 * Is this texture a surrogate for a render buffer?
	 */
	public void setRenderTarget(boolean renderTarget)
	{
		this.renderTarget = renderTarget;
	}

	/**
	 * Sets if this texture is not supposed to be filtered in any way (kept NEAREST filtered).
	 */
	public void setForcedNearest(boolean forcedNearest)
	{
		this.forcedNearest = forcedNearest;
	}
	
	/**
	 * Sets the depth id for the render targets.
	 */
	public void setDepthId(int id)
	{
		this.depthId = id;
	}

	/**
	 * Sets the texture's border in texels.
	 */
	public void setBorder(int border)
	{
		this.border = border;
	}

	/**
	 * Sets the initial wrapping mode for this texture's S-axis.
	 */
	public void setWrappingModeS(WrapType wt)
	{
		wrappingTypeS = wt;
	}

	/**
	 * Sets the initial wrapping mode for this texture's T-axis.
	 */
	public void setWrappingModeT(WrapType wt)
	{
		wrappingTypeT = wt;
	}

	@Override
	public Dimension getDimension()
	{
		return dimension;
	}

	@Override
	public boolean isHeightMap()
	{
		return heightMap;
	}

	@Override
	public boolean isNotMipmapped()
	{
		return notMipmapped;
	}

	@Override
	public boolean isNormalMap()
	{
		return normalMap;
	}

	@Override
	public boolean isNotAlpha()
	{
		return notAlpha;
	}

	@Override
	public boolean isNotCompressable()
	{
		return notCompressable;
	}

	@Override
	public boolean isForcedNearest()
	{
		return forcedNearest;
	}

	@Override
	public boolean isRenderTarget()
	{
		return renderTarget;
	}

	@Override
	public int getDepthId()
	{
		return depthId;
	}
	
	@Override
	public int getBorder()
	{
		return border;
	}

	@Override
	public WrapType getWrappingModeS()
	{
		return wrappingTypeS;
	}

	@Override
	public WrapType getWrappingModeT()
	{
		return wrappingTypeT;
	}

}
