package com.blackrook.ogl.util.scene2d;

import com.blackrook.commons.math.geometry.Rectangle2F;
import com.blackrook.ogl.data.OGLColor;
import com.blackrook.ogl.mesh.MeshView;
import com.blackrook.ogl.util.resource.OGLTextureResource;

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
	 * Gets the textures to use for a particular object.
	 * @param object the object.
	 * @param outTextures the array that receives the textures to use.
	 * @return the amount of texture units.
	 */
	public int getObjectTextures(T object, OGLTextureResource[] outTextures);
	
	/**
	 * Gets the 2D bounds of a particular object.
	 * @param object the object.
	 * @param outRect the rectangle to set the bounds on.
	 */
	public void getObjectBounds(T object, Rectangle2F outRect);
	
	/**
	 * Gets the 2D bounds of a particular object.
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
	 * Gets the a sort bias for a particular object, which can
	 * force the order by which it is rendered.
	 * @param object the object.
	 */
	public double getObjectSortBias(T object);
	
}
