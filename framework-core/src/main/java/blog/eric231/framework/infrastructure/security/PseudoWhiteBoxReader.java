package blog.eric231.framework.infrastructure.security;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Component
public class PseudoWhiteBoxReader {

    public String reconstructKey(String tablePath, String recipePath) throws IOException {
        // 1. Read the recipe file
        Properties recipe = new Properties();
        try (FileInputStream recipeIn = new FileInputStream(recipePath)) {
            recipe.load(recipeIn);
        }

        byte[] salt = hexToBytes(recipe.getProperty("salt"));
        int iterations = Integer.parseInt(recipe.getProperty("iterations"));
        String indicesStr = recipe.getProperty("indices");
        if (indicesStr == null || indicesStr.isEmpty()) {
            return "";
        }
        String[] obfuscatedIndicesStr = indicesStr.split(",");

        // 2. De-obfuscate the indices
        int[] indices = new int[obfuscatedIndicesStr.length];
        for (int i = 0; i < obfuscatedIndicesStr.length; i++) {
            int obfuscatedIndex = Integer.parseInt(obfuscatedIndicesStr[i]);
            indices[i] = obfuscatedIndex ^ (iterations + i) ^ salt[i % salt.length];
        }

        // 3. Read the secret table
        String tableContent = new String(Files.readAllBytes(Paths.get(tablePath)));

        // 4. Reconstruct the secret
        StringBuilder secret = new StringBuilder();
        for (int index : indices) {
            secret.append(tableContent.charAt(index));
        }

        return secret.toString();
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new NumberFormatException("Invalid hex string");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
