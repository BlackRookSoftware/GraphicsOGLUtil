/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.math;


/**
 * A data node to be used in a Parallax System.
 * This contains the numerical data needed for parallax information.
 * @author Matthew Tropiano
 */
public class ParallaxNode
{
	/** Parallax bias info, x-axis translation. */
	private double translateXBias;
	/** Parallax bias info, y-axis translation. */
	private double translateYBias;
	/** Parallax bias info, x-axis rotation. */
	private double rotateXBias;
	/** Parallax bias info, y-axis rotation. */
	private double rotateYBias;

	/** X translation. */
	private double translateX; 
	/** Y translation. */
	private double translateY; 
	/** X rotation. */
	private double rotateX; 
	/** Y rotation. */
	private double rotateY; 
	
	/**
	 * Creates a new ParallaxNode.
	 * All biases are 1, all translations and rotations are 0.
	 */
	public ParallaxNode()
	{
		this (1, 1);
	}

	/**
	 * Creates a new ParallaxNode.
	 * All biases are 1, all translations and rotations are 0.
	 * @param translateBias	the initial setting for all translation biases.
	 * @param rotateBias	the initial setting for all rotation biases.
	 */
	public ParallaxNode(double translateBias, double rotateBias)
	{
		translateXBias = translateBias;
		translateYBias = translateBias;
		translateX = 0;
		translateY = 0;
		rotateXBias = rotateBias;
		rotateYBias = rotateBias;
		rotateX = 0;
		rotateY = 0;
	}

	/**
	 * Translates this node on the x-axis.
	 * Final value is translateX + x * translateXBias.
	 */
	public void doTranslateX(double x)
	{
		translateX += x * translateXBias; 
	}
	
	/**
	 * Translates this node on the y-axis.
	 * Final value is translateY + y * translateYBias.
	 */
	public void doTranslateY(double y)
	{
		translateY += y * translateYBias; 
	}

	/**
	 * Rotates this node around the x-axis.
	 * Final value is rotateX + x * rotateXBias.
	 */
	public void doRotateX(double x)
	{
		rotateX += x * rotateXBias; 
	}
	
	/**
	 * Rotates this node around the y-axis.
	 * Final value is rotateY + y * rotateYBias.
	 */
	public void doRotateY(double y)
	{
		rotateY += y * rotateYBias; 
	}

	public final double getTranslateXBias()
	{
		return translateXBias;
	}

	public final double getTranslateYBias()
	{
		return translateYBias;
	}

	public final double getRotateXBias()
	{
		return rotateXBias;
	}

	public final double getRotateYBias()
	{
		return rotateYBias;
	}

	public final double getTranslateX()
	{
		return translateX;
	}

	public final double getTranslateY()
	{
		return translateY;
	}
	
	public final double getRotateX()
	{
		return rotateX;
	}

	public final double getRotateY()
	{
		return rotateY;
	}

	public final void setTranslateXBias(double translateXBias)
	{
		this.translateXBias = translateXBias;
	}

	public final void setTranslateYBias(double translateYBias)
	{
		this.translateYBias = translateYBias;
	}

	public final void setRotateXBias(double rotateXBias)
	{
		this.rotateXBias = rotateXBias;
	}

	public final void setRotateYBias(double rotateYBias)
	{
		this.rotateYBias = rotateYBias;
	}
	
	public final void setTranslateX(double translateX)
	{
		this.translateX = translateX;
	}

	public final void setTranslateY(double translateY)
	{
		this.translateY = translateY;
	}

	public final void setRotateX(double rotateX)
	{
		this.rotateX = rotateX;
	}

	public final void setRotateY(double rotateY)
	{
		this.rotateY = rotateY;
	}
	
	
	
}
