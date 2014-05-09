/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.tile2d;

/**
 * Adapter class for OGLGridListener.
 * @author Matthew Tropiano
 */
public class OGLTile2DAdapter implements OGLTile2DListener
{

	@Override
	public void gridKeyPress(int keycode, int gridX, int gridY)
	{
		// Do nothing.
	}

	@Override
	public void gridKeyRelease(int keycode, int gridX, int gridY)
	{
		// Do nothing.
	}

	@Override
	public void gridKeyTyped(int keycode, int gridX, int gridY)
	{
		// Do nothing.
	}

	@Override
	public void gridMouseOver(int gridX, int gridY)
	{
		// Do nothing.
	}

	@Override
	public void gridMouseLeave(int gridX, int gridY)
	{
		// Do nothing.
	}

	@Override
	public void gridMouseClick(int button, int gridX, int gridY)
	{
		// Do nothing.
	}

	@Override
	public void gridMouseDrag(int button, int gridX, int gridY,
			float unitsX, float positionX, float unitsY, float positionY)
	{
		// Do nothing.
	}

	@Override
	public void gridMousePress(int button, int gridX, int gridY)
	{
		// Do nothing.
	}

	@Override
	public void gridMouseRelease(int button, int gridX, int gridY)
	{
		// Do nothing.
	}

	@Override
	public void gridMouseWheel(int units, int gridX, int gridY)
	{
		// Do nothing.
	}

	@Override
	public void gridFocus()
	{
		// Do nothing.
	}

	@Override
	public void gridUnfocus()
	{
		// Do nothing.
	}

	@Override
	public void gridMouseEnter()
	{
		// Do nothing.
	}

	@Override
	public void gridMouseExit()
	{
		// Do nothing.
	}

}
