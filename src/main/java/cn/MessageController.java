package cn;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.io.IOException;

public class MessageController {

    @FXML
    private Label label; // Example component in the dynamic pane
    @FXML
    private VBox messagePane;


    @FXML
    public void initialize() throws IOException {

    }

    public void setData(String data) {
        label.setText(data);
    }

    public void setPaneRightSide(){
        messagePane.setTranslateX(120);
    }


}
