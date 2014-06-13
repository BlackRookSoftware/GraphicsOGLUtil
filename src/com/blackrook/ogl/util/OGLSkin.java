/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util;

import java.io.File;
import java.util.Iterator;

import com.blackrook.commons.Sizable;
import com.blackrook.commons.list.List;
import com.blackrook.commons.math.wave.Wave;
import com.blackrook.ogl.util.resource.OGLShaderResource;
import com.blackrook.ogl.util.resource.OGLTextureFile;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * A set of texture layer steps that tell the utility libraries
 * how to render something on geometry.
 * @author Matthew Tropiano
 */
public class OGLSkin implements Iterable<OGLSkin.Step>, Sizable
{
	private static final OGLTextureResource[] EMPTY_TEXTURE_LIST = new OGLTextureResource[0];

	/**
	 * Skin blending type.
	 */
	public static enum BlendType
	{
		ADD,
		MULTIPLY,
		REPLACE,
		ALPHA;
	}

	/** Steps. */
	private List<Step> steps;
	
	/** Creates a new skin with no steps. */
	public OGLSkin()
	{
		this(2);
	}
	
	/** 
	 * Creates a new skin with no steps.
	 * @param capacity initial list capacity. 
	 */
	public OGLSkin(int capacity)
	{
		steps = new List<Step>(capacity);
	}

	/**
	 * Creates a new skin with texture resources attached.
	 * @param resources the resources to attach as steps, in order.
	 */
	public OGLSkin(OGLTextureResource ... resources)
	{
		this(resources.length);
		for (OGLTextureResource res : resources)
		{
			Step s = new Step();
			s.setTextureList(res);
			add(s);
		}
	}
	
	/**
	 * Creates a new skin using references to files, added as steps.
	 * @param files the files to attach as steps, in order.
	 */
	public OGLSkin(File ... files)
	{
		this(files.length);
		for (File f : files)
		{
			Step s = new Step();
			s.setTextureList(new OGLTextureFile(f));
			add(s);
		}
	}
	
	/**
	 * Creates a new skin using file paths, added as steps.
	 * @param paths the file paths to attach as steps, in order.
	 */
	public OGLSkin(String ... paths)
	{
		this(paths.length);
		for (String p : paths)
		{
			Step s = new Step();
			s.setTextureList(new OGLTextureFile(p));
			add(s);
		}
	}
	
	/**
	 * Adds a step to this skin.
	 * @param step the step to add.
	 */
	public void add(Step step)
	{
		steps.add(step);
	}
	
	
	/**
	 * Gets a step from this skin.
	 * @param index the index of the step to retrieve.
	 * @return the desired Step or null if bad index.
	 */
	public Step get(int index)
	{
		return steps.getByIndex(index);
	}
	
	@Override
	public int size()
	{
		return steps.size();
	}
	
	@Override
	public boolean isEmpty()
	{
		return steps.isEmpty();
	}

	@Override
	public Iterator<Step> iterator()
	{
		return steps.iterator();
	}

	/**
	 * A rendering step for two-dimensional objects.
	 * @author Matthew Tropiano
	 */
	public static class Step
	{
		public static final int 
		OBJECT_X =			0,
		OBJECT_Y =			1,
		OBJECT_WIDTH =		2,
		OBJECT_HEIGHT =		3;

		public static final int 
		TEXCOORD_S =		0,
		TEXCOORD_T =		1,

		TEXPLANE_A =		0,
		TEXPLANE_B =		1,
		TEXPLANE_C =		2,
		TEXPLANE_D =		3;

		public static final int 
		COLOR_R =			0,
		COLOR_G =			1,
		COLOR_B =			2,
		COLOR_A =			3;

		public static final int 
		TEXGEN_NONE =		0,
		TEXGEN_EYE =		1,
		TEXGEN_OBJECT =		2;
	
		/** Texture objects. */
		private OGLTextureResource[] textureList;
		/** Treat texture list as unit list? */
		private boolean multitexture;
		/** Shader objects. */
		private OGLShaderResource shaderProgram;
		/** This object's blending type. */
		private BlendType blendType;
		/** Color coordinates. */
		private float[][] colorCoords;
		/** Texture rotation coordinates. */
		private float[] textureRotation;
		/** This object's texture rotation pivot point. */
		private float[] textureRotationPivot;
		/** Texture coordinates. */
		private float[][][] textureCoords;
		/** The texture generation type, s-axis. */
		private int texGenS;
		/** The texture generation type, t-axis. */
		private int texGenT;

