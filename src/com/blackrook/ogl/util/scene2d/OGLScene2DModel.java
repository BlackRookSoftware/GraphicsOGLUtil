package com.blackrook.ogl.util.scene2d;

import com.blackrook.commons.math.Tuple2F;
import com.blackrook.commons.math.geometry.Point2F;
import com.blackrook.ogl.data.OGLColor;
import com.blackrook.ogl.enums.BlendFunc;
import com.blackrook.ogl.mesh.MeshView;
import com.blackrook.ogl.object.shader.OGLShaderProgram;
import com.blackrook.ogl.object.texture.OGLTexture2D;

/**
 * Implementors of this class describe a model by which scene elements
 * are returned on camera space queries. 
 * @author Matthew Tropiano
 */
public interface OGLScene2DModel<T extends Object>
{

	/**
	 * Returns a set of objects that represent viewable objects that touch
	 * a point in space. The objects visible are returned into the provided 
	 * array until either the end of the array is reached completely or 
	 * there are no more visible objects.
	 * <p>This is expected to perform better than evaluating each object in the
	 * model using {@link #objectIsInPoint(Object, double, double)}.
	 * @param centerX the point's coordinate, x-axis.
	 * @param centerY the point's coordinate, y-axis.
	 * @param outArray the output array.
	 * @param offset the starting offset into the array. 
	 * @return the amount of objects returned. 
	 */
	public int getObjectsInPoint(double centerX, double centerY, T[] outArray, int offset);

	/**
	 * Returns a set of objects that represent viewable objects inside
	 * a bounding circle. The objects visible are returned into the provided 
	 * array until either the end of the array is reached completely or 
	 * there are no more visible objects.
	 * <p>This is expected to perform better than evaluating each object in the 
	 * model using {@link #objectIsInCircle(Object, double, double, double)}.
	 * @param centerX the center of the bounding area, x-axis.
	 * @param centerY the center of the bounding area, y-axis.
	 * @param radius the radius of the bounding area.
	 * @param outArray the output array.
	 * @param offset the starting offset into the array. 
	 * @return the amount of objects returned. 
	 */
	public int getObjectsInCircle(double centerX, double centerY, double radius, T[] outArray, int offset);

	/**
	 * Returns a set of objects that represent viewable objects inside
	 * a bounding box. The objects visible are returned into the provided 
	 * array until either the end of the array is reached completely or 
	 * there are no more visible objects.
	 * <p>This is expected to perform better than evaluating each object in the 
	 * model using {@link #objectIsInBox(Object, double, double, double, double)}.
	 * @param centerX the center of the bounding area, x-axis.
	 * @param centerY the center of the bounding area, y-axis.
	 * @param halfWidth the half-width of the bounding area. 
	 * @param halfHeight the half-height of the bounding area.
	 * @param outArray the output array.
	 * @param offset the starting offset into the array.
	 * @return the amount of objects returned. 
	 */
	public int getObjectsInBox(double centerX, double centerY, double halfWidth, double halfHeight, T[] outArray, int offset);

	/**
	 * Checks if an object, according to the model, is touching a point.
	 * @param object the object to test.
	 * @param centerX the point's coordinate, x-axis.
	 * @param centerY the point's coordinate, y-axis.
	 * @return if the object is touching the point. 
	 */
	public boolean objectIsInPoint(T object, double centerX, double centerY); 

	/**
	 * Checks if an object, according to the model, is inside a bounding circle. 
	 * @param object the object to test.
	 * @param centerX the center of the bounding area, x-axis.
	 * @param centerY the center of the bounding area, y-axis.
	 * @param radius the radius of the bounding area.
	 * @return if the object is in the bounding circle. 
	 */
	public boolean objectIsInCircle(T object, double centerX, double centerY, double radius);
	
	/**
	 * Checks if an object, according to the model, is inside a bounding box. 
	 * @param object the object to test.
	 * @param centerX the center of the bounding area, x-axis.
	 * @param centerY the center of the bounding area, y-axis.
	 * @param halfWidth the half-width of the bounding area. 
	 * @param halfHeight the half-height of the bounding area.
	 * @return if the object is in the bounding box. 
	 */
	public boolean objectIsInBox(T object, double centerX, double centerY, double halfWidth, double halfHeight); 
	
	/**
	 * Gets the blending to use for a particular object.
	 * @param object the object.
	 */
	public BlendFunc getObjectBlending(T object);
	
	/**
	 * Gets the shader to use for a particular object.
	 * @param object the object.
	 * @return the shader to use. can be null.
	 */
	public OGLShaderProgram getObjectShader(T object);
	
	/**
	 * Gets the textures to use for a particular object.
	 * @param object the object.
	 * @param outTextures the array that receives the textures to use.
	 * @return the amount of texture units.
	 */
	public int getObjectTextures(T object, OGLTexture2D[] outTextures);
	
	/**
	 * Gets the color of a particular object.
	 * @param object the object.
	 * @param outColor the color object to set colors on.
	 */
	public void getObjectColor(T object, OGLColor outColor);
	
	/**
	 * Gets the mesh to use to render a particular object.
	 * @param object the object.
	 * @return the mesh view to use for rendering the geometry.
	 */
	public MeshView getObjectMesh(T object);

	/**
	 * Gets the depth for a particular object, which can
	 * force the order by which it is rendered.
	 * @param object the object.
	 */
	public float getObjectDepth(T object);

	/**
	 * Gets the a sort bias for a particular object, which can
	 * force the order by which it is rendered.
	 * @param object the object.
	 */
	public float getObjectSortBias(T object);

	/**
	 * Gets the centerpoint of a particular object.
	 * @param object the object.
	 * @param centerPoint the centerpoint to set.
	 */
	public void getObjectCenter(T object, Point2F centerPoint);

	/**
	 * Gets the half widths of a particular object.
	 * @param object the object.
	 * @param widthTuple the two-dimensional tuple to set.
	 */
	public void getObjectHalfWidths(T object, Tuple2F widthTuple);

	/**
	 * Gets the color of a particular object.
	 * @param object the object.
	 * @return the rotation in degrees.
	 */
	public float getObjectRotation(T object);
	
}
