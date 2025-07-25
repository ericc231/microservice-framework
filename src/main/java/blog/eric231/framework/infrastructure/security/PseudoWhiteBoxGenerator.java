package blog.eric231.framework.infrastructure.security;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class PseudoWhiteBoxGenerator {

    private static final int ITERATION_BASE = 1000;
    private static final int SALT_LENGTH = 16;

    /**
     * Generates the secret.table and secret.recipe files.
     *
     * @param secret The secret string to be protected.
     * @param tablePath The path to save the secret.table file.
     * @param recipePath The path to save the secret.recipe file.
     * @throws IOException If an I/O error occurs.
     */
    public static void generate(String secret, String tablePath, String recipePath) throws IOException {
        char[] secretChars = secret.toCharArray();
        int secretLength = secretChars.length;

        // 1. Generate the shuffled character list (secret.table)
        List<Character> charList = new ArrayList<>();
        for (char c : secretChars) {
            charList.add(c);
        }
        // Add some dummy characters to make it harder to guess the length
        for (int i = 0; i < secretLength * 2; i++) {
            charList.add((char) (new SecureRandom().nextInt(26) + 'a'));
        }
        Collections.shuffle(charList);

        StringBuilder tableContent = new StringBuilder();
        for (Character c : charList) {
            tableContent.append(c);
        }

        // 2. Generate the recipe (secret.recipe)
        Properties recipe = new Properties();
        int[] indices = new int[secretLength];
        for (int i = 0; i < secretLength; i++) {
            indices[i] = charList.indexOf(secretChars[i]);
        }

        // 3. Generate salt and iterations for obfuscation
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        int iterations = ITERATION_BASE + random.nextInt(1000);

        // 4. Obfuscate indices
        int[] obfuscatedIndices = new int[secretLength];
        for (int i = 0; i < secretLength; i++) {
            obfuscatedIndices[i] = indices[i] ^ (iterations + i) ^ salt[i % SALT_LENGTH];
        }

        // 5. Save the recipe
        recipe.setProperty("salt", bytesToHex(salt));
        recipe.setProperty("iterations", String.valueOf(iterations));
        recipe.setProperty("indices", intArrayToString(obfuscatedIndices));

        // Write files
        try (FileOutputStream tableOut = new FileOutputStream(tablePath)) {
            tableOut.write(tableContent.toString().getBytes());
        }

        try (FileOutputStream recipeOut = new FileOutputStream(recipePath)) {
            recipe.store(recipeOut, "Secret Recipe");
        }
    }

    private static String intArrayToString(int[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        // --- Usage Example ---
        // Replace with your actual secret password
        String mySecretPassword = "ThisIsAStrongPasswordForJasypt!";

        // Define where to save the generated files
        String tableFilePath = "secret.table";
        String recipeFilePath = "secret.recipe";

        generate(mySecretPassword, tableFilePath, recipeFilePath);

        System.out.println("Successfully generated '" + tableFilePath + "' and '" + recipeFilePath + "'.");
        System.out.println("Please move these files to a secure location accessible by your application.");
    }
}
