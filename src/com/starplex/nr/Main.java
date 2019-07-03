package com.starplex.nr;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;

@SuppressWarnings("WeakerAccess")
public class Main {

    public static final int WINDOW_SIZE = 750;

    private long window;

    GameController controller;

    public void run() throws InterruptedException, CloneNotSupportedException {
        init();
        loop();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(WINDOW_SIZE, WINDOW_SIZE, "Nuclear Reactor", 0, 0);
        if ( window == 0 )
            throw new RuntimeException("Failed to create the GLFW window");

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        glfwShowWindow(window);
    }

    public void loop() throws InterruptedException, CloneNotSupportedException {
        GL.createCapabilities();

        glViewport(0, 0, WINDOW_SIZE, WINDOW_SIZE);
        glClearColor(0, 0, 0, 1);

        controller = new GameController(WINDOW_SIZE);

        double frameTime = 0;
        double lastTime = 0;
        long cnt = 0;

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            double deltaTime = glfwGetTime() - lastTime;
            lastTime = glfwGetTime();

            frameTime += deltaTime;
            cnt++;
            if (frameTime >= 1) { // FPS counter
//                System.out.println(cnt);
                cnt = 0;
                frameTime = 0;
                System.out.print(controller.reactor.getNeutronsCount());
                System.out.print(" - ");
                System.out.print(controller.reactor.removedNeutrons);
                controller.reactor.removedNeutrons = 0;
                System.out.println(" N/s");
            }

            controller.processInput(window);
            controller.update(deltaTime);
            controller.render();

            glfwSwapBuffers(window);

            glfwPollEvents();
        }
    }

    public static void main(String[] args) throws InterruptedException, CloneNotSupportedException {
	    new Main().run();
    }
}
