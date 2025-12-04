package com.comp2042;

import com.comp2042.controller.GameController;
import com.comp2042.controller.GuiController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        URL location = getClass().getClassLoader().getResource("gameLayout.fxml");
        //removed 'resources' variable and 'null' argument
        FXMLLoader fxmlLoader = new FXMLLoader(location);
        //'root' to 'rootNode'
        Parent rootNode = fxmlLoader.load();
        //'c' to 'guiController'
        GuiController guiController = fxmlLoader.getController();

        primaryStage.setTitle("TetrisJFX");
        Scene scene = new Scene(rootNode, 300, 510);
        primaryStage.setScene(scene);
        primaryStage.show();
        new GameController(guiController);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
