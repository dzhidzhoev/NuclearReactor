package com.starplex.nr;

import static org.lwjgl.opengl.GL11.*;

public class ReactorRenderer {
    private Reactor reactor;
    @SuppressWarnings("WeakerAccess")
    public static final double UI_SCALE = 0.4;
    @SuppressWarnings("WeakerAccess")
    public static final int NEUTRON_COUNT_RADIUS = 1;
    @SuppressWarnings("WeakerAccess")
    public static final int MAX_NEUTRON_COUNT_RADIUS = 1;

    private static final int MAX_NEUTRON_COUNT_DEFAULT_VALUE = 1;
    private int maxNeutronCount = MAX_NEUTRON_COUNT_DEFAULT_VALUE;

    public ReactorRenderer(Reactor r) {
        reactor = r;

//        BufferedImage atom = null;
//        try {
//            atom = ImageIO.read(new File("atom.png"));
//            atom.getData().
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void setReactor(Reactor r) {
        reactor = r;
        maxNeutronCount = MAX_NEUTRON_COUNT_DEFAULT_VALUE;
    }

    public void render() {
        glPointSize(Reactor.NEUTRON_RADIUS );
        glBegin(GL_POINTS);
        glColor3f(1, 1, 1);
        for (Reactor.Neutron neutron : reactor.getNeutrons()) {
            glVertex3d(neutron.x / reactor.getRightBound(), neutron.y / reactor.getRightBound(), 0);
        }
        glEnd();

        glPointSize(Reactor.ATOM_RADIUS );
        glBegin(GL_POINTS);
        reactor.forEachAtom(this::renderAtom);
        glEnd();

        glPointSize(NEUTRON_COUNT_RADIUS);
        glBegin(GL_POINTS);

        long i = 0;
        for (Integer y: reactor.getNeutronCountData()) {
            maxNeutronCount = Math.max(y, maxNeutronCount);
            glColor3f(0, 0, 1);
            glVertex3d(1 - (double)(reactor.getNeutronCountData().size() - i) / reactor.MAX_NEUTRON_COUNT_DATA, -1 + UI_SCALE * y / maxNeutronCount, 0);
            glPointSize(MAX_NEUTRON_COUNT_RADIUS);
            glColor3f(1, 0, 0);
            glVertex3d(1 - (double)(reactor.getNeutronCountData().size() - i) / reactor.MAX_NEUTRON_COUNT_DATA, -1 + UI_SCALE, 0);
            i++;
        }
        glEnd();
    }

    private void renderAtom(Reactor.Particle atom) {
        float colorCoeff = (float) ((double)atom.getLifeTime() / atom.getMaxLifetime());
        boolean useCoeff = !atom.isEternal() && Reactor.ENABLE_LIFETIME;
        if (atom.isAbsorber)
            glColor3f(0, !useCoeff  ? 1 : colorCoeff, !useCoeff ? 1 : colorCoeff);
        else
            glColor3f(!useCoeff ? 1 : colorCoeff, !useCoeff ? 1 : colorCoeff, 0);
        glVertex3d(atom.x  / reactor.getRightBound(), atom.y  / reactor.getRightBound(), 0);
    }
}
