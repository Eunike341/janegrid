package com.janegrid.core;

public sealed interface Move permits Move.Attack, Move.Step {

    record Step (Position to) implements Move {}
    record Attack (Position targetPos) implements Move {}
}
