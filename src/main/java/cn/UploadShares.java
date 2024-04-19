package cn;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UploadShares {

    private static final BigInteger field = new BigInteger("8CF83642A709A097B447997640129DA299B1A47D1EB3750BA308B0FE64F5FBD3", 16);
    private static final SecureRandom rndGenerator = new SecureRandom();


    public static void main(String[] args) throws GeneralSecurityException, IOException {
        int polyDegree = 1;		// = threshold - 1
        int nShareholders = 3;	// must be > than polyDegree

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = new SecureRandom();
        //secureRandom.setSeed(new byte[] {0x07, 0x08,0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,0x03,0x08,0x0E,0x04,0x09, 0x0D, 0x05, 0x0A, 0x03, 0x08, 0x01, 0x0A, 0x09});
        keyGen.init(128, secureRandom); // for example
        SecretKey secretKey = keyGen.generateKey();
        byte[] keyBytes = secretKey.getEncoded();
        BigInteger secret = new BigInteger(1, keyBytes);
        byte[] originalKeyBytes = secretKey.getEncoded();
        System.out.println("Original key: " + bytesToHex(originalKeyBytes));
        //random secret for testing purposes; secret should be less than field
        //BigInteger secret = new BigInteger(field.bitLength() - 1, rndGenerator);
        System.out.printf("Secret: %s\n", secret);

        //calculating shares
        UploadShares.Share[] shares = share(polyDegree, nShareholders, secret);

        System.out.println("Shares (shareholder, share):");
        for (UploadShares.Share share : shares) {
            System.out.printf("\t(%s, %s)\n", share.getShareholder(), share.getShare());
            if(share.getShareholder().equals(BigInteger.valueOf(1))) {
                // Write the BigInteger to a text file
                try (PrintWriter writer = new PrintWriter("share_dropbox.txt", "UTF-8")) {
                    writer.println("1," + share.getShare().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (share.getShareholder().equals(BigInteger.valueOf(2))) {
                // Write the BigInteger to a text file
                try (PrintWriter writer = new PrintWriter("share_googledrive.txt", "UTF-8")) {
                    writer.println("2," + share.getShare().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (share.getShareholder().equals(BigInteger.valueOf(3))) {
                // Write the BigInteger to a text file
                try (PrintWriter writer = new PrintWriter("share_github.txt", "UTF-8")) {
                    writer.println("3," + share.getShare().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        DropBox db = new DropBox();
        GoogleDrive gd = new GoogleDrive();
        GitHub gh = new GitHub();
        db.UploadShare("share_dropbox.txt");
        gd.UploadShare("share_googledrive.txt");
        gh.UploadShare("share_github.txt");

        // Specify the names of the files you want to remove
        String[] filesToRemove = {"share_dropbox.txt", "share_googledrive.txt", "share_github.txt"};

        //Clean directory
        String rootPath = System.getProperty("user.dir");

        try {
            // Iterate through each file name and remove it
            for (String fileName : filesToRemove) {
                Path filePath = Paths.get(rootPath, fileName);

                if (Files.exists(filePath)) {
                    // Delete the file
                    Files.delete(filePath);
                    System.out.println("File removed: " + fileName);
                } else {
                    System.out.println("File does not exist: " + fileName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static UploadShares.Share[] share(int polyDegree, int nShareholders, BigInteger secret) {
        //creating polynomial: P(x) = a_d * x^d + ... + a_1 * x^1 + secret
        BigInteger[] polynomial = new BigInteger[polyDegree + 1];

        polynomial[0] = secret;
        for(int i = 1; i < polynomial.length; i++){
            polynomial[i] = new BigInteger(field.bitLength() - 1, rndGenerator);
        }

        //calculating shares
        UploadShares.Share[] shares = new UploadShares.Share[nShareholders];
        for (int i = 0; i < nShareholders; i++) {
            BigInteger shareholder = BigInteger.valueOf(i + 1); //shareholder id can be any positive number, except 0
            BigInteger share = calculatePoint(shareholder, polynomial);
            shares[i] = new Share(shareholder, share);
        }

        return shares;
    }

    public static void uploadEncryptedMessages(String username) throws GeneralSecurityException, IOException {
        DropBox db = new DropBox();
        GoogleDrive gd = new GoogleDrive();
        //GitHub gh = new GitHub();


        Set<String> fileList;
        try (Stream<Path> stream = Files.list(Paths.get("chatsMessages/" + username))) {
            fileList = stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String fileName : fileList) {
            db.UploadFile("chatsMessages/" + username + "/"+fileName);
            gd.UploadFile("chatsMessages/" + username + "/"+fileName);
            //gh.UploadFile(fileName);
        }
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

    private static BigInteger calculatePoint(BigInteger x, BigInteger[] polynomial) {
        BigInteger sum = BigInteger.valueOf(0);
        for( int i = 0; i < polynomial.length; i++){
            sum = sum.add(polynomial[i].multiply(x.pow(i))) ;
        }
        return sum;
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