		/** Color wave. */
		private Wave colorWave;
		/** Texture index wave. */
		private Wave textureIndexWave;
		/** Texture rotation wave. */
		private Wave textureRotationWave;
		/** Texture S-coordinate wave. */
		private Wave textureSWave;
		/** Texture T-coordinate wave. */
		private Wave textureTWave;

		/**
		 * Creates a render step.
		 */
		public Step()
		{
			colorCoords = new float[][]{{1,1,1,1}, {1,1,1,1}};
			setShaderProgram(null);
			setTextureList(EMPTY_TEXTURE_LIST);
			setMultitexture(false);
			setBlendType(BlendType.ALPHA);
			setTexGenS(TEXGEN_NONE);
			setTexGenT(TEXGEN_NONE);
			textureRotationPivot = new float[]{0,0};
			textureRotation = new float[]{0,0};
			textureCoords = new float[][][]{{ {0,1,0,0}, {0,1,0,0} }, { {0,1}, {0,1} }};

			colorWave = null;
			textureIndexWave = null;
			textureRotationWave = null;
			textureSWave = null;
			textureTWave = null;
		}
		
		/**
		 * Gets the shader program that this step uses.
		 */
		public OGLShaderResource getShaderProgram()
		{
			return shaderProgram;
		}

		/**
		 * Gets this step's blending type.
		 */
		public BlendType getBlendType()
		{
			return blendType;
		}

		/**
		 * Returns the list of textures to use at this step.
		 */
		public OGLTextureResource[] getTextureList()
		{
			return textureList;
		}

		/**
		 * Gets if the texture list is a multitexture unit and not
		 * an index of individual textures.
		 */
		public boolean isMultitexture()
		{
			return multitexture;
		}

		/**
		 * Gets the texture coordinate generation plane equation, s-axis.
		 * Coefficients are in order from A to D in <code>ax + by + cz + d = 0</code>.
		 */
		public float[] getTextureSPlane()
		{
			return textureCoords[0][TEXCOORD_S];
		}

		/**
		 * Gets the texture coordinate generation plane equation, t-axis.
		 * Coefficients are in order from A to D in <code>ax + by + cz + d = 0</code>.
		 */
		public float[] getTextureTPlane()
		{
			return textureCoords[0][TEXCOORD_T];
		}

		public float getTextureRotationPivotS()
		{
			return textureRotationPivot[TEXCOORD_S];
		}

		public float getTextureRotationPivotT()
		{
			return textureRotationPivot[TEXCOORD_T];
		}

		/**
		 * @return the texGenS
		 */
		public int getTexGenS()
		{
			return texGenS;
		}

		/**
		 * @return the texGenT
		 */
		public int getTexGenT()
		{
			return texGenT;
		}

		public Wave getTextureIndexWave()
		{
			return textureIndexWave;
		}

		public Wave getColorWave()
		{
			return colorWave;
		}

		/**
		 * Gets the wave that handles changes in texture rotation.
		 */
		public Wave getTextureRotationWave()
		{
			return textureRotationWave;
		}

		/**
		 * Gets the wave that handles changes in texture s-axis.
		 */
		public Wave getTextureSWave()
		{
			return textureSWave;
		}

		/**
		 * Gets the wave that handles changes in texture t-axis.
		 */
		public Wave getTextureTWave()
		{
			return textureTWave;
		}

		public void setTextureIndexWave(Wave textureIndexWave)
		{
			this.textureIndexWave = textureIndexWave;
		}

		public void setColorWave(Wave colorWave)
		{
			this.colorWave = colorWave;
		}

		public void setTextureRotationWave(Wave textureRotationWave)
		{
			this.textureRotationWave = textureRotationWave;
		}

		public void setTextureSWave(Wave textureSWave)
		{
			this.textureSWave = textureSWave;
		}

		public void setTextureTWave(Wave textureTWave)
		{
			this.textureTWave = textureTWave;
		}

		/**
		 * Gets the proper interpolated value for the index of the primary texture in the list.
		 * @param time	the current time in milliseconds.
		 */
		public int getTextureIndex(long time)
		{
			if (textureList.length == 0)
				return -1;
			if (textureIndexWave == null)
				return 0;
			return (int)textureIndexWave.getInterpolatedValue(time, 0, textureList.length-1);
		}

