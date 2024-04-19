package cn;

import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class MainController {

    List<PrintWriter> writers = new ArrayList<>();
    List<SSLSocket> sslSockets= new ArrayList<>();
    public static String chatName;
    public static String filename;
    private List<DynamicController> controllers = new ArrayList<>();
    private Thread threadRead;
    @FXML
    private VBox contacts;
    @FXML
    private VBox messages;
    @FXML
    private Label userOnline;
    @FXML
    private TextField messageField;
    @FXML
    private ToolBar toolBar;

    public void sendButtonPress(ActionEvent event) throws IOException, InvalidCipherTextException, PolicySyntaxException, ClassNotFoundException {
        sendMessage(messageField.getText());
        messageField.clear();
    }


    @FXML
    public void initialize() throws IOException, InterruptedException {
        Thread.sleep(400);
        threadToRead();
        for(File file : Peer.listOfFiles){

                FXMLLoader loader = new FXMLLoader(getClass().getResource("DynamicPane.fxml"));
                Pane contact = loader.load();

                DynamicController controller = loader.getController();
                controllers.add(controller);
                if (file.getName().contains("_"))
                    controller.setData(file.getName().split("_")[1].split("\\.")[0]);
                else controller.setData(file.getName().split("\\.")[0]);
                controller.setFilename(file.getName().split("\\.")[0]);
                //System.out.println(controller.getFilename());

                contact.setOnMouseClicked(event -> {
                    if(!controller.getData().equals(chatName)) {


                        Platform.runLater(() -> {
                           controller.resetCounter();
                           for(DynamicController cont:controllers){
                               cont.deselect();
                           }
                           controller.select();
                        });
                        messages.getChildren().clear();
                        writers.clear();
                        sslSockets.clear();
                        chatName = controller.getData();
                        filename=controller.getFilename();
                        try {
                            loadChat(filename);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        List<String> usernames=new ArrayList<>();
                        if (Peer.groupUsers.containsKey(chatName)){
                            //usernames.addAll(Peer.groupUsers.get(chatName));
                            //PURPOSEDLY SENT TO ALL THE USERS FOR THE CONSEQUENTLY SHOW OF THE DECRYPTION FAILING DUE TO LACK OF RIGHT ATTRIBUTES
                            usernames.addAll(Peer.usersReaders.keySet());
                        }else{
                            usernames.add(filename);
                        }
                        for (String username : usernames) {
                            //System.out.println(username);
                        }
                        boolean oneUserOffline=false;
                        for (String user : usernames){
                            SSLSocket sslSocket = Peer.sslSocketUsers.get(user);
                            if (sslSocket == null) {
                                oneUserOffline=true;
                            }else sslSockets.add(sslSocket);
                        }
                        if (oneUserOffline){
                            if (!toolBar.isDisable()) toolBar.setDisable(true);
                            userOnline.setText("Chat is Offline");
                        } else {
                            if (toolBar.isDisable()) toolBar.setDisable(false);
                            userOnline.setText("Chat is Online");
                            for (SSLSocket s :sslSockets){
                                try {
                                    writers.add(new PrintWriter(s.getOutputStream(), true));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            try {
                                threadToRead();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    }
                });
                contacts.getChildren().add(contact);
            }
    }


    private void loadChat(String filename) throws IOException {
        for (File f : Peer.listOfFiles){
            if (f.getName().equals(filename + ".txt")){
                try {
                    for (String message: DownloadShares.decryptMessages("chatsMessages/" + Peer.userName +"/"+ f.getName(), DownloadShares.getSecret())){
                        System.out.println(message);
                        String[] messageAttributes = message.split(",");
                        if(messageAttributes[0].equals("false")) receiveMessage(messageAttributes[1]);
                        else loadMessageUI(messageAttributes[1]);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if(Peer.messages.containsKey(filename)){
            for(Message i : Peer.messages.get(filename)){
                String content = i.getContent();

                FXMLLoader loader2 = new FXMLLoader(getClass().getResource("Message.fxml"));
                AnchorPane message = loader2.load();
                MessageController controller2 = loader2.getController();
                controller2.setData(content);

                if(i.getSent()) controller2.setPaneRightSide();

                messages.getChildren().add(message);
            }
        }
    }


    private void threadToRead() throws IOException {
        Task<Void> backgroundTask = new Task<>(){

            @Override
            protected Void call() throws Exception {
                String aux = chatName;
                System.out.println("Reading messages from chat" + chatName);
                Peer.notificationQueue.clear();
                while (true) {
                    // Wait for a notification
                    Peer.notificationQueue.take();
                    if (chatName==null){
                        //System.out.println("Mensagem recebida de" + Peer.lastMessageReceived.getFileName());
                        notiMessage(Peer.lastMessageReceived.getFileName());
                    }
                    else if(aux==null&&chatName!=null){
                        Peer.notificationQueue.put(true);
                        break;
                    }
                    else if (aux.equals(chatName)) {
                        if(Peer.lastMessageReceived.getFileName().equals(filename) && !Peer.lastMessageReceived.getSent()){
                            String receivedMessage = Peer.lastMessageReceived.getContent();
                            Platform.runLater(() -> {
                                try {
                                    receiveMessage(receivedMessage);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            //System.out.println("chatname: " + chatName);
                            //System.out.println("Receivedmaincontroller: " + receivedMessage);
                        }else{
                            System.out.println("Mensagem recebida de" + Peer.lastMessageReceived.getFileName());
                            notiMessage(Peer.lastMessageReceived.getFileName());

                        }

                    } else {
                        Peer.notificationQueue.put(true);
                        break;
                    }
                }
                return null;
            }
        };
        threadRead = new Thread(backgroundTask);
        threadRead.setDaemon(true);
        threadRead.start();
    }

    public void notiMessage(String filename) {
        for (DynamicController dc:controllers){
            if(dc.getFilename().equals(filename)){
                Platform.runLater(dc::incrementCounter);
                break;
            }
        }
    }

    private void sendMessage(String text) throws IOException, PolicySyntaxException, ClassNotFoundException, InvalidCipherTextException {
        if(Peer.groupUsers.get(chatName)==null){
            Peer.messages.get(filename).add(new Message(true, filename, Peer.userName, text));
            for (PrintWriter writer : writers){
                writer.println(Peer.userName + "," + text);
            }
        }
        else {
            //App.defineAccessPolicyString("40 and (200 or 430 or 30)");
            Peer.messages.get(filename).add(new Message(true, filename, Peer.userName, text));
            String enctext= App.encryptStringPublic(text, Peer.group_accessstring.get(filename),Peer.attribute_publickey.get(chatName));
            for (PrintWriter writer : writers){
                writer.println(filename + "," + enctext);
            }
        }
        loadMessageUI(text);
    }
    public void receiveMessage(String text) throws IOException {
        loadOtherMessageUI(text);

    }

    private void loadMessageUI(String message) throws IOException {
        FXMLLoader loader2 = new FXMLLoader(getClass().getResource("Message.fxml"));
        AnchorPane messagePane = loader2.load();
        MessageController controller2 = loader2.getController();
        controller2.setData(message);
        controller2.setPaneRightSide();
        messages.getChildren().add(messagePane);
    }
    private void loadOtherMessageUI(String message) throws IOException {
        FXMLLoader loader2 = new FXMLLoader(getClass().getResource("Message.fxml"));
        AnchorPane messagePane = loader2.load();
        MessageController controller2 = loader2.getController();
        controller2.setData(message);

        messages.getChildren().add(messagePane);
    }

}

