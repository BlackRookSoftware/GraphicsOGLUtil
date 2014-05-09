/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.resource;

import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.ogl.object.shader.uniform.OGLUniform;

/**
 * A shader resource handler for OGLResourceLoader.
 * @author Matthew Tropiano
 */
public interface OGLShaderResource
{
	/**
	 * Gets the name of this resource.
	 */
	public String getName();
	
	/**
	 * Gets the locator path of the vertex program handle.
	 * Usually this is a file path or URI or classpath or something.
	 * If null, the program is not loaded.
	 */
	public String getVertexPath();
	
	/**
	 * Gets the locator path of the geometry program handle.
	 * Usually this is a file path or URI or classpath or something.
	 * If null, the program is not loaded.
	 */
	public String getGeometryPath();
	
	/**
	 * Gets the locator path of the fragment program handle.
	 * Usually this is a file path or URI or classpath or something.
	 * If null, the program is not loaded.
	 */
	public String getFragmentPath();
	
	/**
	 * Gets the list of uniforms that this resource uses.
	 */
	public Queue<OGLUniform> getUniforms();
	
}
