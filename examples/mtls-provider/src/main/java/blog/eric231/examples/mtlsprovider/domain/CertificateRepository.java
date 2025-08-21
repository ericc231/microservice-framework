package blog.eric231.examples.mtlsprovider.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    
    Optional<Certificate> findBySubjectDN(String subjectDN);
    
    Optional<Certificate> findBySerialNumber(String serialNumber);
    
    Optional<Certificate> findByFingerprintSha256(String fingerprint);
    
    List<Certificate> findByStatus(CertificateStatus status);
    
    List<Certificate> findByValidToBeforeAndStatus(LocalDateTime dateTime, CertificateStatus status);
    
    @Query("SELECT c FROM Certificate c WHERE c.validTo < :now AND c.status = 'ACTIVE'")
    List<Certificate> findExpiredCertificates(@Param("now") LocalDateTime now);
    
    @Query("SELECT c FROM Certificate c WHERE c.validTo BETWEEN :start AND :end AND c.status = 'ACTIVE'")
    List<Certificate> findCertificatesExpiringBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    boolean existsBySubjectDN(String subjectDN);
    
    boolean existsBySerialNumber(String serialNumber);
    
    boolean existsByFingerprintSha256(String fingerprint);
    
    @Query("SELECT COUNT(c) FROM Certificate c WHERE c.status = :status")
    long countByStatus(@Param("status") CertificateStatus status);
}