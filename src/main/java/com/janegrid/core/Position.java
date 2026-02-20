package com.janegrid.core;

public record Position(int x, int y) {
    public Position add (int dx, int dy) {
        return new Position(this.x + dx, this.y + dy);
    }
}
