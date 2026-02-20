package com.janegrid.core;

public enum PlayerId {
    HUMAN, AI;

    public PlayerId opponent() {
        return this == HUMAN ? AI : HUMAN;
    }
}
