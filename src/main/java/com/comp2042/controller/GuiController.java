package com.comp2042.controller;

import com.comp2042.view.GameOverPanel;
import com.comp2042.model.DownData;
import com.comp2042.model.ViewData;
import com.comp2042.view.NotificationPanel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class GuiController implements Initializable {

    //increased size to fix half board game over panel
    private static final int BRICK_SIZE = 30;

    @FXML
    private GridPane gamePanel;

    @FXML
    private Group groupNotification;

    @FXML
    private GridPane brickPanel;

    @FXML
    private GridPane nextPiecePanel; //added new placeholder

    @FXML
    private GameOverPanel gameOverPanel;

    //menu
    @FXML private VBox mainMenu;
    @FXML private HBox gameRoot;
    @FXML private Text modeTitle;
    @FXML private Label timeLabel;
    @FXML private Label linesLabel;

    private Rectangle[][] displayMatrix;

    private InputEventListener eventListener;

    private Rectangle[][] rectangles;

    private Timeline timeLine;

    private final BooleanProperty isPause = new SimpleBooleanProperty();

    private final BooleanProperty isGameOver = new SimpleBooleanProperty();

    //game mode setting
    private boolean isWhooshMode = false;
    private int secondsElapsed = 0;
    private int linesCleared = 0;
    private Rectangle[][] nextPieceMatrix;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        URL fontUrl = getClass().getClassLoader().getResource("digital.ttf");
        if (fontUrl != null){
            Font.loadFont(fontUrl.toExternalForm(), 38);
        }

        //keyboard
        gamePanel.setFocusTraversable(true);
        gamePanel.requestFocus();
        gamePanel.setOnMouseClicked(event -> gamePanel.requestFocus());
        //falling piece blocked mouse clicks
        brickPanel.setMouseTransparent(true);

        //controls
        gamePanel.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (isPause.getValue() == Boolean.FALSE && isGameOver.getValue() == Boolean.FALSE) {
                    //left
                    if (keyEvent.getCode() == KeyCode.LEFT) {
                        refreshBrick(eventListener.onLeftEvent(new MoveEvent(EventType.LEFT, EventSource.USER)));
                        keyEvent.consume();
                    }
                    //right
                    if (keyEvent.getCode() == KeyCode.RIGHT) {
                        refreshBrick(eventListener.onRightEvent(new MoveEvent(EventType.RIGHT, EventSource.USER)));
                        keyEvent.consume();
                    }
                    //rotate (arrow up)
                    if (keyEvent.getCode() == KeyCode.UP) {
                        refreshBrick(eventListener.onRotateEvent(new MoveEvent(EventType.ROTATE, EventSource.USER)));
                        keyEvent.consume();
                    }
                    //soft drop (arrow down)
                    if (keyEvent.getCode() == KeyCode.DOWN) {
                        moveDown(new MoveEvent(EventType.DOWN, EventSource.USER));
                        keyEvent.consume();
                    }
                    //hard drop (space)
                    if (keyEvent.getCode() == KeyCode.SPACE) {
                        DownData data;
                        do {
                            data = eventListener.onDownEvent(new MoveEvent(EventType.DOWN, EventSource.USER));
                            refreshBrick(data.getViewData());
                        } while (data.getClearRow() == null && data.getViewData().getBrickData() != null);
                        keyEvent.consume();
                    }
                    //hold (C)
                    if (keyEvent.getCode() == KeyCode.C) {
                        System.out.println("Hold feature pending implementation");
                        keyEvent.consume();
                    }
                }
                if (keyEvent.getCode() == KeyCode.N) {
                    newGame(null);
                }
            }
        });

        gameOverPanel.setVisible(false);
    }

    //menu actions
    public void startLucidGame(ActionEvent event){
        setupGame(false, "LUCID");
    }

    public void startWhooshGame(ActionEvent event){
        setupGame(true, "WHOOSH!");
    }

    public void backToMenu(ActionEvent event){
        if (timeLine != null) timeLine.stop();
        gameRoot.setVisible(false);
        mainMenu.setVisible(true);
    }

    private void setupGame(boolean isWhoosh, String title) {
        isWhooshMode = isWhoosh;
        modeTitle.setText(title);
        //whoosh 120 secs
        //lucid 0 secs
        secondsElapsed = isWhooshMode ? 120 : 0;
        linesCleared = 0;
        updateStatsUI();

        mainMenu.setVisible(false);
        gameRoot.setVisible(true);
        gamePanel.requestFocus();
        newGame(null);
    }

    //game loop and logic
    public void initGameView(int[][] boardMatrix, ViewData brick) {

        displayMatrix = new Rectangle[boardMatrix.length][boardMatrix[0].length];
        for (int i = 2; i < boardMatrix.length; i++) {
            for (int j = 0; j < boardMatrix[i].length; j++) {
                Rectangle rectangle = new Rectangle(BRICK_SIZE, BRICK_SIZE);
                rectangle.setFill(Color.TRANSPARENT);
                displayMatrix[i][j] = rectangle;
                gamePanel.add(rectangle, j, i - 2);
            }
        }

        //fixing glitch
        refreshBrick(brick);

        //next piece panel
        nextPiecePanel.getChildren().clear();
        nextPieceMatrix = new Rectangle[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Rectangle rectangle = new Rectangle(BRICK_SIZE + 1 * 0.7, BRICK_SIZE + 1 * 0.7);
                rectangle.setFill(Color.TRANSPARENT);
                nextPieceMatrix[i][j] = rectangle;
                nextPiecePanel.add(rectangle, j, i);
            }
        }
        if (timeLine != null) timeLine.stop();

        //gravity + timer
        timeLine = new Timeline(new KeyFrame(
                Duration.millis(600),
                ae -> {
                    moveDown(new MoveEvent(EventType.DOWN, EventSource.THREAD));

                    //clock logic
                    if (isWhooshMode) {
                        secondsElapsed--; //whoosh (countdown)
                        if (secondsElapsed <= 0) gameOver();
                    } else {
                        secondsElapsed++; //lucid (stopwatch)
                    }
                    updateStatsUI();
                }
        ));
        timeLine.setCycleCount(Timeline.INDEFINITE);
        timeLine.play();
    }
        private void updateStatsUI() {
            //format time
            int min = Math.abs(secondsElapsed) / 60;
            int sec = Math.abs(secondsElapsed) % 60;
            timeLabel.setText(String.format("Time: %02d:%02d", min, sec));

            //format lines
            if (isWhooshMode) {
                linesLabel.setText("Lines: " + linesCleared + " / 50");
            } else {
                linesLabel.setText("Lines: " + linesCleared);
            }
        }
        private Paint getFillColor(int i) {
            Paint returnPaint;
            switch (i) {
                case 0:
                    returnPaint = Color.TRANSPARENT;
                    break;
                case 1:
                    returnPaint = Color.AQUA;
                    break;
                case 2:
                    returnPaint = Color.BLUEVIOLET;
                    break;
                case 3:
                    returnPaint = Color.DARKGREEN;
                    break;
                case 4:
                    returnPaint = Color.YELLOW;
                    break;
                case 5:
                    returnPaint = Color.RED;
                    break;
                case 6:
                    returnPaint = Color.BEIGE;
                    break;
                case 7:
                    returnPaint = Color.BURLYWOOD;
                    break;
                default:
                    returnPaint = Color.WHITE;
                    break;
            }
            return returnPaint;
        }



    private void refreshBrick(ViewData brick) {
        if (isPause.getValue() == Boolean.FALSE) {

            brickPanel.setLayoutX(brick.getxPosition() * BRICK_SIZE);
            brickPanel.setLayoutY((brick.getyPosition() - 2) * BRICK_SIZE);

            //clear old piece shape
            brickPanel.getChildren().clear();

            //re-initialize falling piece view based on ViewData shape
            int rows = brick.getBrickData().length;
            int cols = brick.getBrickData()[0].length;

            rectangles = new Rectangle[rows][cols]; //Re-size the rectangles array

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    Rectangle rectangle = new Rectangle(BRICK_SIZE, BRICK_SIZE);
                    setRectangleData(brick.getBrickData()[i][j], rectangle); //set color
                    rectangles[i][j] = rectangle;
                    brickPanel.add(rectangle, j, i); //add rectangle to the pane
                }
            }
        }
    }

    public void refreshGameBackground(int[][] board) {
        for (int i = 2; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                setRectangleData(board[i][j], displayMatrix[i][j]);
            }
        }
    }

    private void setRectangleData(int color, Rectangle rectangle) {
        rectangle.setFill(getFillColor(color));
        rectangle.setArcHeight(9);
        rectangle.setArcWidth(9);
    }

    private void moveDown(MoveEvent event) {
        if (isPause.getValue() == Boolean.FALSE) {
            DownData downData = eventListener.onDownEvent(event);
            if (downData.getClearRow() != null && downData.getClearRow().getLinesRemoved() > 0) {
                //lines cleared (updated)
                linesCleared += downData.getClearRow().getLinesRemoved();
                updateStatsUI();

                NotificationPanel notificationPanel = new NotificationPanel("+" + downData.getClearRow().getScoreBonus());
                groupNotification.getChildren().add(notificationPanel);
                notificationPanel.showScore(groupNotification.getChildren());
            }
            refreshBrick(downData.getViewData());
        }
        gamePanel.requestFocus();
    }

    public void setEventListener(InputEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void bindScore(IntegerProperty integerProperty) {}

        public void gameOver() {
            if (timeLine != null) timeLine.stop();
            gameOverPanel.setVisible(true);
            isGameOver.setValue(Boolean.TRUE);
        }

    public void newGame(ActionEvent actionEvent) {
        if (timeLine !=null) timeLine.stop();
        gameOverPanel.setVisible(false);
        eventListener.createNewGame();
        gamePanel.requestFocus();
        if (timeLine !=null) timeLine.play();
        isPause.setValue(Boolean.FALSE);
        isGameOver.setValue(Boolean.FALSE);

        //reset stats
        linesCleared = 0;
        updateStatsUI();
    }

    //next piece in side box
    public void refreshNextPiece(int[][] nextPieceData) {
        if (nextPieceData == null || nextPieceMatrix == null) return;

        //clear 4x4
        for (int i =0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                nextPieceMatrix[i][j].setFill(Color.TRANSPARENT);
            }
        }
        //draw piece itself
        for (int i = 0; i < nextPieceData.length; i++) {
            for (int j = 0; j < nextPieceData[i].length; j++) {
                if (i < 4 && j < 4) {
                    setRectangleData(nextPieceData[i][j], nextPieceMatrix[i][j]);
                }
            }
        }
    }
}