		/**
		 * Gets the proper interpolated value for the color's red component.
		 * @param time	the current time in milliseconds.
		 */
		public float getColorRed(long time)
		{
			return colorWave != null ? colorWave.getInterpolatedValue(time, 
					colorCoords[0][COLOR_R], colorCoords[1][COLOR_R]) : colorCoords[0][COLOR_R];
		}

		/**
		 * Gets the proper interpolated value for the color's green component.
		 * @param time	the current time in milliseconds.
		 */
		public float getColorGreen(long time)
		{
			return colorWave != null ? colorWave.getInterpolatedValue(time, 
					colorCoords[0][COLOR_G], colorCoords[1][COLOR_G]) : colorCoords[0][COLOR_G];
		}

		/**
		 * Gets the proper interpolated value for the color's blue component.
		 * @param time	the current time in milliseconds.
		 */
		public float getColorBlue(long time)
		{
			return colorWave != null ? colorWave.getInterpolatedValue(time, 
					colorCoords[0][COLOR_B], colorCoords[1][COLOR_B]) : colorCoords[0][COLOR_B];
		}

		/**
		 * Gets the proper interpolated value for the color's alpha component.
		 * @param time	the current time in milliseconds.
		 */
		public float getColorAlpha(long time)
		{
			return colorWave != null ? colorWave.getInterpolatedValue(time, 
					colorCoords[0][COLOR_A], colorCoords[1][COLOR_A]) : colorCoords[0][COLOR_A];
		}

		/**
		 * Gets the proper interpolated value for the texture's first S-coordinate.
		 * @param time	the current time in milliseconds.
		 */
		public float getTextureS0(long time)
		{
			return textureSWave != null ? textureSWave.getInterpolatedValue(time, 
					textureCoords[0][TEXCOORD_S][0], 
					textureCoords[1][TEXCOORD_S][0]) : textureCoords[0][TEXCOORD_S][0];
		}

		/**
		 * Gets the proper interpolated value for the texture's second S-coordinate.
		 * @param time	the current time in milliseconds.
		 */
		public float getTextureS1(long time)
		{
			return textureSWave != null ? textureSWave.getInterpolatedValue(time, 
					textureCoords[0][TEXCOORD_S][1], 
					textureCoords[1][TEXCOORD_S][1]) : textureCoords[0][TEXCOORD_S][1];
		}

		/**
		 * Gets the proper interpolated value for the texture's first T-coordinate.
		 * @param time	the current time in milliseconds.
		 */
		public float getTextureT0(long time)
		{
			return textureTWave != null ? textureTWave.getInterpolatedValue(time, 
					textureCoords[0][TEXCOORD_T][0], 
					textureCoords[1][TEXCOORD_T][0]) : textureCoords[0][TEXCOORD_T][0];
		}

		/**
		 * Gets the proper interpolated value for the texture's second T-coordinate.
		 * @param time	the current time in milliseconds.
		 */
		public float getTextureT1(long time)
		{
			return textureTWave != null ? textureTWave.getInterpolatedValue(time, 
					textureCoords[0][TEXCOORD_T][1], 
					textureCoords[1][TEXCOORD_T][1]) : textureCoords[0][TEXCOORD_T][1];
		}

		/**
		 * Gets the proper interpolated value for the texture rotation.
		 * @param time	the current time in milliseconds.
		 */
		public float getTextureRotation(long time)
		{
			return textureRotationWave != null ? textureRotationWave.getInterpolatedValue(time, 
					textureRotation[0], textureRotation[1]) : textureRotation[0];
		}

		/**
		 * Sets the shader that this step uses. Can be set to null, for fixed pipeline.
		 */
		public void setShaderProgram(OGLShaderResource shaderProgram)
		{
			this.shaderProgram = shaderProgram;
		}

		/**
		 * Sets the list of textures that this step uses.
		 */
		public void setTextureList(OGLTextureResource ... textures)
		{
			textureList = textures;
		}

		/**
		 * Sets if the texture list is a multitexture unit and not
		 * an index of individual textures.
		 */
		public void setMultitexture(boolean multitexture)
		{
			this.multitexture = multitexture;
		}

		/**
		 * Sets the color.
		 * @param r the red component.
		 * @param g the green component.
		 * @param b the blue component.
		 * @param a the alpha component.
		 */
		public void setColor(float r, float g, float b, float a)
		{
			colorCoords[0][COLOR_R] = r;
			colorCoords[0][COLOR_G] = g;
			colorCoords[0][COLOR_B] = b;
			colorCoords[0][COLOR_A] = a;
		}

