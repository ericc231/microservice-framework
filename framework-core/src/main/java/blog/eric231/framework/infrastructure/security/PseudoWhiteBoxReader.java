package blog.eric231.framework.infrastructure.security;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Properties;

@Component
public class PseudoWhiteBoxReader {

    public String reconstructKey(String tablePath, String recipePath) throws Exception {
        // 1. Read recipe and table
        Properties recipe = new Properties();
        try (FileInputStream recipeIn = new FileInputStream(recipePath)) {
            recipe.load(recipeIn);
        }
        String salt = recipe.getProperty("salt");
        String iterationsStr = recipe.getProperty("iterations");
        String encryptedIndicesHex = recipe.getProperty("password");
        
        if (salt == null || iterationsStr == null || encryptedIndicesHex == null) {
            throw new IllegalArgumentException("Recipe file is missing required properties");
        }
        
        int iterations = Integer.parseInt(iterationsStr);
        byte[] saltBytes = hexToBytes(salt);

        byte[] tableBytes = Files.readAllBytes(Paths.get(tablePath));
        String tableContent = new String(tableBytes, StandardCharsets.UTF_8);

        // 2. Prepare for Decryption
        // AES Key is SHA-256 of the table
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] aesKeyBytes = sha256.digest(tableBytes);
        SecretKeySpec secretKey = new SecretKeySpec(aesKeyBytes, "AES");

        // IV is MD5 of the salt
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] ivBytes = md5.digest(salt.getBytes(StandardCharsets.UTF_8));
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        // 3. Decrypt Indices
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encryptedIndices = hexToBytes(encryptedIndicesHex);
        byte[] decryptedIndicesBytes = cipher.doFinal(encryptedIndices);
        String decryptedIndicesHex = new String(decryptedIndicesBytes, StandardCharsets.UTF_8);

        // 4. Reconstruct the Password
        StringBuilder secret = new StringBuilder();
        int charIndex = 0;
        for (int i = 0; i < decryptedIndicesHex.length(); i += 4) {
            String hexIndex = decryptedIndicesHex.substring(i, i + 4);
            int obfuscatedIndex = Integer.parseInt(hexIndex, 16);
            
            // Reverse the XOR obfuscation: index ^ (iterations + charIndex) ^ salt[charIndex % SALT_LENGTH]
            int deobfuscatedIndex = obfuscatedIndex ^ (iterations + charIndex) ^ (saltBytes[charIndex % saltBytes.length] & 0xFF);
            
            if (deobfuscatedIndex >= 0 && deobfuscatedIndex < tableContent.length()) {
                secret.append(tableContent.charAt(deobfuscatedIndex));
            } else {
                throw new IllegalStateException("Deobfuscated index " + deobfuscatedIndex + " out of bounds for table length " + tableContent.length() + 
                    " (original obfuscated: " + obfuscatedIndex + ", charIndex: " + charIndex + ", iterations: " + iterations + ")");
            }
            charIndex++;
        }

        return secret.toString();
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even number of characters.");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
