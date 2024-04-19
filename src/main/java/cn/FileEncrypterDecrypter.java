package cn;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class FileEncrypterDecrypter {

    private SecretKey secretKey;
    private Cipher cipher;

    FileEncrypterDecrypter(SecretKey secretKey, String transformation) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.secretKey = secretKey;
        this.cipher = Cipher.getInstance(transformation);
    }

    public void encrypt(List<String> content, String fileName) throws InvalidKeyException {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();

        try (FileOutputStream fileOut = new FileOutputStream(fileName);
             CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
            fileOut.write(iv);
            for(String s : content){
                cipherOut.write(s.getBytes());
                cipherOut.write(System.lineSeparator().getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> decrypt(String fileName) throws IOException {

        List<String> tmp = new ArrayList<>();

        File yourFile = new File(fileName);
        if (!yourFile.exists()){
            int lastSlash = fileName.lastIndexOf("/");
            if (lastSlash != -1) {
                String replacedString = fileName.substring(0, lastSlash) +
                        "/Group_" +
                        fileName.substring(lastSlash + 1);
                yourFile = new File(replacedString);
            }
        }
        yourFile.createNewFile();
        try (FileInputStream fileIn = new FileInputStream(fileName)) {
            byte[] fileIv = new byte[16];
            fileIn.read(fileIv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(fileIv));

            try (
                    CipherInputStream cipherIn = new CipherInputStream(fileIn, cipher);
                    InputStreamReader inputReader = new InputStreamReader(cipherIn);
                    BufferedReader reader = new BufferedReader(inputReader)
            ) {

                String line;
                while ((line = reader.readLine()) != null) {
                    tmp.add(line);
                }
            }
        } catch (IOException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

        return tmp;
    }
}

