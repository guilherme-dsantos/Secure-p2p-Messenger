package cn;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;

import java.io.IOException;

public class DynamicController {

    private String filename;
    private boolean selected = false;

    @FXML
    private Label label; // Example component in the dynamic pane
    @FXML
    private Pane contact;
    @FXML
    private Label numberOfUnreadMessages;
    @FXML
    private SVGPath bell;

    @FXML
    public void initialize() throws IOException {
        hideBell();

    }

    // Method to set data in the dynamic pane components
    public void setData(String data) {
        label.setText(data);
    }

    public String getData(){ return label.getText();}

    public String getFilename(){
        return filename;
    }
    public void setFilename(String file) {
        filename = file;
    }

    public void setColorLighter(){
        contact.setStyle("-fx-background-color:#5e6397");
    }
    public void setColorNormal(){
        if(!selected){
            contact.setStyle("-fx-background-color: #4d528c");
        }
    }
    public void select(){
        selected=true;
        setColorLighter();
    }
    public void deselect(){
        selected=false;
        setColorNormal();
    }
    public void hideBell(){
        bell.setVisible(false);
        numberOfUnreadMessages.setVisible(false);
    }
    public void showBell(){
        bell.setVisible(true);
        numberOfUnreadMessages.setVisible(true);
    }
    public void resetCounter(){
        numberOfUnreadMessages.setText("0");
        if(bell.isVisible()){
            hideBell();
        }
    }
    public void incrementCounter(){
        int aux = Integer.parseInt(numberOfUnreadMessages.getText())+1;
        numberOfUnreadMessages.setText(""+aux);
        if(!bell.isVisible()){
            showBell();
        }
    }
}