		/**
		 * Sets the second color.
		 * @param r the red component.
		 * @param g the green component.
		 * @param b the blue component.
		 * @param a the alpha component.
		 */
		public void setColor2(float r, float g, float b, float a)
		{
			colorCoords[1][COLOR_R] = r;
			colorCoords[1][COLOR_G] = g;
			colorCoords[1][COLOR_B] = b;
			colorCoords[1][COLOR_A] = a;
		}

		/**
		 * Sets this step's blending type.
		 * Note that changing this may affect how it is sorted before
		 * drawing in the scene.
		 */
		public void setBlendType(BlendType blendType)
		{
			this.blendType = blendType;
		}

		/**
		 * Sets the texture rotation.
		 * @param rot	the rotation of the texture.
		 */
		public void setTextureRotation(float rot)
		{
			textureRotation[0] = rot;
		}

		/**
		 * Sets the second texture rotation.
		 * @param rot	the rotation of the texture.
		 */
		public void setTextureRotation2(float rot)
		{
			textureRotation[1] = rot;
		}

		/**
		 * Sets the texture coordinates.
		 * @param s0	the s-coordinate start.
		 * @param t0	the t-coordinate start.
		 * @param s1	the s-coordinate end.
		 * @param t1	the t-coordinate end.
		 */
		public void setTextureCoords(float s0, float t0, float s1, float t1)
		{
			textureCoords[0][TEXCOORD_S][0] = s0;
			textureCoords[0][TEXCOORD_T][0] = t0;
			textureCoords[0][TEXCOORD_S][1] = s1;
			textureCoords[0][TEXCOORD_T][1] = t1;
		}

		/**
		 * Sets the plane equation for generating texture coordinates on the s-axis.
		 * Must be used with a generation type other than TEXGEN_NONE.<br>
		 * Equation is <code>ax + by + cz + d = 0</code>.
		 * @param a	the A coefficient.
		 * @param b	the B coefficient.
		 * @param c	the C coefficient.
		 * @param d	the D coefficient.
		 */
		public void setTextureSPlane(float a, float b, float c, float d)
		{
			textureCoords[0][TEXCOORD_S][0] = a;
			textureCoords[0][TEXCOORD_S][1] = b;
			textureCoords[0][TEXCOORD_S][2] = c;
			textureCoords[0][TEXCOORD_S][3] = d;
		}

		/**
		 * Sets the plane equation for generating texture coordinates on the t-axis.
		 * Must be used with a generation type other than TEXGEN_NONE.<br>
		 * Equation is <code>ax + by + cz + d = 0</code>.
		 * @param a	the A coefficient.
		 * @param b	the B coefficient.
		 * @param c	the C coefficient.
		 * @param d	the D coefficient.
		 */
		public void setTextureTPlane(float a, float b, float c, float d)
		{
			textureCoords[0][TEXCOORD_T][0] = a;
			textureCoords[0][TEXCOORD_T][1] = b;
			textureCoords[0][TEXCOORD_T][2] = c;
			textureCoords[0][TEXCOORD_T][3] = d;
		}

		/**
		 * Sets the second set of texture coordinates.
		 * @param s0	the s-coordinate start.
		 * @param t0	the t-coordinate start.
		 * @param s1	the s-coordinate end.
		 * @param t1	the t-coordinate end.
		 */
		public void setTextureCoords2(float s0, float t0, float s1, float t1)
		{
			textureCoords[1][TEXCOORD_S][0] = s0;
			textureCoords[1][TEXCOORD_T][0] = t0;
			textureCoords[1][TEXCOORD_S][1] = s1;
			textureCoords[1][TEXCOORD_T][1] = t1;
		}

		/**
		 * Sets the texture rotation pivot coordinates.
		 * @param s	the S-coordinate pivot.
		 * @param t	the T-coordinate pivot.
		 */
		public void setTextureRotationPivot(float s, float t)
		{
			textureRotationPivot[TEXCOORD_S] = s;
			textureRotationPivot[TEXCOORD_T] = t;
		}

		/**
		 * @param texGenS the texGenS to set
		 */
		public void setTexGenS(int texGenS)
		{
			this.texGenS = texGenS;
		}

		/**
		 * @param texGenT the texGenT to set
		 */
		public void setTexGenT(int texGenT)
		{
			this.texGenT = texGenT;
		}
}
}
