/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util;

/**
 * Listener interface for camera changes.
 * @author Matthew Tropiano
 */
public interface OGL2DCameraListener
{
	/**
	 * Called on camera change.
	 * @param changeX camera's change in x.
	 * @param changeY camera's change in Y.
	 * @param changeWidth camera's change in width.
	 * @param changeHeight camera's change in height.
	 */
	public void onCameraChange(float changeX, float changeY, float changeWidth, float changeHeight);

}
