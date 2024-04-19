package cn;


import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;

public class DownloadShares {

    private static final BigInteger field = new BigInteger("8CF83642A709A097B447997640129DA299B1A47D1EB3750BA308B0FE64F5FBD3", 16);
    private static final String CIPHER = "AES/CBC/PKCS5Padding";
    private static final byte[] IV = new byte[16]; // TODO: Use a random IV and store it for decryption
    String[] messages = {"test1", "test2", "test3"};

    public static BigInteger getSecret(){
        DropBox db = new DropBox();
        GoogleDrive gd = null;
        try {
            gd = new GoogleDrive();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //GitHub gh = new GitHub();
        db.GetShare("share_dropbox.txt");
        gd.GetShare("share_googledrive.txt");
        //gh.GetShare("share_github.txt");

        // Specify the folder path
        String folderPath = "shares";
        Share[] shares;
        // Create a File object for the folder
        File folder = new File(folderPath);
        int shareHolder = 1;
        // Check if the specified path is a directory
        if (folder.isDirectory()) {
            // Get all files in the directory
            File[] files = folder.listFiles();
            shares = new Share[files.length];
            int index = 0;
            // Iterate through each file
            for (File file : files) {
                // Check if the file is a text file
                if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
                    // Read and print the content of the text file
                    //System.out.println("Reading share: " + file.getName());
                    shares[index++] = readTextFileAsShare(file);
                }
            }
            BigInteger recoveredSecret = combine(shares);
            return recoveredSecret;
        } else {
            System.out.println("Specified path is not a directory.");
            return null;
        }
    }
    public static void encryptMessage(String fileName, List<String> messages, BigInteger recoveredSecret) throws Exception {

            byte[] recoveredKeyBytes = recoveredSecret.toByteArray();


            // Convert the byte array back to a SecretKey
            SecretKey recoveredAesKey = new SecretKeySpec(recoveredKeyBytes, "AES");


            FileEncrypterDecrypter xd = new FileEncrypterDecrypter(recoveredAesKey, "AES/CBC/PKCS5Padding");
            List<String> oldMessages = xd.decrypt(fileName);
            oldMessages.addAll(messages);
            xd.encrypt(oldMessages,fileName);


    }
    public static List<String> decryptMessages(String fileName, BigInteger recoveredSecret) throws Exception {


            byte[] recoveredKeyBytes = recoveredSecret.toByteArray();


            // Convert the byte array back to a SecretKey
            SecretKey recoveredAesKey = new SecretKeySpec(recoveredKeyBytes, "AES");
            //System.out.println(bytesToHex(recoveredAesKey.getEncoded()));

            FileEncrypterDecrypter xd = new FileEncrypterDecrypter(recoveredAesKey, "AES/CBC/PKCS5Padding");
            List<String> oldMessages = xd.decrypt(fileName);
            xd.encrypt(oldMessages, fileName);
            String[] filesToRemove = {"shares/share_dropbox.txt", "shares/share_googledrive.txt", "shares/share_github.txt"};

            //Clean directory
            String rootPath = System.getProperty("user.dir");

            try {
                // Iterate through each file name and remove it
                for (String file : filesToRemove) {
                    Path filePath = Paths.get(rootPath, file);

                    if (Files.exists(filePath)) {
                        // Delete the file
                        Files.delete(filePath);
                        System.out.println("File removed: " + file);
                    } else {
                        System.out.println("File does not exist: " + file);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return oldMessages;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    private static Share readTextFileAsShare(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                // Split the line using a comma
                String[] parts = line.split(",");

                // Ensure there are two parts (shareholder and share)
                if (parts.length == 2) {
                    BigInteger shareholder = new BigInteger(parts[0].trim());
                    BigInteger share = new BigInteger(parts[1].trim());

                    //System.out.println("Shareholder: " + shareholder);
                    //System.out.println("Share: " + share);

                    // Create and return a new Share object
                    return new Share(shareholder, share);
                } else {
                    System.err.println("Invalid format in file: " + file.getName());
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        // Return null if there was an issue reading or parsing the content
        return null;
    }


    /**
     * This method combines shares, using Lagrange polynomials, to recover the secret.
     * 	Lagrange's polynomials: https://en.wikipedia.org/wiki/Lagrange_polynomial.
     * @param shares Shares of the secret.
     * @return Recovered secret.
     */
    private static BigInteger combine(DownloadShares.Share[] shares) {

        BigInteger result = BigInteger.valueOf(0);

        for(int j = 0; j < shares.length; j++){
            BigInteger upper = BigInteger.valueOf(1);
            BigInteger lower = BigInteger.valueOf(1);
            BigInteger divisionResult = BigInteger.valueOf(0);
            for( int m = 0; m < shares.length; m++) {
                if ( j != m) {
                    upper = upper.multiply(shares[m].getShareholder());
                    lower = lower.multiply(shares[m].getShareholder().subtract(shares[j].getShareholder()));
                }
            }
            divisionResult = upper.divide(lower);
            BigInteger aux = shares[j].getShare().multiply(divisionResult);
            result = result.add(aux);
        }

        return result;

    }
    private static class Share {
        private final BigInteger shareholder;
        private final BigInteger share;

        private Share(BigInteger shareholder, BigInteger share) {
            this.shareholder = shareholder;
            this.share = share;
        }

        public BigInteger getShare() {
            return share;
        }

        public BigInteger getShareholder() {
            return shareholder;
        }
    }
}
