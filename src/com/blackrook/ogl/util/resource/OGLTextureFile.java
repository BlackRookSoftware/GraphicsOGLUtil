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
 * A texture file to use as a texture resource handle.
 * @author Matthew Tropiano
 */
public class OGLTextureFile extends OGLTextureResourceAbstract
{
	private File file;
	
	public OGLTextureFile(String path)
	{
		this(new File(path));
	}
	
	public OGLTextureFile(File file)
	{
		super();
		this.file = file;
	}
	
	@Override
	public String getName()
	{
		return file.getPath().substring(0, file.getPath().lastIndexOf(".")).replaceAll("\\\\", "/");
	}
	
	@Override
	public String getPath()
	{
		return file.getAbsolutePath();
	}

}
