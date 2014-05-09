/*******************************************************************************
 * Copyright (c) 2014 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.ogl.util.model;

import com.blackrook.commons.list.List;
import com.blackrook.commons.math.RMath;
import com.blackrook.io.SuperReader;
import com.blackrook.ogl.OGLGeometryUtils;
import com.blackrook.ogl.OGLGraphics;
import com.blackrook.ogl.OGLMesh;
import com.blackrook.ogl.enums.AccessType;
import com.blackrook.ogl.enums.BufferType;
import com.blackrook.ogl.enums.CachingHint;
import com.blackrook.ogl.enums.GeometryType;
import com.blackrook.ogl.mesh.MeshView;
import com.blackrook.ogl.object.buffer.OGLFloatBuffer;

import java.io.*;
import java.nio.FloatBuffer;

/**
 * id tech 2 (Quake 2) Model format.
 * @author Matthew Tropiano
 */
public class MD2Model implements OGLMesh
{
	public static final int
	MD2_IDENT = 844121161,
	MD2_VERSION = 8;
	
	/** Model's Skin width */
	private int skinWidth;
	/** Model's Skin height */
	private int skinHeight;

	/** Skin names */
	private String[] skins;
	/** Number of vertices */
	private int numVertices;
	/** OpenGL geometry command integers */
	private int numOGLCmdInts;
	/** Texture coordinates */
	private TexCoord[] texCoords;
	/** Triangles */
	private Triangle[] triangles;
	/** Model frames */
	private Frame[] frames;
	/** OpenGL Commands */
	private OGLC[] oglCmds;

	/**
	 * Constructs a new MD2 Model.
	 * @param modelFile				the file to use for the model.
	 * @throws FileNotFoundException if the file can't be found.
	 * @throws IOException			if the file can't be read.
	 * @throws NullPointerException	if modelFile is null.
	 */
	public MD2Model(String modelFile) throws IOException
	{
		this(new File(modelFile));
	}

	/**
	 * Constructs a new MD2 Model.
	 * @param modelFile			the file to use for the model.
	 * @throws FileNotFoundException if the file can't be found.
	 * @throws IOException			if the file can't be read.
	 * @throws NullPointerException	if modelFile is null.
	 */
	public MD2Model(File modelFile) throws IOException
	{
		this(new FileInputStream(modelFile));
	}

	/**
	 * Constructs a new MD2 Model from an InputStream.
	 * @param in	the InputStream.
	 */
	public MD2Model(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in,SuperReader.LITTLE_ENDIAN);
		
		// is this an MD2?
		if (sr.readInt() != MD2_IDENT || sr.readInt() != MD2_VERSION)
			throw new IOException("This is not an MD2 file.");
		
		// read model header.
		skinWidth = sr.readInt();
		skinHeight = sr.readInt();
		
		sr.readInt(); // frameSize 

		skins = new String[sr.readInt()];
		numVertices = sr.readInt();
		texCoords = new TexCoord[sr.readInt()];
		triangles = new Triangle[sr.readInt()];
		numOGLCmdInts = sr.readInt();
		frames = new Frame[sr.readInt()];
		
		sr.readInt(); // skinOffset
		sr.readInt(); // texCoordOffset
		sr.readInt(); // triangleOffset
		sr.readInt(); // frameOffset
		sr.readInt(); // OGLCmdOffset
		
		sr.readInt();
		
		// load skins
		for (int i = 0; i < skins.length; i++)
			skins[i] = new String(sr.readBytes(64)).trim();
		
		// load texcoords
		for (int i = 0; i < texCoords.length; i++)
		{
			short[] c = sr.readShorts(2);
			texCoords[i] = new TexCoord(
					(float)c[0]/(float)skinWidth,
					(float)c[1]/(float)skinHeight);
		}
		
		// load triangles
		for (int i = 0; i < triangles.length; i++)
			triangles[i] = new Triangle(sr.readShorts(3),sr.readShorts(3));
		
		// load frames
		for (int i = 0; i < frames.length; i++)
		{
			float[] scaleVect = sr.readFloats(3);
			float[] transVect = sr.readFloats(3);
			String fname = new String(sr.readBytes(16)).trim();
			
			Vertex[] verts = new Vertex[numVertices];
			// load vertices per frame.
			for (int j = 0; j < verts.length; j++)
			{
				byte[] coords = sr.readBytes(3);
				byte norm = sr.readByte();
				
				float[] realpos = new float[3];

				for (int k = 0; k < 3; k++)
				{
					int ubyte = coords[k] < 0 ? coords[k]+256 : coords[k];
					realpos[k] = (ubyte * scaleVect[k]) + transVect[k];
				}

				float[] normalvec = Normal.anorm[(norm<0)?norm+256:norm];
				verts[j] = new Vertex(realpos,normalvec);
			}
			
			frames[i] = new Frame(fname,verts);
		}
		
