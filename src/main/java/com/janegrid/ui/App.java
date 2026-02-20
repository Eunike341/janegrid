package com.janegrid.ui;

import com.janegrid.core.PlayerId;
import com.janegrid.core.Position;
import com.janegrid.core.Unit;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;

public class App extends Application {

    private static final int SIZE = 5;
    private static final int TILE = 80;

    // Initial positions: opposite ends
    private static final Unit HUMAN = new Unit(PlayerId.HUMAN, new Position(0, 4), 10);
    private static final Unit AI    = new Unit(PlayerId.AI,    new Position(4, 0), 10);

    @Override
    public void start(Stage stage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);

        // Render background tiles (and a center sigil)
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(TILE, TILE);

                Rectangle tile = new Rectangle(TILE, TILE);
                tile.setFill(Color.BEIGE);
                tile.setStroke(Color.GRAY);

                cell.getChildren().add(tile);

                // Optional: Sigil at center (2,2)
                if (x == 2 && y == 2) {
                    Circle sigil = new Circle(TILE * 0.18);
                    sigil.setFill(Color.GOLD);
                    sigil.setOpacity(0.75);

                    Circle glow = new Circle(TILE * 0.24);
                    glow.setFill(Color.GOLD);
                    glow.setOpacity(0.20);

                    cell.getChildren().addAll(glow, sigil);
                }

                grid.add(cell, x, y);
            }
        }

        // Render champions
        List<Unit> units = List.of(HUMAN, AI);
        for (Unit u : units) {
            StackPane token = createChampionToken(u);
            grid.add(token, u.pos().x(), u.pos().y());
        }

        Scene scene = new Scene(grid, 600, 600);
        stage.setTitle("Jane Grid");
        stage.setScene(scene);
        stage.show();
    }

    private StackPane createChampionToken(Unit unit) {
        StackPane root = new StackPane();
        root.setPickOnBounds(false); // clicks pass through empty area
        root.setMouseTransparent(true); // for now: just visuals (we'll enable clicks later)

        Circle body = new Circle(TILE * 0.28);
        body.setStroke(Color.rgb(20, 20, 20, 0.35));
        body.setStrokeWidth(2);

        if (unit.owner() == PlayerId.HUMAN) {
            body.setFill(Color.DODGERBLUE);
        } else {
            body.setFill(Color.INDIANRED);
        }

        Text hp = new Text(String.valueOf(unit.hp()));
        hp.setFill(Color.WHITE);
        hp.setFont(Font.font(18));

        root.getChildren().addAll(body, hp);
        return root;
    }

    public static void main(String[] args) {
        launch();
    }
}