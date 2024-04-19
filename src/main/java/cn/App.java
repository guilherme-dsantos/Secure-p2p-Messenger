package cn;


import cn.edu.buaa.crypto.access.parser.ParserUtils;
import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.cpabe.bsw07.CPABEBSW07Engine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;


/**
 * Hello world!
 *
 */
public class App {

    static String pairingParameters = "src/main/java/cn/edu/buaa/crypto/encryption/abe/kpabe/a_80_256.properties";
    static String accessPolicyString; //= "40 and (200 or 430 or 30)";
    static final String[] attributes = new String[]{"40", "200"};
    static int[][] accessPolicy;



    static String[] rhos;



    static PairingParameters pg = PairingFactory.getPairingParameters(pairingParameters);
    static Pairing pairing = PairingFactory.getPairing(pg);
    static PairingKeySerPair keyPair = CPABEBSW07Engine.getInstance().setup(pg, 500);

    static PairingKeySerParameter publicKey = keyPair.getPublic();
    static PairingKeySerParameter masterKey;
    static PairingKeySerParameter secretKey;


    public static byte[] SerCipherParameter(CipherParameters cipherParameters) throws IOException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(cipherParameters);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        objectOutputStream.close();
        byteArrayOutputStream.close();
        return byteArray;
    }

    public static CipherParameters deserCipherParameters(byte[] byteArrays) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrays);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        CipherParameters cipherParameters = (CipherParameters)objectInputStream.readObject();
        objectInputStream.close();
        byteArrayInputStream.close();
        return cipherParameters;
    }

    public static void defineAccessPolicyString(String s) throws PolicySyntaxException {
        accessPolicyString=s;
        accessPolicy = ParserUtils.GenerateAccessPolicy(accessPolicyString);
        rhos = ParserUtils.GenerateRhos(accessPolicyString);
    }

    public static PairingKeySerParameter setup() throws IOException, ClassNotFoundException {

        byte[] byteArrayPublicKey = SerCipherParameter(publicKey);
        CipherParameters anPublicKey = deserCipherParameters(byteArrayPublicKey);

        //System.out.println(publicKey.equals(anPublicKey));
        publicKey = (PairingKeySerParameter) anPublicKey;

        masterKey = keyPair.getPrivate();
        byte[] byteArrayMasterKey = SerCipherParameter(masterKey);
        CipherParameters anMasterKey = deserCipherParameters(byteArrayMasterKey);
        //System.out.println(masterKey.equals(anMasterKey));
        masterKey = (PairingKeySerParameter) anMasterKey;
        return publicKey;
    }

    public static PairingKeySerParameter keyGen(String[] attributes) throws IOException, ClassNotFoundException, PolicySyntaxException {
        secretKey = CPABEBSW07Engine.getInstance().keyGen(publicKey,masterKey,attributes);
        byte[] byteArraySecretKey = SerCipherParameter(secretKey);
        CipherParameters anSecretKey = deserCipherParameters(byteArraySecretKey);
        secretKey = (PairingKeySerParameter) anSecretKey;
        return secretKey;
    }


    public static String encryptStringPublic(String message, String accessString, PairingKeySerParameter publicKey) throws IOException, ClassNotFoundException, PolicySyntaxException {
        System.out.println("Message before encryption" + message);

        Element elementTest = pairing.getGT().newElementFromBytes(message.getBytes(StandardCharsets.UTF_8));
        PairingCipherSerParameter ciphertextTest = CPABEBSW07Engine.getInstance().encryption(publicKey, accessString, elementTest);
        byte[] byteArrayCiphertextTest = SerCipherParameter(ciphertextTest);
        String str = Base64.getEncoder().encodeToString(byteArrayCiphertextTest);
        System.out.println("Message after encryption" + str);
        return str;
    }

    public static String decryptStringPublic(String str, PairingKeySerParameter secretkey,String accessString, PairingKeySerParameter publicKey) throws InvalidCipherTextException, IOException, ClassNotFoundException, PolicySyntaxException {
        System.out.println("Message to decrypt: " + str);
        byte[] receivedBytes = Base64.getDecoder().decode(str);
        PairingCipherSerParameter TestCiphertext = (PairingCipherSerParameter) deserCipherParameters(receivedBytes);
        Element TestMessage = CPABEBSW07Engine.getInstance().decryption(publicKey, secretkey, accessString, TestCiphertext);
        String decryptedReceived = new String(TestMessage.toBytes(), StandardCharsets.UTF_8);
        decryptedReceived=decryptedReceived.replace("\0", "").trim();
        System.out.println("Decrypted Message: " + decryptedReceived.replace("\0", "").trim());
        return decryptedReceived;
    }
}


