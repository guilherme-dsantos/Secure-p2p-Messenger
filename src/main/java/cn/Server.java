package cn;

import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.*;


// Server class for tests
class Server {

    static HashMap<String, String> username_ip = new HashMap<>();
    static HashMap<String, List<String>> username_attributes = new HashMap<>();
    static HashMap<String,int[][]> attribute_accesspolicy = new HashMap<>();
    static HashMap<String,String[]> attribute_rhos = new HashMap<>();
    static HashMap<String,String> attribute_accesspolicystring = new HashMap<>();
    static HashMap<String, PairingKeySerParameter> attribute_publickey = new HashMap<>();
    static PairingKeySerParameter publicKey;


    public static void main(String[] args) throws Exception {
        registerAttribute("Alice","Movies");
        registerAttribute("Bob","Movies");
        registerAttribute("Charlie","Movies");
        registerAttribute("Alice","Sports");
        registerAttribute("Bob","Sports");
        registerAttribute("Dave","Sports");
        String str= "Movies and (";
        String str2 = "Sports and (";
        for (String username : username_attributes.keySet()){
            if(username_attributes.get(username).contains("Movies")){
                str=str.concat(username + " or ");
            }
            if(username_attributes.get(username).contains("Sports")){
                str2=str2.concat(username + " or ");
            }
        }
        str = str.substring(0,str.length()-4) + ")";
        str2=str2.substring(0,str.length()-4) + ")";
        //System.out.println(str);
        attribute_accesspolicystring.put("Movies", str);
        attribute_accesspolicystring.put("Sports", str2);

        for (String attribute : attribute_accesspolicystring.keySet()){
            //System.out.println(attribute_accesspolicystring.get(attribute));
            App.defineAccessPolicyString(attribute_accesspolicystring.get(attribute));
            publicKey=App.setup();
            attribute_publickey.put(attribute, publicKey);
        }

        System.setProperty("javax.net.ssl.keyStore", "certs/Server/Serverkeystore.jks");
        System.setProperty("javax.net.ssl.trustStore", "certs/Server/Servertruststore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", System.getenv("server_password"));
        System.setProperty("javax.net.ssl.trustStorePassword", System.getenv("server_password"));

        SSLContext sslContext = SSLContext.getDefault();
        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

        SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(9090);
        sslServerSocket.setNeedClientAuth(true);
        System.out.println("Waiting for client connection...");
        while (true) {
            SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();

            System.out.println("Client connected!");
            String subjectCN = null;
            String clientIp = null;

            X509Certificate[] clientCertificates = (X509Certificate[]) sslSocket.getSession().getPeerCertificates();
            if (clientCertificates.length > 0) {
                X509Certificate clientCertificate = clientCertificates[0]; // Assuming the client provides a certificate
                subjectCN = extractSubjectCommonName(clientCertificate);
                System.out.println("The first name of the person's certificate -> " + subjectCN);

                clientIp = sslSocket.getInetAddress().getHostAddress();
                System.out.println(clientIp);

            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            String serverCipherSuite = sslSocket.getSession().getCipherSuite();
            //System.out.println("Server Cipher Suite: " + serverCipherSuite);
            String serverTLSVersion = sslSocket.getSession().getProtocol();
            //System.out.println("Server TLS Version: " + serverTLSVersion);
            PrintWriter writer = new PrintWriter(sslSocket.getOutputStream(), true);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(sslSocket.getOutputStream());

            HandleUserThread myThread = new HandleUserThread(subjectCN,clientIp, reader, writer,objectOutputStream, sslSocket);
            myThread.start();
        }
    }

    private static String extractSubjectCommonName(X509Certificate certificate) {
        String subjectDN = certificate.getSubjectX500Principal().getName();
        String[] dnComponents = subjectDN.split(",");
        for (String component : dnComponents) {
            if (component.trim().startsWith("CN=")) {
                return component.trim().substring(3);
            }
        }
        return "Unknown";
    }

    public static void registerAttribute(String username, String attribute){
        if(username_attributes.containsKey(username)){
            username_attributes.get(username).add(attribute);
        }else{
            username_attributes.put(username, new ArrayList<>());
            username_attributes.get(username).add(attribute);
        }
    }

    public static String getIpFromUsername(String username){
        if(username_ip.get(username)==null) return "";
        return username_ip.get(username);
    }

    public static String getIpsFromAttribute(String attribute){
        String result="";
        for(String username : username_attributes.keySet()){
            if(username_attributes.get(username).contains(attribute)){
                result=result.concat(username+",");
            }
        }
        if(result.isEmpty()) return result;
        result=result.substring(0,result.length()-1);
        return result;
    }

    public static boolean hasAttribute(String user, String attribute){
        if(username_attributes.containsKey(user)){
            return username_attributes.get(user).contains(attribute);
        }
        return false;
    }

    public static String accessStringForAttribute(String attribute){
        if(attribute_accesspolicystring.containsKey(attribute)){
            return attribute_accesspolicystring.get(attribute);
        }
        return null;
    }

    public static PairingKeySerParameter generateSecretKey(String username){
        List<String> attributes = username_attributes.get(username);
        attributes.add(username);
        try {
            return App.keyGen(attributes.toArray(new String[0]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (PolicySyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}