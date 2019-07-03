package com.starplex.nr;

import com.google.gson.*;
import com.oracle.javafx.jmx.json.JSONException;

import java.io.*;
import java.util.Random;

import static com.starplex.nr.Reactor.PARTICLE_DEFAULT_LIFETIME;
import static org.lwjgl.glfw.GLFW.*;

public class GameController {
    public Reactor reactor;
    private ReactorRenderer renderer;
    private int windowSize;
    private boolean paused = false;

    @SuppressWarnings("WeakerAccess")
    public static final String SAVE_FILE_NAME = "game.json";

    private static final boolean GENERATE_DEFAULT_MAP = true;

    public GameController(int windowSize) {
        this.windowSize = windowSize;
        reactor = new Reactor(- windowSize / 2, windowSize / 2);
        renderer = new ReactorRenderer(reactor);

        if (GENERATE_DEFAULT_MAP) {
            generateDefaultMap();
        }
    }

    private void generateDefaultMap() {
        Random rnd = new Random();
        final int ATOMS_COUNT = 55;
        for (int i = 0; i < ATOMS_COUNT; i++) {
            Reactor.Particle atom = new Reactor.Particle();
            Reactor.Particle absorber = new Reactor.Particle();
            double angle = 2.0 * i / ATOMS_COUNT * Math.PI;
            atom.x = Math.cos(angle) * (reactor.getRightBound() - reactor.getLeftBound()) / 3.6;
            atom.y = Math.sin(angle) * (reactor.getRightBound() - reactor.getLeftBound()) / 3.6;
            absorber.x = atom.x * 1.4;
            absorber.y = atom.y * 1.4;
            absorber.isAbsorber = true;
            atom.setLifeTime(rnd.nextInt(atom.getMaxLifetime() / 2) + atom.getMaxLifetime() / 2 + 1);
            absorber.setLifeTime(rnd.nextInt(absorber.getMaxLifetime() / 2) + absorber.getMaxLifetime() / 2 + 1);
            reactor.addAtom(atom);
            reactor.addAtom(absorber);
        }
    }

    private double[] mousePosX = new double[1];
    private double[] mousePosY = new double[1];
    private int prevLeftState = -10;
    private int prevRightState = -10;
    private int prevSpaceState = -10;
    private int prevPState = -10;
    private int prevSState = -10;
    private int prevLState = -10;
    public void processInput(long window) {
        int leftState = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT);
        if (leftState == GLFW_RELEASE && prevLeftState == GLFW_PRESS) {
            glfwGetCursorPos(window, mousePosX, mousePosY);
            mousePosX[0] -= windowSize / 2;
            mousePosY[0] = windowSize / 2 - mousePosY[0];
            Reactor.Particle atom = new Reactor.Particle();
            atom.x = (float) mousePosX[0];
            atom.y = (float) mousePosY[0];
            reactor.addAtom(atom);
        }
        prevLeftState = leftState;
        int rightState = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT);
        if (rightState == GLFW_RELEASE && prevRightState == GLFW_PRESS) {
            glfwGetCursorPos(window, mousePosX, mousePosY);
            mousePosX[0] -= windowSize / 2;
            mousePosY[0] = windowSize / 2 - mousePosY[0];
            for (double i = 0; i < 2 * Math.PI; i += 2 * Math.PI / 100) {
                Reactor.Neutron neutron = new Reactor.Neutron();
                neutron.normal_dirX = Math.cos(i);
                neutron.normal_dirY = Math.sin(i);
                neutron.x = mousePosX[0];
                neutron.y = mousePosY[0];
                reactor.addNeutron(neutron);
            }
        }
        int spaceState = glfwGetKey(window, GLFW_KEY_SPACE);
        if (spaceState == GLFW_RELEASE && prevSpaceState == GLFW_PRESS) {
            glfwGetCursorPos(window, mousePosX, mousePosY);
            mousePosX[0] -= windowSize / 2;
            mousePosY[0] = windowSize / 2 - mousePosY[0];

            Reactor.Particle atom = new Reactor.Particle();
            atom.x = (float) mousePosX[0];
            atom.y = (float) mousePosY[0];
            atom.isAbsorber = true;
            reactor.addAtom(atom);
        }
        int PState = glfwGetKey(window, GLFW_KEY_P);
        if (PState == GLFW_RELEASE && prevPState == GLFW_PRESS) {
            paused = !paused;
        }
        int SState = glfwGetKey(window, GLFW_KEY_S);
        if (SState == GLFW_RELEASE && prevSState == GLFW_PRESS) {
            Gson gson = new GsonBuilder().create();
            try (PrintStream ps = new PrintStream(new FileOutputStream(SAVE_FILE_NAME))) {
                ps.print(gson.toJson(reactor));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int LState = glfwGetKey(window, GLFW_KEY_L);
        if (LState == GLFW_RELEASE && prevLState == GLFW_PRESS) {
            Gson gson = new GsonBuilder().create();
            try (FileReader ps = new FileReader(SAVE_FILE_NAME)) {
                Reactor newReactor = gson.fromJson(ps, Reactor.class);
                reactor = newReactor;
                renderer.setReactor(newReactor);
            } catch (IOException | JsonParseException e) {
                e.printStackTrace();
            }
        }
        prevSpaceState = spaceState;
        prevRightState = rightState;
        prevPState = PState;
        prevSState = SState;
        prevLState = LState;
    }

    public void update(double deltaTime) throws CloneNotSupportedException {
        if (paused)
            return;
        reactor.update(deltaTime);
    }

    public void render() {
        renderer.render();
    }
}
