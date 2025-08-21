package blog.eric231.examples.mtlsprovider;

import blog.eric231.examples.mtlsprovider.annotation.Provider;
import blog.eric231.examples.mtlsprovider.domain.Certificate;
import blog.eric231.examples.mtlsprovider.domain.CertificateStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MtlsProviderSimpleTest {

    @Test
    void testProviderAnnotationExists() {
        // Test that Provider annotation exists and has correct values
        assertTrue(Provider.class.isAnnotation());
        assertEquals("blog.eric231.examples.mtlsprovider.annotation.Provider", Provider.class.getName());
    }

    @Test
    void testCertificateEntityBasicFunctionality() {
        // Test basic certificate entity functionality without database
        Certificate cert = new Certificate();
        cert.setSubjectDN("CN=Test");
        cert.setStatus(CertificateStatus.ACTIVE);
        cert.setValidFrom(LocalDateTime.now());
        cert.setValidTo(LocalDateTime.now().plusYears(1));
        
        assertEquals("CN=Test", cert.getSubjectDN());
        assertEquals(CertificateStatus.ACTIVE, cert.getStatus());
        assertTrue(cert.isValid());
        assertFalse(cert.isExpired());
    }

    @Test
    void testCertificateStatusEnum() {
        // Test certificate status enum
        assertEquals("ACTIVE", CertificateStatus.ACTIVE.name());
        assertEquals("REVOKED", CertificateStatus.REVOKED.name());
        assertEquals("EXPIRED", CertificateStatus.EXPIRED.name());
    }
}