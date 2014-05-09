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
import com.blackrook.ogl.object.shader.uniform.OGLUniformWave;

/**
 * Abstract shader resource with methods to set attributes.
 * @author Matthew Tropiano
 */
public abstract class OGLShaderResourceAbstract implements OGLShaderResource
{
	/** The list of uniforms. */
	private Queue<OGLUniform> uniformList;
	
	/** Default constructor. */
	protected OGLShaderResourceAbstract()
	{
		uniformList = new Queue<OGLUniform>();
	}
	
	/**
	 * Adds a shader uniform to this resource.
	 * Figures out by the class type whether or not to
	 * add it to the varying list or the regular list
	 * (if the class is an instance of {@link OGLUniformWave}, 
	 * it is added to the varying list).
	 */
	public void addUniform(OGLUniform uniform)
	{
		uniformList.add(uniform);
	}
	
	@Override
	public Queue<OGLUniform> getUniforms()
	{
		return uniformList;
	}
	
}
