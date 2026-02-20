package com.janegrid.core;

import java.util.List;
import java.util.Optional;

public record GameState(List<Unit> units, PlayerId currentPlayer) {

    public Optional<Unit> unitAt(Position p) {
        return units.stream()
                .filter(u -> u.pos().equals(p)).findFirst();
    }

    public Unit mustGet(PlayerId owner) {
        return units.stream()
                .filter(u -> u.owner() == owner)
                .findFirst()
                .orElseThrow();
    }

    public boolean isOccupied (Position p) {
        return unitAt(p).isPresent();
    }

    public boolean isTerminal() {
        boolean humanAlive = units.stream().anyMatch(u -> u.owner() == PlayerId.HUMAN && u.hp() > 0);
        boolean aiAlive = units.stream().anyMatch(u -> u.owner() == PlayerId.AI && u.hp() > 0);
        return !(humanAlive && aiAlive);
    }

    public GameState apply (Move move) {
        if (isTerminal()) return this;

        return switch (move) {
            case Move.Step step -> applyStep (step);
            case Move.Attack attack -> applyAttack(attack);
        };
    }

    public GameState applyStep (Move.Step step) {
        Unit mover = mustGet(currentPlayer);
        Unit moved = mover.withPos(step.to());

        List<Unit> nextUnits = units.stream()
                .map(u -> u.owner() == currentPlayer ? moved : u)
                .toList();

        return new GameState (nextUnits, currentPlayer.opponent());
    }

    public GameState applyAttack (Move.Attack attack) {
        Unit attacker = mustGet (currentPlayer);
        Unit defender = unitAt(attack.targetPos()).orElseThrow();

        int newHp = defender.hp() - 3;
        Unit updatedDef = defender.withHp(newHp);

        List<Unit> nextUnits = units.stream()
                .map(u -> u.equals(defender) ? updatedDef : u)
                .filter(u -> u.hp() > 0)
                .toList();

        return new GameState(nextUnits, currentPlayer.opponent());
    }
}