		// load OpenGL Commands
		int[] cmd = sr.readInts(numOGLCmdInts);
		int cmdIndex = 0;
		int c;
		List<OGLC> v = new List<OGLC>(10);
		while ((c = cmd[cmdIndex++]) != 0)
		{
			int glcmd = c < 0 ? GeometryType.TRIANGLE_FAN.glValue : GeometryType.TRIANGLE_STRIP.glValue;
			c = Math.abs(c);
			
			OGLCNode[] nodes = new OGLCNode[c];
			int node = 0;
			while (c != 0)
			{
				float s = Float.intBitsToFloat(cmd[cmdIndex]);
				float t = Float.intBitsToFloat(cmd[cmdIndex+1]);
				int vtx = cmd[cmdIndex+2];
				
				nodes[node++] = new OGLCNode(s,t,vtx);
				c--;
				cmdIndex+=3;
			}
			v.add(new OGLC(glcmd,nodes));
		}
		oglCmds = new OGLC[v.size()];
		v.toArray(oglCmds);
	}
	
	public int getFrameCount()
	{
		return frames.length;
	}

	/**
	 * Returns a view for rendering this model.
	 */
	public MD2ModelView getView()
	{
		return new MD2ModelView();
	}

	/**
	 * The view that draws this model.
	 */
	public class MD2ModelView extends MeshView
	{
		/** Current frame index. */
		private int currentFrame;
		/** Next frame index. */
		private int nextFrame;
		/** Interpolation factor. */
		private float interpFactor;
		/** Trigger for re-grabbing model info. */
		private boolean redrawTrigger;
		
		/** Geometry Buffer. */
		private OGLFloatBuffer geometryBuffer;
		
		private MD2ModelView()
		{
			currentFrame = 0;
			nextFrame = 0;
			interpFactor = 0f;
		}
		
		public int getCurrentFrame()
		{
			return currentFrame;
		}

		public float getInterpolationFactor()
		{
			return interpFactor;
		}

		public int getNextFrame()
		{
			return nextFrame;
		}

		public void setFrame(int currentFrame, int nextFrame, float interpFactor)
		{
			this.currentFrame = currentFrame;
			this.nextFrame = nextFrame;
			this.interpFactor = interpFactor;
			redrawTrigger = true;
		}

		@Override
		public GeometryType getGeometryType()
		{
			return GeometryType.TRIANGLES;
		}

		@Override
		public int getElementCount()
		{
			return triangles.length * 3;
		}

		@Override
		public int getIndex(int element)
		{
			return element;
		}

		@Override
		public float getVertex(int index, int dimension)
		{
			return (float)RMath.linearInterpolate(interpFactor, 
				frames[currentFrame].vertices[triangles[index / 3].vIndex[index % 3]].pos[dimension], 
				frames[nextFrame].vertices[triangles[index / 3].vIndex[index % 3]].pos[dimension]
			); 
		}

		@Override
		public float getTextureCoordinate(int index, int unit, int dimension)
		{
			TexCoord tx = texCoords[triangles[index / 3].tIndex[index % 3]];
			return dimension == 0 ? tx.s : tx.t;
		}

		@Override
		public float getNormal(int index, int component)
		{
			return (float)RMath.linearInterpolate(interpFactor, 
				frames[currentFrame].vertices[triangles[index / 3].vIndex[index % 3]].norm[component], 
				frames[nextFrame].vertices[triangles[index / 3].vIndex[index % 3]].norm[component]
			);
		}

		@Override
		public float getColor(int index, int component)
		{
			return 1f;
		}

		@Override
		public void drawUsing(OGLGraphics g)
		{
			if (g.supportsVertexBuffers())
				drawVBOBranch(g);
			else
				drawDefaultBranch(g);
		}
		
		/**
		 * Drawing branch for VBO path.
		 */
		protected void drawVBOBranch(OGLGraphics g)
		{
			if (geometryBuffer == null)
			{
				int vertnum = (triangles.length*3);
				int flts = (vertnum*3 + vertnum*2 + vertnum*3);
				geometryBuffer = new OGLFloatBuffer(g, BufferType.GEOMETRY);
				// align to closest power of two.
				geometryBuffer.setCapacity(g, CachingHint.STREAM_DRAW, RMath.closestPowerOfTwo(flts));
			}
			
			if (redrawTrigger)
			{
				FloatBuffer fb = geometryBuffer.mapBuffer(g, AccessType.WRITE);
				getVertices(fb, 0, 3, 0, 8);
				getTextureCoordinates(fb, 0, 0, 2, 3, 8);
				getNormals(fb, 0, 5, 8);
				geometryBuffer.unmapBuffer(g);
				redrawTrigger = false;
			}

			OGLGeometryUtils.drawInterleavedGeometry(
				g, geometryBuffer, getGeometryType(), getElementCount(),
				OGLGeometryUtils.vertices(3, 8, 0),
				OGLGeometryUtils.texCoords(0, 2, 8, 3),
				OGLGeometryUtils.normals(8, 5)
			);
		}

		public void drawDefaultBranch(OGLGraphics g)
		{
			// asdasd
		}
		
	}

	protected static class TexCoord
	{
		float s;
		float t;
		
		public TexCoord(float s, float t)
		{
			this.s = s;
			this.t = t;
		}
	}

	protected static class Triangle
	{
		short[] vIndex;
		short[] tIndex;
		
		public Triangle(short[] v, short[] t)
		{
			vIndex = v;
			tIndex = t;
		}
}

	protected static class Vertex
	{
		float[] pos;
		float[] norm;
		
		public Vertex(float[] pos, float[] norm)
		{
			this.pos = pos;
			this.norm = norm;
		}
		
}

	protected static class Frame
	{
		String name;
		Vertex[] vertices;
		
		public Frame(String name, Vertex[] v)
		{
			this.name = name;
			vertices = v;
		}
}

	protected static class OGLC
	{
		int glGeom;
		OGLCNode[] vertices;
		
		public OGLC(int gl, OGLCNode[] verts)
		{
			glGeom = gl;
			vertices = verts;
		}
	}

	protected static class OGLCNode
	{
		TexCoord tc;
		int vIndex;
		
		public OGLCNode(float s, float t, int v)
		{
			tc = new TexCoord(s,t);
			vIndex = v;
		}
		
	}
	
	public static class Normal
	{
		/** MD2 enumerated vertex normals. */
		public static final float[][] anorm =
		{
			{ -0.525731f, 0.000000f, 0.850651f }, 
			{ -0.442863f, 0.238856f, 0.864188f }, 
			{ -0.295242f, 0.000000f, 0.955423f }, 
			{ -0.309017f, 0.500000f, 0.809017f }, 
			{ -0.162460f, 0.262866f, 0.951056f }, 
			{ 0.000000f, 0.000000f, 1.000000f }, 
			{ 0.000000f, 0.850651f, 0.525731f }, 
			{ -0.147621f, 0.716567f, 0.681718f }, 
			{ 0.147621f, 0.716567f, 0.681718f }, 
			{ 0.000000f, 0.525731f, 0.850651f }, 
			{ 0.309017f, 0.500000f, 0.809017f }, 
			{ 0.525731f, 0.000000f, 0.850651f }, 
			{ 0.295242f, 0.000000f, 0.955423f }, 
			{ 0.442863f, 0.238856f, 0.864188f }, 
			{ 0.162460f, 0.262866f, 0.951056f }, 
			{ -0.681718f, 0.147621f, 0.716567f }, 
			{ -0.809017f, 0.309017f, 0.500000f }, 
			{ -0.587785f, 0.425325f, 0.688191f }, 
			{ -0.850651f, 0.525731f, 0.000000f }, 
			{ -0.864188f, 0.442863f, 0.238856f }, 
			{ -0.716567f, 0.681718f, 0.147621f }, 
			{ -0.688191f, 0.587785f, 0.425325f }, 
			{ -0.500000f, 0.809017f, 0.309017f }, 
			{ -0.238856f, 0.864188f, 0.442863f }, 
			{ -0.425325f, 0.688191f, 0.587785f }, 
			{ -0.716567f, 0.681718f, -0.147621f }, 
			{ -0.500000f, 0.809017f, -0.309017f }, 
			{ -0.525731f, 0.850651f, 0.000000f }, 
			{ 0.000000f, 0.850651f, -0.525731f }, 
			{ -0.238856f, 0.864188f, -0.442863f }, 
			{ 0.000000f, 0.955423f, -0.295242f }, 
			{ -0.262866f, 0.951056f, -0.162460f }, 
			{ 0.000000f, 1.000000f, 0.000000f }, 
			{ 0.000000f, 0.955423f, 0.295242f }, 
			{ -0.262866f, 0.951056f, 0.162460f }, 
			{ 0.238856f, 0.864188f, 0.442863f }, 
			{ 0.262866f, 0.951056f, 0.162460f }, 
			{ 0.500000f, 0.809017f, 0.309017f }, 
			{ 0.238856f, 0.864188f, -0.442863f }, 
			{ 0.262866f, 0.951056f, -0.162460f }, 
			{ 0.500000f, 0.809017f, -0.309017f }, 
			{ 0.850651f, 0.525731f, 0.000000f }, 
			{ 0.716567f, 0.681718f, 0.147621f }, 
			{ 0.716567f, 0.681718f, -0.147621f },
			{ 0.525731f, 0.850651f, 0.000000f }, 
			{ 0.425325f, 0.688191f, 0.587785f }, 
			{ 0.864188f, 0.442863f, 0.238856f }, 
			{ 0.688191f, 0.587785f, 0.425325f }, 
			{ 0.809017f, 0.309017f, 0.500000f }, 
			{ 0.681718f, 0.147621f, 0.716567f }, 
			{ 0.587785f, 0.425325f, 0.688191f }, 
			{ 0.955423f, 0.295242f, 0.000000f }, 
			{ 1.000000f, 0.000000f, 0.000000f }, 
			{ 0.951056f, 0.162460f, 0.262866f }, 
			{ 0.850651f, -0.525731f, 0.000000f },
			{ 0.955423f, -0.295242f, 0.000000f },
			{ 0.864188f, -0.442863f, 0.238856f },
			{ 0.951056f, -0.162460f, 0.262866f },
			{ 0.809017f, -0.309017f, 0.500000f },
			{ 0.681718f, -0.147621f, 0.716567f },
			{ 0.850651f, 0.000000f, 0.525731f }, 
			{ 0.864188f, 0.442863f, -0.238856f },
			{ 0.809017f, 0.309017f, -0.500000f },
			{ 0.951056f, 0.162460f, -0.262866f },
			{ 0.525731f, 0.000000f, -0.850651f },
			{ 0.681718f, 0.147621f, -0.716567f },
			{ 0.681718f, -0.147621f, -0.716567f },
			{ 0.850651f, 0.000000f, -0.525731f }, 
			{ 0.809017f, -0.309017f, -0.500000f }, 
			{ 0.864188f, -0.442863f, -0.238856f }, 
			{ 0.951056f, -0.162460f, -0.262866f }, 
			{ 0.147621f, 0.716567f, -0.681718f }, 
			{ 0.309017f, 0.500000f, -0.809017f }, 
			{ 0.425325f, 0.688191f, -0.587785f }, 
			{ 0.442863f, 0.238856f, -0.864188f }, 
			{ 0.587785f, 0.425325f, -0.688191f }, 
			{ 0.688191f, 0.587785f, -0.425325f }, 
			{ -0.147621f, 0.716567f, -0.681718f },
			{ -0.309017f, 0.500000f, -0.809017f },
			{ 0.000000f, 0.525731f, -0.850651f }, 
			{ -0.525731f, 0.000000f, -0.850651f },
			{ -0.442863f, 0.238856f, -0.864188f },
			{ -0.295242f, 0.000000f, -0.955423f },
			{ -0.162460f, 0.262866f, -0.951056f },
			{ 0.000000f, 0.000000f, -1.000000f }, 
			{ 0.295242f, 0.000000f, -0.955423f }, 
			{ 0.162460f, 0.262866f, -0.951056f }, 
			{ -0.442863f, -0.238856f, -0.864188f }, 
			{ -0.309017f, -0.500000f, -0.809017f }, 
			{ -0.162460f, -0.262866f, -0.951056f }, 
			{ 0.000000f, -0.850651f, -0.525731f }, 
			{ -0.147621f, -0.716567f, -0.681718f }, 
			{ 0.147621f, -0.716567f, -0.681718f }, 
			{ 0.000000f, -0.525731f, -0.850651f }, 
			{ 0.309017f, -0.500000f, -0.809017f }, 
			{ 0.442863f, -0.238856f, -0.864188f }, 
			{ 0.162460f, -0.262866f, -0.951056f }, 
			{ 0.238856f, -0.864188f, -0.442863f }, 
			{ 0.500000f, -0.809017f, -0.309017f }, 
			{ 0.425325f, -0.688191f, -0.587785f }, 
			{ 0.716567f, -0.681718f, -0.147621f }, 
			{ 0.688191f, -0.587785f, -0.425325f }, 
			{ 0.587785f, -0.425325f, -0.688191f }, 
			{ 0.000000f, -0.955423f, -0.295242f }, 
			{ 0.000000f, -1.000000f, 0.000000f }, 
			{ 0.262866f, -0.951056f, -0.162460f }, 
			{ 0.000000f, -0.850651f, 0.525731f }, 
			{ 0.000000f, -0.955423f, 0.295242f }, 
			{ 0.238856f, -0.864188f, 0.442863f }, 
			{ 0.262866f, -0.951056f, 0.162460f }, 
			{ 0.500000f, -0.809017f, 0.309017f }, 
			{ 0.716567f, -0.681718f, 0.147621f }, 
			{ 0.525731f, -0.850651f, 0.000000f }, 
			{ -0.238856f, -0.864188f, -0.442863f }, 
			{ -0.500000f, -0.809017f, -0.309017f }, 
			{ -0.262866f, -0.951056f, -0.162460f }, 
			{ -0.850651f, -0.525731f, 0.000000f }, 
			{ -0.716567f, -0.681718f, -0.147621f },
			{ -0.716567f, -0.681718f, 0.147621f }, 
			{ -0.525731f, -0.850651f, 0.000000f }, 
			{ -0.500000f, -0.809017f, 0.309017f }, 
			{ -0.238856f, -0.864188f, 0.442863f }, 
			{ -0.262866f, -0.951056f, 0.162460f }, 
			{ -0.864188f, -0.442863f, 0.238856f }, 
			{ -0.809017f, -0.309017f, 0.500000f }, 
			{ -0.688191f, -0.587785f, 0.425325f }, 
			{ -0.681718f, -0.147621f, 0.716567f }, 
			{ -0.442863f, -0.238856f, 0.864188f }, 
			{ -0.587785f, -0.425325f, 0.688191f }, 
			{ -0.309017f, -0.500000f, 0.809017f }, 
			{ -0.147621f, -0.716567f, 0.681718f }, 
			{ -0.425325f, -0.688191f, 0.587785f }, 
			{ -0.162460f, -0.262866f, 0.951056f }, 
			{ 0.442863f, -0.238856f, 0.864188f }, 
			{ 0.162460f, -0.262866f, 0.951056f }, 
			{ 0.309017f, -0.500000f, 0.809017f },
			{ 0.147621f, -0.716567f, 0.681718f },
			{ 0.000000f, -0.525731f, 0.850651f },
			{ 0.425325f, -0.688191f, 0.587785f },
			{ 0.587785f, -0.425325f, 0.688191f },
			{ 0.688191f, -0.587785f, 0.425325f },
			{ -0.955423f, 0.295242f, 0.000000f },
			{ -0.951056f, 0.162460f, 0.262866f },
			{ -1.000000f, 0.000000f, 0.000000f },
			{ -0.850651f, 0.000000f, 0.525731f },
			{ -0.955423f, -0.295242f, 0.000000f }, 
			{ -0.951056f, -0.162460f, 0.262866f }, 
			{ -0.864188f, 0.442863f, -0.238856f }, 
			{ -0.951056f, 0.162460f, -0.262866f }, 
			{ -0.809017f, 0.309017f, -0.500000f }, 
			{ -0.864188f, -0.442863f, -0.238856f },
			{ -0.951056f, -0.162460f, -0.262866f },
			{ -0.809017f, -0.309017f, -0.500000f },
			{ -0.681718f, 0.147621f, -0.716567f }, 
			{ -0.681718f, -0.147621f, -0.716567f },
			{ -0.850651f, 0.000000f, -0.525731f }, 
			{ -0.688191f, 0.587785f, -0.425325f }, 
			{ -0.587785f, 0.425325f, -0.688191f }, 
			{ -0.425325f, 0.688191f, -0.587785f }, 
			{ -0.425325f, -0.688191f, -0.587785f },
			{ -0.587785f, -0.425325f, -0.688191f },
			{ -0.688191f, -0.587785f, -0.425325f }
	};
}

}

