package com.phat.verletballs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;

public class BallModel {

    private final String vertexShaderCode =
			"uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
			
  		  + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
  		  + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.			  
  		  
  		  + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
  		  
  		  + "void main()                    \n"		// The entry point for our vertex shader.
  		  + "{                              \n"
  		  + "   v_Color = a_Color;          \n"		// Pass the color through to the fragment shader. 
  		  											// It will be interpolated across the triangle.
  		  + "   gl_Position = u_MVPMatrix   \n" 	// gl_Position is a special variable used to store the final position.
  		  + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in 			                                            			 
  		  + "}                              \n";    // normalized screen coordinates.

    private final String fragmentShaderCode =
			"precision mediump float;       \n"		// Set the default precision to medium. We don't need as high of a 
			// precision in the fragment shader.				
    		+ "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the
    		
    		// triangle per fragment.			  
    		+ "void main()                    \n"		// The entry point for our fragment shader.
    		+ "{                              \n"
    		+ "   gl_FragColor = v_Color;     \n"		// Pass the color directly through the pipeline.		  
    		+ "}                              \n";												

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private int mProgramHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

	// This triangle is red, green, and blue.
	final float[] triangle1VerticesData = {
			// X, Y, Z, 
			// R, G, B, A
            -0.5f, -0.25f, 0.0f, 
            1.0f, 0.0f, 0.0f, 1.0f,
            
            0.5f, -0.25f, 0.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            
            0.0f, 0.559016994f, 0.0f, 
            0.0f, 1.0f, 0.0f, 1.0f};
    
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float squareCoords[] = {
            -0.5f,  0.5f, 0.0f,   // top left
            -0.5f, -0.5f, 0.0f,   // bottom left
             0.5f, -0.5f, 0.0f,   // bottom right
             0.5f,  0.5f, 0.0f }; // top right

    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    float color[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public BallModel() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
                squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = VerletBallsGLRender.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = VerletBallsGLRender.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        mProgramHandle = GLES20.glCreateProgram();             // create empty OpenGL Program
        VerletBallsGLRender.checkGlError("glCreateProgram");
        
        GLES20.glAttachShader(mProgramHandle, vertexShader);   // add the vertex shader to program
        VerletBallsGLRender.checkGlError("glAttachShader:vertexShader");
        
        GLES20.glAttachShader(mProgramHandle, fragmentShader); // add the fragment shader to program
        VerletBallsGLRender.checkGlError("glAttachShader:fragmentShader");
        
        GLES20.glLinkProgram(mProgramHandle);                  // create OpenGL program executables
        VerletBallsGLRender.checkGlError("glLinkProgram");
        
        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");        
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");        
        
        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(mProgramHandle);        
        
        
    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgramHandle);
        VerletBallsGLRender.checkGlError("glUseProgram");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        VerletBallsGLRender.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
	
}
