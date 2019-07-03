package com.starplex.nr;

import java.util.*;
import java.util.function.Consumer;

public class Reactor {
    public static final int NEUTRON_RADIUS = 2;
    public static final int ATOM_RADIUS = 8;
    @SuppressWarnings("WeakerAccess")
    public static final int NEUTRON_SPEED = 70;
    @SuppressWarnings("WeakerAccess")
    public static final int SQR_ATOM_NEUTRON_RADIUS = (NEUTRON_RADIUS + ATOM_RADIUS) * (NEUTRON_RADIUS + ATOM_RADIUS);
    @SuppressWarnings("WeakerAccess")
    public static final double ATOM_SELF_RADIOACTIVITY_PROBABILITY = 0;//0.001;
    @SuppressWarnings("WeakerAccess")
    public static final boolean ENABLE_LIFETIME = true;
    @SuppressWarnings("WeakerAccess")
    public static final int PARTICLE_DEFAULT_LIFETIME = 100000;

    private Random random = new Random();

    public static class Particle implements Comparable<Particle>, Cloneable {
        public double x, y;
        public boolean isAbsorber = false;
        private int lifeTime = PARTICLE_DEFAULT_LIFETIME;

        @Override
        public int compareTo(Particle o) {
            return Double.compare(x, o.x);
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public int getMaxLifetime() {
            return PARTICLE_DEFAULT_LIFETIME;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean isEternal() {
            return false;
        }

        public void setLifeTime(int lt) {
            lifeTime = lt;
        }

        public long getLifeTime() {
            return lifeTime;
        }
    }

    private static class BSPNode {
        BSPNode left, right;
        boolean horiz = false;
        double leftBound, rightBound;
        LinkedList<Particle> atoms;

        static class Pair {
            ListIterator<Particle> iter;
            Particle particle;

            public Pair(ListIterator<Particle> it, Particle part) {
                iter = it;
                particle = part;
            }
        }

        public Pair findIterator(Particle particle) {
            if (atoms != null) {
                for (ListIterator<Particle> it = atoms.listIterator(); it.hasNext();) {
                    Particle atom = it.next();
                    if ((atom.x - particle.x) * (atom.x - particle.x) + (atom.y - particle.y) * (atom.y - particle.y)
                            <= SQR_ATOM_NEUTRON_RADIUS) {
                        return new Pair(it, atom);
                    }
                }

                return null;
            } else {
                double mid = (leftBound + rightBound) / 2;
                if (Math.abs(mid - (horiz ? particle.y : particle.x)) <= ATOM_RADIUS + NEUTRON_RADIUS) {
                    Pair left_res = left.findIterator(particle);
                    Pair right_res = right.findIterator(particle);
                    return left_res == null ? right_res : left_res;
                }
                if (mid > (horiz ? particle.y : particle.x)) {
                    return left.findIterator(particle);
                } else {
                    return right.findIterator(particle);
                }
            }
        }

        @SuppressWarnings("unused")
        public Particle find(Particle particle) {
            Pair iter = findIterator(particle);
            if (iter != null) {
                return iter.particle;
            } else {
                return null;
            }
//            if (atoms != null) {
//                for (Particle atom : atoms) {
//                    if ((atom.x - particle.x) * (atom.x - particle.x) + (atom.y - particle.y) * (atom.y - particle.y)
//                            <= SQR_ATOM_NEUTRON_RADIUS) {
//                        return atom;
//                    }
//                }
//
//                return null;
//            } else {
//                double mid = (leftBound + rightBound) / 2;
//                if (Math.abs(mid - (horiz ? particle.y : particle.x)) <= ATOM_RADIUS + NEUTRON_RADIUS) {
//                    Particle left_res = left.find(particle);
//                    Particle right_res = right.find(particle);
//                    return left_res == null ? right_res : left_res;
//                }
//                if (mid > (horiz ? particle.y : particle.x)) {
//                    return left.find(particle);
//                } else {
//                    return right.find(particle);
//                }
//            }
        }
    }

    private BSPNode bspTree;

    private void buildBSPTree(BSPNode v) {
        if (Math.abs(v.rightBound - v.leftBound) <= ATOM_RADIUS + NEUTRON_RADIUS) {
            v.atoms = new LinkedList<>();
            return;
        }

        v.left = new BSPNode();
        v.right = new BSPNode();
        v.left.horiz = !v.horiz;
        v.right.horiz = !v.horiz;
        v.left.leftBound = v.leftBound;
        v.right.leftBound = v.left.rightBound = (v.leftBound + v.rightBound) / 2;
        v.right.rightBound = v.rightBound;
        buildBSPTree(v.left);
        buildBSPTree(v.right);
    }

    private void buildBSPTree(double left, double right) {
        bspTree = new BSPNode();
        bspTree.leftBound = left;
        bspTree.rightBound = right;
        bspTree.horiz = false;
        buildBSPTree(bspTree);
    }

    public static class Neutron extends Particle implements Cloneable {
        public double normal_dirX, normal_dirY;
        public double detectTime = 0;

        @Override
        public boolean isEternal() {
            return true;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public void setRandomDirection(Random random) {
            double angle = random.nextDouble() * Math.PI * 2;
            normal_dirX = Math.cos(angle);
            normal_dirY = Math.sin(angle);
        }
    }

    private LinkedList<Neutron> neutrons = new LinkedList<>();
    public long removedNeutrons = 0;
    private double leftBound, rightBound;

    public long MAX_NEUTRON_COUNT_DATA = 1000;
    @SuppressWarnings("WeakerAccess")
    public double NEUTRON_COUNT_DATA_UPDATE_TIME = 0.4;
    private LinkedList<Integer> neutronCountData = new LinkedList<>();
    private double timeFromLastUpdate = 0;

    public Reactor(double leftBound, double rightBound) {
        this.leftBound = leftBound;
        this.rightBound = rightBound;
        buildBSPTree(leftBound, rightBound);
    }

    public void addAtom(Particle atom) {
        BSPNode cur = bspTree;
        while (cur.atoms == null) {
            double mid = (cur.leftBound + cur.rightBound) / 2;
            double val = cur.horiz ? atom.y : atom.x;
            if (mid >= val) {
                cur = cur.left;
            } else {
                cur = cur.right;
            }
        }

        cur.atoms.add(atom);
    }

    public void addNeutron(Neutron neutron) {
        neutrons.add(neutron);
    }

    public void update(double deltaTime) throws CloneNotSupportedException {
        for (ListIterator<Neutron> iter = neutrons.listIterator(); iter.hasNext();) {
            Neutron neutron = iter.next();

            neutron.x += NEUTRON_SPEED * neutron.normal_dirX * deltaTime;
            neutron.y += NEUTRON_SPEED * neutron.normal_dirY * deltaTime;

            if (neutron.x < leftBound || neutron.x > rightBound
                    || neutron.y < leftBound || neutron.y > rightBound) {
                iter.remove();
                removedNeutrons++;
            }
        }

        for (ListIterator<Neutron> iter = neutrons.listIterator(); iter.hasNext();) {
            Neutron neutron = iter.next();

            if (neutron.detectTime > 0) {
                neutron.detectTime = Math.max(0, neutron.detectTime - deltaTime);
            }

            if (neutron.detectTime < 1e-7) {
                BSPNode.Pair atomIt = bspTree.findIterator(neutron);
                if (atomIt != null) {
                    Particle atom = atomIt.particle;
                    iter.remove();
                    if (!atom.isAbsorber) {
                        Neutron second = (Neutron) neutron.clone();
                        neutron.detectTime = 2 * (ATOM_RADIUS + NEUTRON_RADIUS) * Math.PI / NEUTRON_SPEED;
                        second.detectTime = 2 * (ATOM_RADIUS + NEUTRON_RADIUS) * Math.PI / NEUTRON_SPEED;
                        second.setRandomDirection(random);
                        neutron.setRandomDirection(random);
                        iter.add(neutron);
                        iter.add(second);
                    }
                    if (ENABLE_LIFETIME && !atom.isEternal()) {
                        atom.lifeTime--;
                        if (atom.lifeTime <= 0) {
                            atomIt.iter.remove();
                        }
                    }
                }
            }
        }

        forEachAtom(atom -> {
            if (random.nextDouble() < ATOM_SELF_RADIOACTIVITY_PROBABILITY) {
                Neutron neutron = new Neutron();
                neutron.x = atom.x;
                neutron.y = atom.y;
                double angle = random.nextDouble() * 2 * Math.PI;
                neutron.normal_dirX = Math.cos(angle);
                neutron.normal_dirY = Math.sin(angle);
                addNeutron(neutron);
            }
        });

        if (timeFromLastUpdate + deltaTime > NEUTRON_COUNT_DATA_UPDATE_TIME) {
            neutronCountData.add(getNeutronsCount());
            if (neutronCountData.size() > MAX_NEUTRON_COUNT_DATA) {
                neutronCountData.removeFirst();
            }
            timeFromLastUpdate = 0;
        } else {
            timeFromLastUpdate += deltaTime;
        }
    }

    public double getLeftBound() {
        return leftBound;
    }

    public double getRightBound() {
        return rightBound;
    }

    public LinkedList<Neutron> getNeutrons() {
        return neutrons;
    }

    private void forEachBSP(BSPNode v, Consumer<Particle> f) {
        if (v.atoms == null) {
            forEachBSP(v.left, f);
            forEachBSP(v.right, f);
        } else {
            v.atoms.forEach(f);
        }
    }

    public void forEachAtom(Consumer<Particle> f) {
        forEachBSP(bspTree, f);
    }

    public int getNeutronsCount() {
        return neutrons.size();
    }

    public List<Integer> getNeutronCountData() {
        return neutronCountData;
    }
}
