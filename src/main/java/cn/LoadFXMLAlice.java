package cn;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import com.fasterxml.jackson.databind.ObjectMapper;


import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.*;


public class LoadFXMLAlice extends Application{
    public HashMap<String, String> username_ip = new HashMap<>();
    private List<String> ips = new ArrayList<>();
    private static String username = "Alice";

    public static void main(String[] args) {

        Peer peer = new Peer(username);
        peer.start();
        launch(args);

    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MessageAppUI.fxml"));
        Parent vbox = loader.load();

        Scene scene = new Scene(vbox);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);
        primaryStage.setTitle(username);

    }

    private void closeWindowEvent(WindowEvent windowEvent) {
        ObjectMapper objectMapper = new ObjectMapper();
        BigInteger recoveredSecret = DownloadShares.getSecret();
        for (String chatName : Peer.messages.keySet()){
            String fileName= "chatsMessages/" +username +"/"+ chatName + ".txt";
            List<String> messages = new ArrayList<>();
            for (Message message : Peer.messages.get(chatName)){
                messages.add(message.toString());
            }
            try {
                DownloadShares.encryptMessage(fileName,messages, recoveredSecret);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try {
            UploadShares.uploadEncryptedMessages(username);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PrintWriter writer;
        for (String s : Peer.sslSocketUsers.keySet()){
            SSLSocket socket = Peer.sslSocketUsers.get(s);
            try {
                writer = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            writer.println("close");
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.exit(0);
    }
}