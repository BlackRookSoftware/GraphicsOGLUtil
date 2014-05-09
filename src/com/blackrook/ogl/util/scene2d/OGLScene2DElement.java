/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.scene2d;

import com.blackrook.ogl.mesh.MeshView;
import com.blackrook.ogl.util.OGLSkin;

/**
 * Interface for describing objects to be drawn in a
 * graphics environment. 
 * @author Matthew Tropiano
 */
public interface OGLScene2DElement
{
	/**
	 * Returns this object's skin.
	 */
	public OGLSkin getSkin();

	/**
	 * Returns this object's drawable mesh.
	 */
	public MeshView getMeshView();

	/**
	 * Gets the object's rendering position, x-axis.
	 */
	public float getRenderPositionX();

	/**
	 * Gets the object's rendering position, y-axis.
	 */
	public float getRenderPositionY();

	/**
	 * Gets the object's rendering position, z-axis.
	 */
	public float getRenderPositionZ();

	/**
	 * Gets the object's rendering half-width.
	 */
	public float getRenderHalfWidth();

	/**
	 * Gets the object's rendering half-height.
	 */
	public float getRenderHalfHeight();

	/**
	 * Gets the object's rendering half-depth.
	 */
	public float getRenderHalfDepth();

	/**
	 * Gets the object's rotation in degrees, z-axis.
	 */
	public float getRenderRotationZ();

	/**
	 * Gets the object's radius, for use with determining what is on the screen or not.
	 * <p>
	 * Since this could be an expensive call, this 
	 * is not always used - it is used if the object's useRenderRadius() 
	 * function returns true, which leaves it in the hands of the implementor.
	 */
	public float getRenderRadius();

	/**
	 * Should the object's radius value be used for collision, with the camera,
	 * rather than its half-height or half-width?
	 */
	public boolean useRenderRadius();
	
	/**
	 * Gets the object's skin scaling, S-axis.
	 * Should, in most cases, return 1f.
	 */
	public float getSkinScaleS();

	/**
	 * Gets the object's skin scaling, T-axis.
	 * Should, in most cases, return 1f.
	 */
	public float getSkinScaleT();

	/**
	 * Gets the object's red channel value.
	 */
	public float getRed();

	/**
	 * Gets the object's green channel value.
	 */
	public float getGreen();

	/**
	 * Gets the object's blue channel value.
	 */
	public float getBlue();

	/**
	 * Gets the object's alpha channel value.
	 */
	public float getAlpha();

	/**
	 * Gets if the object is visible. This affects whether or not
	 * the renderer attempts to draw this object.
	 */
	public boolean isVisible();

}
