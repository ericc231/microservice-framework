package blog.eric231.framework.infrastructure.security;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SelfSignedCertificateGenerator {

    public static void generate(String outputPath) throws Exception {
        // Generate RSA key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Certificate details
        X500Name issuer = new X500Name("CN=Microservice Framework CA, OU=Framework, O=Microservice Framework, C=US");
        X500Name subject = new X500Name("CN=localhost, OU=Microservice, O=Microservice Framework, C=US");
        
        // Generate random serial number
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        
        // Certificate validity - 2 years
        Date validityStartDate = new Date();
        Date validityEndDate = new Date(System.currentTimeMillis() + (2L * 365 * 24 * 60 * 60 * 1000)); // 2 years

        // Create certificate builder
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serialNumber, validityStartDate, validityEndDate, subject, keyPair.getPublic());

        // Add extensions
        
        // Basic Constraints - not a CA certificate
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        
        // Key Usage - for server authentication
        certBuilder.addExtension(Extension.keyUsage, true, 
            new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.digitalSignature));
            
        // Subject Alternative Names - include multiple hostnames
        GeneralName[] altNames = new GeneralName[]{
            new GeneralName(GeneralName.dNSName, "localhost"),
            new GeneralName(GeneralName.dNSName, "127.0.0.1"),
            new GeneralName(GeneralName.dNSName, "0.0.0.0"),
            new GeneralName(GeneralName.dNSName, "*.localhost"),
            new GeneralName(GeneralName.iPAddress, "127.0.0.1"),
            new GeneralName(GeneralName.iPAddress, "::1")
        };
        certBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(altNames));

        // Extended Key Usage - server authentication
        ASN1ObjectIdentifier serverAuth = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.3.1"); // serverAuth
        ASN1ObjectIdentifier clientAuth = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.3.2"); // clientAuth
        certBuilder.addExtension(Extension.extendedKeyUsage, false, 
            new DERSequence(new ASN1Encodable[]{serverAuth, clientAuth}));

        // Sign the certificate
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);
        X509Certificate certificate = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().getCertificate(certHolder);

        // Create PKCS12 keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("selfsigned", keyPair.getPrivate(), "password".toCharArray(), 
            new java.security.cert.Certificate[]{certificate});

        // Save keystore
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            keyStore.store(fos, "password".toCharArray());
        }
        
        System.out.println("Self-signed certificate generated: " + outputPath);
        System.out.println("Certificate Details:");
        System.out.println("  Subject: " + subject);
        System.out.println("  Valid from: " + validityStartDate);
        System.out.println("  Valid to: " + validityEndDate);
        System.out.println("  Serial: " + serialNumber.toString(16));
        System.out.println("  Key Usage: Digital Signature, Key Encipherment");
        System.out.println("  Extended Key Usage: Server Authentication, Client Authentication");
        System.out.println("  Subject Alternative Names: localhost, 127.0.0.1, ::1");
    }
}
