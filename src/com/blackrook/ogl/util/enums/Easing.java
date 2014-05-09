/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.enums;

import com.blackrook.commons.math.RMath;

/**
 * An enumeration of base easing types for actions. 
 */
public enum Easing implements EasingType
{
	LINEAR
	{
		@Override
		public float getSample(float time)
		{
			time = RMath.clampValue(time, 0f, 1f);
			return time;
		}
	},
	EASE_IN
	{
		@Override
		public float getSample(float time)
		{
			time = RMath.clampValue(time, 0f, 1f);
			return 1f - ((float)Math.pow(1f-time, 3));
		}
	},
	EASE_OUT
	{
		@Override
		public float getSample(float time)
		{
			time = RMath.clampValue(time, 0f, 1f);
			return (float)Math.pow(time, 3);
		}
	},
	EASE_IN_AND_OUT
	{
		@Override
		public float getSample(float time)
		{
			time = RMath.clampValue(time, 0f, 1f);
			time = time * 2;
            if (time < 1) {
                return (float)Math.pow(time, 3) / 2;
            }
            time -= 2;
            return ((float)Math.pow(time, 3) + 2) / 2;
		}
	},
	BOUNCE
	{
		@Override
		public float getSample(float time)
		{
			time = RMath.clampValue(time, 0f, 1f);
            float s = 7.5625f;
            float p = 2.75f;
            float out = 0f;
	        if (time < (1 / p))
	        {
	            out = s * time * time;
	        } 
	        else
	        {
	            if (time < (2 / p))
	            {
	                time -= (1.5 / p);
	                out = s * time * time + .75f;
	            } 
	            else
	            {
	                if (time < (2.5 / p))
	                {
	                    time -= (2.25 / p);
	                    out = s * time * time + .9375f;
	                } 
	                else
	                {
	                    time -= (2.625 / p);
	                    out = s * time * time + .984375f;
	                }
	            }
	        }
	        return out;
		}
	},
	ELASTIC
	{
		@Override
		public float getSample(float time)
		{
			time = RMath.clampValue(time, 0f, 1f);
            if (time == 0 || time == 1)
            {
                return time;
            }
            float p = 0.3f;
            float s = p / 4;
            return (float)(Math.pow(2, -10 * time) * Math.sin((time - s) * (2 * Math.PI) / p) + 1);
		}
	},
	BACK_IN
	{
		@Override
		public float getSample(float time)
		{
			time = RMath.clampValue(time, 0f, 1f);
            float s = 1.70158f;
            return time * time * ((s + 1) * time - s);
		}
	},
	BACK_OUT
	{
		@Override
		public float getSample(float time)
		{
			time = RMath.clampValue(time, 0f, 1f);
            time = time - 1;
            float s = 1.70158f;
            return time * time * ((s + 1) * time + s) + 1;
		}
	},
	;

}
