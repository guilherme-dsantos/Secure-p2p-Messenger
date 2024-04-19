package cn;

import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.*;

// Server class
public class Peer extends Thread  {

    public static String usernameReceiver;
    public static Set<String> ipsReceived = new HashSet<>();
    public static HashMap<String, List<String>> groupUsers = new HashMap<>();
    private static BufferedReader serverReader;
    public static PrintWriter serverWriter;
    private static ObjectInputStream serverInput;
    private static SSLSocket sslSocket = null;
    public static HashMap<String, SSLSocket> sslSocketUsers = new HashMap<>();
    public static HashMap<String, BufferedReader> usersReaders = new HashMap<>();
    public static String userName;
    public static final BlockingQueue<Boolean> notificationQueue = new LinkedBlockingQueue<>();
    public static HashMap<String, List<Message>> messages = new HashMap<>();
    public static Message lastMessageReceived;
    public static List<File> listOfFiles = new ArrayList<>();
    public static HashMap<String, String> group_accessstring = new HashMap<>();
    public static PairingKeySerParameter secretKey;
    public static HashMap<String, PairingKeySerParameter> attribute_publickey = new HashMap<>();


    public Peer(String userName){
        Peer.userName =userName;
        System.setProperty("javax.net.ssl.keyStore", "certs/"+userName+"/"+userName+"keystore.jks");
        System.setProperty("javax.net.ssl.trustStore", "certs/"+userName+"/"+userName+"truststore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", System.getenv("peer_password"));
        System.setProperty("javax.net.ssl.trustStorePassword", System.getenv("peer_password"));
    }

