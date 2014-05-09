/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.resource;

import java.io.File;

/**
 * An object for using files to use as shader program resources.
 * @author Matthew Tropiano
 */
public class OGLShaderFile extends OGLShaderResourceAbstract
{
	private File vertexFile;
	private File geometryFile;
	private File fragmentFile;

	/**
	 * Creates a new shader using a vertex program. 
	 * @param vertexPath the file path to the vertex program.  
	 */
	public OGLShaderFile(String vertexPath)
	{
		this(new File(vertexPath), null, null);
	}
	
	/**
	 * Creates a new shader using a vertex and fragment program. 
	 * @param vertexPath the file path to the vertex program.  
	 * @param fragmentPath the file path to the fragment program.
	 */
	public OGLShaderFile(String vertexPath, String fragmentPath)
	{
		this(new File(vertexPath), null, new File(fragmentPath));
	}
	
	/**
	 * Creates a new shader using a vertex, geometry, and fragment program. 
	 * @param vertexPath the file path to the vertex program.  
	 * @param geometryPath the file path to the geometry program.
	 * @param fragmentPath the file path to the fragment program.
	 */
	public OGLShaderFile(String vertexPath, String geometryPath, String fragmentPath)
	{
		this(new File(vertexPath), new File(geometryPath), new File(fragmentPath));
	}
	
	/**
	 * Creates a new shader using a vertex program. 
	 * @param vertexFile the file path to the vertex program.  
	 */
	public OGLShaderFile(File vertexFile)
	{
		this.vertexFile = vertexFile;
	}

	/**
	 * Creates a new shader using a vertex and fragment program. 
	 * @param vertexFile the file path to the vertex program.  
	 * @param fragmentFile the file path to the fragment program.
	 */
	public OGLShaderFile(File vertexFile, File fragmentFile)
	{
		this.vertexFile = vertexFile;
		this.fragmentFile = fragmentFile;
	}

	/**
	 * Creates a new shader using a vertex, geometry, and fragment program. 
	 * @param vertexFile the file path to the vertex program.  
	 * @param geometryFile the file path to the geometry program.
	 * @param fragmentFile the file path to the fragment program.
	 */
	public OGLShaderFile(File vertexFile, File geometryFile, File fragmentFile)
	{
		this.vertexFile = vertexFile;
		this.geometryFile = geometryFile;
		this.fragmentFile = fragmentFile;
	}

	@Override
	public String getName()
	{
		return vertexFile.getPath().substring(0, vertexFile.getPath().lastIndexOf(".")).replaceAll("\\\\", "/");
	}
	
	@Override
	public String getVertexPath()
	{
		return vertexFile != null ? vertexFile.getAbsolutePath() : null;
	}

	@Override
	public String getGeometryPath()
	{
		return geometryFile != null ? geometryFile.getAbsolutePath() : null;
	}

	@Override
	public String getFragmentPath()
	{
		return fragmentFile != null ? fragmentFile.getAbsolutePath() : null;
	}

}
