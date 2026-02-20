package com.janegrid.core;

public final class Rules {

    private Rules() {}

    public static int manhattan(Position a, Position b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    public static boolean inBounds (Position p, int size) {
        return p.x() >= 0 && p.x() < size && p.y() >= 0 && p.y() < size;
    }

}
