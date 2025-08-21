package blog.eric231.examples.mtlsauthrest.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientSessionRepository extends JpaRepository<ClientSession, Long> {
    
    Optional<ClientSession> findByClientDNAndIsActiveTrue(String clientDN);
    
    Optional<ClientSession> findBySessionIdAndIsActiveTrue(String sessionId);
    
    Optional<ClientSession> findByCertificateFingerprintAndIsActiveTrue(String certificateFingerprint);
    
    List<ClientSession> findByIsActiveTrueOrderByLastAccessedDesc();
    
    List<ClientSession> findByClientDNOrderByCreatedAtDesc(String clientDN);
    
    List<ClientSession> findByLastAccessedBeforeAndIsActiveTrue(LocalDateTime cutoffTime);
    
    @Query("SELECT COUNT(c) FROM ClientSession c WHERE c.isActive = true")
    long countActiveSessions();
    
    @Query("SELECT c FROM ClientSession c WHERE c.isActive = true AND c.lastAccessed > :since")
    List<ClientSession> findRecentActiveSessions(@Param("since") LocalDateTime since);
    
    boolean existsByClientDNAndIsActiveTrue(String clientDN);
    
    boolean existsByCertificateFingerprintAndIsActiveTrue(String certificateFingerprint);
}