package blog.eric231.examples.mtlsprovider.service;

import blog.eric231.examples.mtlsprovider.annotation.Provider;
import blog.eric231.examples.mtlsprovider.domain.Certificate;
import blog.eric231.examples.mtlsprovider.domain.CertificateRepository;
import blog.eric231.examples.mtlsprovider.domain.CertificateStatus;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Provider(value = "mtls-certificate-provider", description = "mTLS Certificate Management Provider", authType = "mtls")
@Service
@Transactional
public class CertificateService {

    private final CertificateRepository certificateRepository;

    public CertificateService(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    public Certificate registerCertificate(String certificatePem) throws Exception {
        X509Certificate x509Cert = parseCertificate(certificatePem);
        
        String fingerprint = calculateSha256Fingerprint(x509Cert);
        
        if (certificateRepository.existsByFingerprintSha256(fingerprint)) {
            throw new IllegalArgumentException("Certificate already exists");
        }
        
        Certificate certificate = new Certificate();
        certificate.setSubjectDN(x509Cert.getSubjectX500Principal().getName());
        certificate.setIssuerDN(x509Cert.getIssuerX500Principal().getName());
        certificate.setSerialNumber(x509Cert.getSerialNumber().toString());
        certificate.setCertificateData(certificatePem);
        certificate.setValidFrom(x509Cert.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        certificate.setValidTo(x509Cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        certificate.setFingerprintSha256(fingerprint);
        certificate.setStatus(CertificateStatus.ACTIVE);
        
        // Extract key usage information
        boolean[] keyUsage = x509Cert.getKeyUsage();
        if (keyUsage != null) {
            StringBuilder keyUsageStr = new StringBuilder();
            String[] usageNames = {"digitalSignature", "nonRepudiation", "keyEncipherment", 
                                 "dataEncipherment", "keyAgreement", "keyCertSign", "cRLSign"};
            for (int i = 0; i < Math.min(keyUsage.length, usageNames.length); i++) {
                if (keyUsage[i]) {
                    if (keyUsageStr.length() > 0) keyUsageStr.append(",");
                    keyUsageStr.append(usageNames[i]);
                }
            }
            certificate.setKeyUsage(keyUsageStr.toString());
        }
        
        return certificateRepository.save(certificate);
    }

    public Optional<Certificate> findBySubjectDN(String subjectDN) {
        return certificateRepository.findBySubjectDN(subjectDN);
    }

    public Optional<Certificate> findByFingerprint(String fingerprint) {
        return certificateRepository.findByFingerprintSha256(fingerprint);
    }

    public boolean validateCertificate(String certificatePem) {
        try {
            X509Certificate x509Cert = parseCertificate(certificatePem);
            String fingerprint = calculateSha256Fingerprint(x509Cert);
            
            Optional<Certificate> certOpt = certificateRepository.findByFingerprintSha256(fingerprint);
            if (certOpt.isEmpty()) {
                return false;
            }
            
            Certificate certificate = certOpt.get();
            return certificate.isValid();
            
        } catch (Exception e) {
            return false;
        }
    }

    public List<Certificate> getAllCertificates() {
        return certificateRepository.findAll();
    }

    public List<Certificate> getActiveCertificates() {
        return certificateRepository.findByStatus(CertificateStatus.ACTIVE);
    }

    public List<Certificate> getExpiredCertificates() {
        return certificateRepository.findExpiredCertificates(LocalDateTime.now());
    }

    public void revokeCertificate(Long certificateId, String reason) {
        Certificate certificate = certificateRepository.findById(certificateId)
            .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        
        certificate.revoke(reason);
        certificateRepository.save(certificate);
    }

    public void updateExpiredCertificates() {
        List<Certificate> expiredCerts = certificateRepository.findExpiredCertificates(LocalDateTime.now());
        for (Certificate cert : expiredCerts) {
            cert.setStatus(CertificateStatus.EXPIRED);
            certificateRepository.save(cert);
        }
    }

    private X509Certificate parseCertificate(String certificatePem) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(certificatePem))) {
            Object object = pemParser.readObject();
            if (object instanceof X509CertificateHolder) {
                return new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) object);
            }
            throw new IllegalArgumentException("Invalid certificate format");
        }
    }

    private String calculateSha256Fingerprint(X509Certificate certificate) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(certificate.getEncoded());
        return Base64.getEncoder().encodeToString(digest);
    }
}