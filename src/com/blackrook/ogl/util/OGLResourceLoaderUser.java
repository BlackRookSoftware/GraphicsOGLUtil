/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util;

import com.blackrook.ogl.OGLCanvasNode;


/**
 * All classes that use a resource loader for storing/retrieving its graphics
 * data implement this class.
 * @author Matthew Tropiano
 */
public interface OGLResourceLoaderUser extends OGLCanvasNode
{
	/**
	 * Gets the resource loader to use for this canvas node.
	 */
	public OGLResourceLoader getResourceLoader();

	/**
	 * Sets the resource loader to use for this canvas node.
	 */
	public void setResourceLoader(OGLResourceLoader loader);

}
