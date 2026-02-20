package com.janegrid.ui;

import com.janegrid.core.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {

    private static final int SIZE = 5;
    private static final int TILE = 80;

    private final Cell[][] cells = new Cell[SIZE][SIZE];

    private GameState state;
    private boolean humanSelected = false;
    private final Set<Position> highlightedMoves = new HashSet<>();
    private Optional<Position> highlightedAttackTarget = Optional.empty();

    private final Label status = new Label();
    private final ExecutorService aiExec = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void start(Stage stage) {
        // initial spawn: opposite corners
        Unit human = new Unit(PlayerId.HUMAN, new Position(0, 4), 10);
        Unit ai    = new Unit(PlayerId.AI,    new Position(4, 0), 10);
        state = new GameState(List.of(human, ai), PlayerId.HUMAN);

        GridPane grid = buildGrid();
        VBox root = new VBox(12, status, grid);
        root.setAlignment(Pos.CENTER);

        status.setFont(Font.font(16));

        render();

        Scene scene = new Scene(root, 700, 720);
        stage.setTitle("Jane Grid");
        stage.setScene(scene);
        stage.show();
    }

    private GridPane buildGrid() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {

                StackPane cellPane = new StackPane();
                cellPane.setPrefSize(TILE, TILE);

                Rectangle tile = new Rectangle(TILE, TILE);
                tile.setFill(Color.BEIGE);
                tile.setStroke(Color.GRAY);

                cellPane.getChildren().add(tile);

                // Center sigil
                if (x == 2 && y == 2) {
                    Circle glow = new Circle(TILE * 0.24);
                    glow.setFill(Color.GOLD);
                    glow.setOpacity(0.20);

                    Circle sigil = new Circle(TILE * 0.18);
                    sigil.setFill(Color.GOLD);
                    sigil.setOpacity(0.75);

                    cellPane.getChildren().addAll(glow, sigil);
                }

                Position pos = new Position(x, y);
                Cell cell = new Cell(pos, cellPane, tile);
                cells[x][y] = cell;

                // Click handler for movement (and “click tile” interactions)
                cellPane.setOnMouseClicked(e -> onCellClicked(pos));

                grid.add(cellPane, x, y);
            }
        }

        return grid;
    }

    private void onCellClicked(Position pos) {
        if (state.isTerminal()) return;
        if (state.currentPlayer() != PlayerId.HUMAN) return;

        // If we have a highlighted move and user clicks it -> move
        if (highlightedMoves.contains(pos)) {
            applyHumanMove(new Move.Step(pos));
            return;
        }

        // If user clicks enemy and it is highlighted as attack -> attack
        if (highlightedAttackTarget.isPresent() && highlightedAttackTarget.get().equals(pos)) {
            applyHumanMove(new Move.Attack(pos));
            return;
        }

        // Otherwise: toggle selection if user clicked their unit
        Optional<Unit> u = state.unitAt(pos);
        if (u.isPresent() && u.get().owner() == PlayerId.HUMAN) {
            humanSelected = !humanSelected;
            recomputeHighlights();
            render();
        } else {
            // Clicked elsewhere -> clear selection
            humanSelected = false;
            recomputeHighlights();
            render();
        }
    }

    private void applyHumanMove(Move move) {
        state = state.apply(move);
        humanSelected = false;
        recomputeHighlights();
        render();

        // AI turn
        if (!state.isTerminal() && state.currentPlayer() == PlayerId.AI) {
            status.setText("AI thinking...");
            CompletableFuture
                    .supplyAsync(this::chooseAiMove, aiExec)
                    .thenAccept(aiMove -> Platform.runLater(() -> {
                        if (!state.isTerminal() && state.currentPlayer() == PlayerId.AI) {
                            state = state.apply(aiMove);
                            recomputeHighlights();
                            render();
                        }
                    }));
        }
    }

    private Move chooseAiMove() {
        // Very simple “greedy-ish” AI:
        // 1) If can attack, attack.
        // 2) Else move to reduce distance to human (ties: prefer closer to center sigil).
        Unit ai = state.mustGet(PlayerId.AI);
        Unit human = state.mustGet(PlayerId.HUMAN);

        // Attack if adjacent
        if (Rules.manhattan(ai.pos(), human.pos()) == 1) {
            return new Move.Attack(human.pos());
        }

        List<Position> options = legalStepTargets(ai.pos());

        Position center = new Position(2, 2);
        Position best = options.stream()
                .min(Comparator
                        .comparingInt((Position p) -> Rules.manhattan(p, human.pos()))
                        .thenComparingInt(p -> Rules.manhattan(p, center))
                )
                .orElse(ai.pos());

        return new Move.Step(best);
    }

    private void recomputeHighlights() {
        highlightedMoves.clear();
        highlightedAttackTarget = Optional.empty();

        if (state.isTerminal()) return;
        if (state.currentPlayer() != PlayerId.HUMAN) return;
        if (!humanSelected) return;

        Unit human = state.mustGet(PlayerId.HUMAN);
        Unit enemy = state.mustGet(PlayerId.AI);

        // If adjacent, allow attack by clicking enemy tile (highlighted red)
        if (Rules.manhattan(human.pos(), enemy.pos()) == 1) {
            highlightedAttackTarget = Optional.of(enemy.pos());
        }

        // Movement targets: 1 step orthogonal into empty tiles
        highlightedMoves.addAll(legalStepTargets(human.pos()));
    }

    private List<Position> legalStepTargets(Position from) {
        List<Position> candidates = List.of(
                from.add(1, 0),
                from.add(-1, 0),
                from.add(0, 1),
                from.add(0, -1)
        );

        ArrayList<Position> legal = new ArrayList<>();
        for (Position p : candidates) {
            if (!Rules.inBounds(p, SIZE)) continue;
            if (state.isOccupied(p)) continue;
            legal.add(p);
        }
        return legal;
    }

    private void render() {
        // Update status
        if (state.isTerminal()) {
            boolean humanAlive = state.units().stream().anyMatch(u -> u.owner() == PlayerId.HUMAN);
            status.setText(humanAlive ? "You win! 🎉" : "AI wins!");
        } else {
            status.setText(state.currentPlayer() == PlayerId.HUMAN
                    ? (humanSelected ? "Your turn: choose a highlighted move (or attack if red)." : "Your turn: click your champion.")
                    : "AI turn...");
        }

        // Clear token layers + highlights
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Cell cell = cells[x][y];
                cell.tile.setStroke(Color.GRAY);
                cell.tile.setStrokeWidth(1);

                // Remove any token nodes we added (keep base tile + sigil)
                cell.pane.getChildren().removeIf(n -> n.getProperties().containsKey("token"));
            }
        }

        // Highlights
        for (Position p : highlightedMoves) {
            Cell c = cells[p.x()][p.y()];
            c.tile.setStroke(Color.DODGERBLUE);
            c.tile.setStrokeWidth(4);
        }

        highlightedAttackTarget.ifPresent(p -> {
            Cell c = cells[p.x()][p.y()];
            c.tile.setStroke(Color.FIREBRICK);
            c.tile.setStrokeWidth(4);
        });

        // Render units
        for (Unit u : state.units()) {
            StackPane token = createChampionToken(u);
            token.getProperties().put("token", true);
            cells[u.pos().x()][u.pos().y()].pane.getChildren().add(token);

            // If selected human unit, add a subtle outline effect (via tile stroke already, but you can add more later)
            if (humanSelected && u.owner() == PlayerId.HUMAN) {
                Cell c = cells[u.pos().x()][u.pos().y()];
                c.tile.setStroke(Color.DARKBLUE);
                c.tile.setStrokeWidth(4);
            }
        }
    }

    private StackPane createChampionToken(Unit unit) {
        StackPane root = new StackPane();

        Circle body = new Circle(TILE * 0.28);
        body.setStroke(Color.rgb(20, 20, 20, 0.35));
        body.setStrokeWidth(2);
        body.setFill(unit.owner() == PlayerId.HUMAN ? Color.DODGERBLUE : Color.INDIANRED);

        Text hp = new Text(String.valueOf(unit.hp()));
        hp.setFill(Color.WHITE);
        hp.setFont(Font.font(18));

        root.getChildren().addAll(body, hp);
        return root;
    }

    @Override
    public void stop() {
        aiExec.shutdownNow();
    }

    private record Cell(Position pos, StackPane pane, Rectangle tile) {}
}