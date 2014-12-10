package com.blackrook.ogl.util;

import java.util.Arrays;

import com.blackrook.ogl.OGLGraphics;
import com.blackrook.ogl.enums.BlendArg;
import com.blackrook.ogl.enums.BlendFunc;
import com.blackrook.ogl.enums.TextureMode;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture2D;
import com.blackrook.ogl.util.resource.OGLShaderResource;
import com.blackrook.ogl.util.resource.OGLTextureResource;

/**
 * The reusable draw context for the utility nodes.
 * @author Matthew Tropiano.
 */
public class OGLDrawContext
{
	/** Blending Source. */
	private BlendArg blendSource;
	/** Blending Destination. */
	private BlendArg blendDestination;
	/** Current Shader Program. */
	private OGLShaderResource shader;
	/** Current Texture Mode. */
	private TextureMode textureMode;
	/** Current Texture Units. */
	private OGLTextureResource[] textures;
	/** Amount of texture units. */
	private int textureCount;
	
	/** Creates the context. */
	public OGLDrawContext()
	{
		textures = new OGLTextureResource[8]; // "soft" cleared on reset.
	}
	
	/**
	 * Resets stuff.
	 */
	public void reset(OGLGraphics graphics, OGLResourceLoader loader)
	{
		setBlending(graphics, BlendFunc.ALPHA);
		setShader(graphics, loader, null);
		setTextureEnvironment(graphics, TextureMode.MODULATE);
		setTextures(graphics, loader, textures, 0);
	}
	
	/**
	 * Sets the current shader.
	 * @param graphics the OGL graphics context.
	 * @param shader the shader to use.
	 */
	public void setShader(OGLGraphics graphics, OGLResourceLoader loader, OGLShaderResource shader)
	{
		if (this.shader == shader)
			return;
		
		OGLShaderProgram prog = shader != null ? loader.getShader(shader) : null;
		
		if (prog != null)
			graphics.bind(prog);
		else
			graphics.unbindShaderProgram();
		
		this.shader = shader;
	}
	
	/**
	 * Sets the current textures.
	 * @param graphics the OGL graphics context.
	 * @param inTex the array containing textures to set.
	 * @param count the amount of textures to add.
	 */
	public void setTextures(OGLGraphics graphics, OGLResourceLoader loader, OGLTextureResource[] inTex, int count)
	{
		if (Arrays.equals(textures, inTex) && textureCount == count)
			return;
		
		int lastCount = textureCount;
		
		if (count > textures.length)
		{
			OGLTextureResource[] newarray = new OGLTextureResource[count];
			System.arraycopy(textures, 0, newarray, 0, textures.length);
			textures = newarray;
		}

		for (int x = 0; x < Math.max(count, lastCount); x++)
		{
			OGLTexture2D prog = null;
			if (x < count)
				prog = inTex[x] != null ? loader.getTexture(inTex[x]) : null;

			graphics.setTextureUnit(x);
			
			if (prog != null)
				prog.bindTo(graphics);
			else
				graphics.unbindTexture2D();

			textures[x] = inTex[x];
		}

		textureCount = count;
	}
	
	/**
	 * Sets the current blending.
	 * @param graphics the OGL graphics context.
	 * @param function the blending function.
	 */
	public void setBlending(OGLGraphics graphics, BlendFunc function)
	{
		setBlending(graphics, function.argsrc, function.argdst);
	}

	/**
	 * Sets the current blending.
	 * @param graphics the OGL graphics context.
	 * @param source the blend source (can't be null).
	 * @param destination the blend destination (can't be null).
	 */
	public void setBlending(OGLGraphics graphics, BlendArg source, BlendArg destination)
	{
		if (blendSource == source && blendDestination == destination)
			return;
		
		graphics.setBlendingFunc(source, destination);
		blendSource = source;
		blendDestination = destination;
	}

	/**
	 * Sets the current texture application mode.
	 * @param mode the next texture mode.
	 */
	public void setTextureEnvironment(OGLGraphics graphics, TextureMode mode)
	{
		if (textureMode == mode)
			return;

		graphics.setTextureEnvironment(mode);
		textureMode = mode;
	}
	
}
