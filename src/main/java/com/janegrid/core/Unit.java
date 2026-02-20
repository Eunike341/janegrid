package com.janegrid.core;

public record Unit(PlayerId owner, Position pos, int hp) {
    public Unit withPos(Position p) {
        return new Unit (owner, p, hp);
    }

    public Unit withHp(int newHp) {
        return new Unit(owner, pos, newHp);
    }
}
