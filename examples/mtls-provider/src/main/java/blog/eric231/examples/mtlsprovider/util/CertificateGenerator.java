package blog.eric231.examples.mtlsprovider.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class CertificateGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public void generateServerKeyStoreAndTrustStore() throws Exception {
        // Server keystore with server certificate
        KeyPair serverKeyPair = generateKeyPair();
        X509Certificate serverCert = generateCertificate(
            serverKeyPair, 
            "CN=mTLS Server, OU=mTLS Provider, O=Microservice Framework, C=US",
            "CN=mTLS CA, OU=mTLS Provider, O=Microservice Framework, C=US",
            serverKeyPair.getPrivate(),
            false
        );
        
        saveKeyStore("mtls-keystore.p12", "mtls-server", serverKeyPair.getPrivate(), serverCert, "mtlspass");
        
        // Generate CA certificate for trust store
        KeyPair caKeyPair = generateKeyPair();
        X509Certificate caCert = generateCertificate(
            caKeyPair,
            "CN=mTLS CA, OU=mTLS Provider, O=Microservice Framework, C=US",
            "CN=mTLS CA, OU=mTLS Provider, O=Microservice Framework, C=US",
            caKeyPair.getPrivate(),
            true
        );
        
        saveTrustStore("mtls-truststore.p12", "mtls-ca", caCert, "mtlspass");
        
        // Generate sample client certificate
        KeyPair clientKeyPair = generateKeyPair();
        X509Certificate clientCert = generateCertificate(
            clientKeyPair,
            "CN=Test Client, OU=mTLS Client, O=Microservice Framework, C=US",
            "CN=mTLS CA, OU=mTLS Provider, O=Microservice Framework, C=US",
            caKeyPair.getPrivate(),
            false
        );
        
        saveKeyStore("client-keystore.p12", "client", clientKeyPair.getPrivate(), clientCert, "clientpass");
        
        System.out.println("Generated SSL certificates:");
        System.out.println("- Server keystore: mtls-keystore.p12 (password: mtlspass)");
        System.out.println("- Trust store: mtls-truststore.p12 (password: mtlspass)");
        System.out.println("- Client keystore: client-keystore.p12 (password: clientpass)");
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private X509Certificate generateCertificate(KeyPair keyPair, String subjectDN, String issuerDN, 
                                              PrivateKey signingKey, boolean isCa) 
            throws CertificateException, OperatorCreationException, IOException {
        
        X500Name subject = new X500Name(subjectDN);
        X500Name issuer = new X500Name(issuerDN);
        
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Date validFrom = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        Date validTo = Date.from(LocalDateTime.now().plusYears(1).atZone(ZoneId.systemDefault()).toInstant());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, serialNumber, validFrom, validTo, subject, keyPair.getPublic());

        // Add extensions
        if (isCa) {
            certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            certBuilder.addExtension(Extension.keyUsage, true, 
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } else {
            certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
            certBuilder.addExtension(Extension.keyUsage, true, 
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        }

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(signingKey);

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder);
    }

    private void saveKeyStore(String filename, String alias, PrivateKey privateKey, 
                             X509Certificate certificate, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), new X509Certificate[]{certificate});
        
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            keyStore.store(fos, password.toCharArray());
        }
    }

    private void saveTrustStore(String filename, String alias, X509Certificate certificate, 
                               String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setCertificateEntry(alias, certificate);
        
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            keyStore.store(fos, password.toCharArray());
        }
    }
}