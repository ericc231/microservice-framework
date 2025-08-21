package blog.eric231.examples.mtlsprovider.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "certificates")
public class Certificate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "subject_dn", nullable = false, unique = true)
    private String subjectDN;
    
    @Column(name = "issuer_dn", nullable = false)
    private String issuerDN;
    
    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;
    
    @Column(name = "certificate_data", nullable = false, columnDefinition = "TEXT")
    private String certificateData;
    
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;
    
    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status = CertificateStatus.ACTIVE;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    @Column(name = "revocation_reason")
    private String revocationReason;
    
    @Column(name = "fingerprint_sha256")
    private String fingerprintSha256;
    
    @Column(name = "key_usage")
    private String keyUsage;
    
    @Column(name = "extended_key_usage")
    private String extendedKeyUsage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(validTo);
    }
    
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == CertificateStatus.ACTIVE && 
               now.isAfter(validFrom) && 
               now.isBefore(validTo);
    }
    
    public void revoke(String reason) {
        this.status = CertificateStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
        this.revocationReason = reason;
    }
}