    @Override
    public void run() {
        try {
            File folder = new File("chatsMessages/"+userName);
            File[] files = folder.listFiles();
            assert files != null;
            for (File file : files) {
                if (file.isFile() && !file.getName().split("\\.")[0].equals(userName)) {
                    listOfFiles.add(file);
                }
            }
            System.out.println(listOfFiles.size());

            SSLContext sslContext = SSLContext.getDefault();
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

            SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(2344+userName.length());
            sslServerSocket.setNeedClientAuth(true);

            ConnectToServer("localhost", 9090, usernameReceiver);
            serverWriter.println("port:" + (2344+userName.length()));
            Object ob = serverInput.readObject();
            secretKey= (PairingKeySerParameter) ob;
            attribute_publickey = (HashMap<String, PairingKeySerParameter>) serverInput.readObject();

            for(File file : Peer.listOfFiles) {
                System.out.println("File"+file.getName());
                String fileName = file.getName().split("\\.")[0];
                String name="";
                if (fileName.contains("_")){
                    name = fileName.split("_")[1];
                }else name = fileName;
                messages.put(fileName,new ArrayList<>());
                if (fileName.split("_")[0].equals("Group")) {
                    sendMessageToServerAttribute(name);
                } else{
                    sendMessageToServerUsername(name);
                }

            }
            System.out.println("message size" + Peer.messages.size());
            serverWriter.println("close");
            sslSocket.close();
            for (String str : ipsReceived){
                if (str.split(":").length == 2) {
                    String subjectCN="";
                    try {
                        sslSocket = null;
                        SSLContext sslContext_ = SSLContext.getDefault();
                        SSLSocketFactory sslSocketFactory = sslContext_.getSocketFactory();
                        String[] ip_port = str.split(":");
                        sslSocket = (SSLSocket) sslSocketFactory.createSocket(ip_port[0], Integer.parseInt(ip_port[1]));
                        sslSocket.setNeedClientAuth(true);
                        X509Certificate[] serverCertificates = (X509Certificate[]) sslSocket.getSession().getPeerCertificates();
                        if (serverCertificates.length > 0) {
                            X509Certificate clientCertificate = serverCertificates[0]; // Assuming the client provides a certificate
                            subjectCN = extractSubjectCommonName(clientCertificate);
                            //System.out.println("The first name of the person's certificate -> " + subjectCN);
                            sslSocketUsers.put(subjectCN, sslSocket);
                        }

                        System.out.println("Connected to "+subjectCN+": " + sslSocket);

                    } catch (ConnectException e){
                        System.out.println("User is offline");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (sslSocket != null) {
                    Thread clientThread = new Thread(new ClientHandler(sslSocket));
                    clientThread.start();
                }


            }

            // Start listening to connections
            Thread serverThread = new Thread(new ServerThread(sslServerSocket));
            serverThread.start();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToServerAttribute(String attribute) throws IOException {
        String messageToServer="request:attribute:" + attribute;
        serverWriter.println(messageToServer);
        String read = serverReader.readLine();
        groupUsers.put(attribute, new ArrayList<>());
        if(read.isEmpty())return;
        for (String user : read.split(",")){
            if(!user.equals(userName)){
                groupUsers.get(attribute).add(user);
                sendMessageToServerUsername(user);
            }
        }

        String messageToServer2="request:policy:" + attribute;
        serverWriter.println(messageToServer2);
        String read2 = serverReader.readLine();
        group_accessstring.put("Group_"+attribute,read2);
    }

    public static void sendMessageToServerUsername(String username) throws IOException {
        String messageToServer="request:username:" + username;
        usernameReceiver=username;
        serverWriter.println(messageToServer);
        String read = serverReader.readLine();
        if(read.isEmpty())return;
        ipsReceived.add(read);
    }

    private static String extractSubjectCommonName(X509Certificate certificate) {
        String subjectDN = certificate.getSubjectX500Principal().getName();
        String[] dnComponents = subjectDN.split(",");
        for (String component : dnComponents) {
            if (component.trim().startsWith("CN=")) {
                // Extract the CN value
                return component.trim().substring(3);
            }
        }
        return "Unknown";
    }

    class ServerThread implements Runnable {
        private SSLServerSocket socket;

        public ServerThread(SSLServerSocket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                while (true) {
                    SSLSocket sslSocket = (SSLSocket) socket.accept();
                    System.out.println("New connection accepted: " + sslSocket);

                    // Start a new thread to handle the client
                    Thread clientThread = new Thread(new ClientHandler(sslSocket));
                    clientThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


        public void ConnectToServer(String serverAddress, int serverPort, String username) {
            try {
                SSLContext sslContext = SSLContext.getDefault();
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                sslSocket = (SSLSocket) sslSocketFactory.createSocket(serverAddress, serverPort);
                sslSocket.setNeedClientAuth(true);
                X509Certificate[] serverCertificates = (X509Certificate[]) sslSocket.getSession().getPeerCertificates();
                if (serverCertificates.length > 0) {
                    X509Certificate clientCertificate = serverCertificates[0]; // Assuming the client provides a certificate
                    String subjectCN = extractSubjectCommonName(clientCertificate);
                    //System.out.println("The first name of the person's certificate -> " + subjectCN);
                }

                System.out.println("Connected to server: " + sslSocket);

                serverReader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                serverWriter = new PrintWriter(sslSocket.getOutputStream(), true);
                serverInput = new ObjectInputStream(sslSocket.getInputStream());


            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }


    class ClientHandler implements Runnable {
        private SSLSocket sslSocket;

        public ClientHandler(SSLSocket sslSocket) {
            this.sslSocket = sslSocket;
        }

        public void run() {
            X509Certificate[] clientCertificates = new X509Certificate[0];
            String subjectCN=null;
            try {
                clientCertificates = (X509Certificate[]) sslSocket.getSession().getPeerCertificates();
            } catch (SSLPeerUnverifiedException e) {
                throw new RuntimeException(e);
            }
            if (clientCertificates.length > 0) {
                X509Certificate clientCertificate = clientCertificates[0]; // Assuming the client provides a certificate
                subjectCN = extractSubjectCommonName(clientCertificate);
                //System.out.println("The first name of the person's certificate -> " + subjectCN);
                sslSocketUsers.put(subjectCN,sslSocket);
            }

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                usersReaders.put(subjectCN,reader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String serverCipherSuite = sslSocket.getSession().getCipherSuite();
            //System.out.println("Server Cipher Suite: " + serverCipherSuite);
            String serverTLSVersion = sslSocket.getSession().getProtocol();
            //System.out.println("Server TLS Version: " + serverTLSVersion);
            try {
                while (true) {
                    String receivedMessage = reader.readLine();

                    if (!receivedMessage.equals("close")){
                        if(subjectCN!=null){
                            if (receivedMessage.contains(",")){
                                String[] sections = receivedMessage.split(",");
                                if(!sections[0].contains("Group")){
                                    Message m = new Message(false, receivedMessage.split(",")[0],subjectCN, receivedMessage.split(",")[1]);
                                    messages.get(receivedMessage.split(",")[0]).add(m);
                                    lastMessageReceived=m;
                                    notificationQueue.offer(true);
                                    System.out.println("Received: " + receivedMessage);
                                }else{
                                    String encryptedMessage = sections[1];
                                    String decryptedMessage = App.decryptStringPublic(encryptedMessage,secretKey, group_accessstring.get(sections[0]), attribute_publickey.get(sections[0].split("_")[1]));

                                    Message m = new Message(false, receivedMessage.split(",")[0],subjectCN, decryptedMessage);
                                    messages.get(receivedMessage.split(",")[0]).add(m);
                                    lastMessageReceived=m;
                                    notificationQueue.offer(true);
                                    System.out.println("ReceivedGroup: " + decryptedMessage);

                                }

                            }
                        }

                    }else{
                        System.out.println("close");
                        for (Map.Entry<String, SSLSocket> entry : sslSocketUsers.entrySet()) {
                            if (entry.getValue().equals(sslSocket)) {
                                sslSocketUsers.remove(entry.getKey());
                                break; // Exit loop after the first occurrence is removed
                            }
                        }
                        sslSocket.close();
                        break;
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidCipherTextException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (PolicySyntaxException e) {
                throw new RuntimeException(e);
            }


        }
    }


}
