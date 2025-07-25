package blog.eric231.framework.infrastructure.security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SelfSignedCertificateGenerator {

    public static void generate() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name owner = new X500Name("CN=localhost");
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                owner, new BigInteger("1"), new Date(), new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L), owner, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);
        X509Certificate certificate = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().getCertificate(certHolder);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("selfsigned", keyPair.getPrivate(), "password".toCharArray(), new java.security.cert.Certificate[]{certificate});

        try (FileOutputStream fos = new FileOutputStream("keystore.p12")) {
            keyStore.store(fos, "password".toCharArray());
        }
    }
}
