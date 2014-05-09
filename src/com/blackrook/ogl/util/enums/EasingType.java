/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.enums;

public interface EasingType
{
	/**
	 * Samples this easing to get the final output value for interpolation.
	 * An input time of 0f should return 0f. An input time of 1f should return 1f.  
	 * @param time the input time (between 0 and 1, inclusively).
	 */
	public float getSample(float time);
	
}

