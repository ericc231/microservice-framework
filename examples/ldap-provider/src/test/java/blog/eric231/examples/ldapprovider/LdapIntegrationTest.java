package blog.eric231.examples.ldapprovider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
import org.junit.jupiter.api.BeforeAll;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "framework.connectors.rest.enabled=true",
        "framework.connectors.rest.authMode=ldap",
        "spring.ldap.embedded.port=8389",
        "spring.ldap.embedded.ldif=classpath:users.ldif",
        "spring.ldap.embedded.base-dn=dc=springframework,dc=org",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.ldap.embedded.EmbeddedLdapAutoConfiguration",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.datasource.initialization-mode=never",
        "spring.ldap.embedded.ssl.enabled=false",
        "jasypt.encryptor.enabled=true"
})
public class LdapIntegrationTest {

    @BeforeAll
    static void setup() throws Exception {
        // 1. Create secret.table
        String charSet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{};':\",./<>?~`";
        StringBuilder tableContent = new StringBuilder();
        while (tableContent.length() < 1024) {
            tableContent.append(charSet);
        }
        List<Character> chars = new ArrayList<>();
        for (char c : tableContent.substring(0, 1024).toCharArray()) {
            chars.add(c);
        }
        Collections.shuffle(chars);
        byte[] tableBytes = new byte[1024];
        for (int i = 0; i < 1024; i++) {
            tableBytes[i] = (byte) chars.get(i).charValue();
        }

        // 2. Shuffle table
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < 4096; i++) {
            int a = rand.nextInt(1024);
            int b = rand.nextInt(1024);
            byte temp = tableBytes[a];
            tableBytes[a] = tableBytes[b];
            tableBytes[b] = temp;
        }

        // 3. Hide password
        String password = "password";
        Set<Integer> usedIndices = new HashSet<>();
        StringBuilder secretIndices = new StringBuilder();
        for (char c : password.toCharArray()) {
            int m;
            do {
                m = rand.nextInt(1024);
            } while (usedIndices.contains(m));
            usedIndices.add(m);
            tableBytes[m] = (byte) c;
            secretIndices.append(String.format("%04x", m));
        }
        try (FileOutputStream fos = new FileOutputStream("secret.table")) {
            fos.write(tableBytes);
        }

        // 4. Create secret.recipe
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] aesKeyBytes = sha256.digest(tableBytes);
        SecretKeySpec secretKey = new SecretKeySpec(aesKeyBytes, "AES");

        String salt = "testsalt";
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] ivBytes = md5.digest(salt.getBytes(StandardCharsets.UTF_8));
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        byte[] encryptedIndices = cipher.doFinal(secretIndices.toString().getBytes(StandardCharsets.UTF_8));

        Properties recipe = new Properties();
        recipe.setProperty("salt", salt);
        recipe.setProperty("password", bytesToHex(encryptedIndices));

        try (FileOutputStream fos = new FileOutputStream("secret.recipe")) {
            recipe.store(fos, null);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }



    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLdapAuthenticationSuccess() throws Exception {
        mockMvc.perform(post("/auth/ldap")
                .header("Authorization", "Basic dXNlcjpwYXNzd29yZA==")) // user:password
                .andExpect(status().isOk());
    }

    @Test
    void testLdapAuthenticationFailure() throws Exception {
        mockMvc.perform(post("/auth/ldap")
                .header("Authorization", "Basic dXNlcjp3cm9uZ3Bhc3N3b3Jk")) // user:wrongpassword
                .andExpect(status().isUnauthorized());
    }
}