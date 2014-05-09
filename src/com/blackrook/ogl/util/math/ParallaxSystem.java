/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.math;

import com.blackrook.commons.list.List;

/**
 * A parallaxing system used for calculating parallax for 2D environments.
 * The system is made up a list of nodes, all which have their own biases for translation and rotation.
 * @author Matthew Tropiano
 */
public class ParallaxSystem extends List<ParallaxNode>
{
	/**
	 * Creates a new ParallaxSystem.
	 */
	public ParallaxSystem()
	{
		super(7);
	}
	
	/**
	 * Parallaxes the environment X units along the X axis.
	 * Calls doTranslateX(-x) and doRotateY(x), which will move the "camera" left or right.
	 */
	public synchronized void parallaxX(double x)
	{
		for (ParallaxNode node : this)
		{
			node.doTranslateX(-x);
			node.doRotateY(x);
		}
	}

	/**
	 * Parallaxes the environment Y units along the Y axis.
	 * Calls doTranslateY(-y) and doRotateX(y), which will move the "camera" up or down.
	 */
	public synchronized void parallaxY(double y)
	{
		for (ParallaxNode node : this)
		{
			node.doTranslateY(-y);
			node.doRotateX(y);
		}
	}
	
	/**
	 * Sets the X parallax of all nodes, as if translated/rotated from the origin.
	 */
	public synchronized void setParallaxX(double x)
	{
		for (ParallaxNode node : this)
		{
			node.setTranslateX(0);
			node.setRotateY(0);
		}
		parallaxX(x);
	}
	
	/**
	 * Sets the Y parallax of all nodes, as if translated/rotated from the origin.
	 */
	public synchronized void setParallaxY(double y)
	{
		for (ParallaxNode node : this)
		{
			node.setTranslateY(0);
			node.setRotateX(0);
		}
		parallaxY(y);
	}
	
}
