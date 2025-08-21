package blog.eric231.examples.mtlsprovider.process;

import blog.eric231.examples.mtlsprovider.domain.Certificate;
import blog.eric231.examples.mtlsprovider.service.CertificateService;
import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.application.usecase.BP;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;

@BP("certificate-management-process")
@Component
public class CertificateManagementProcess implements BusinessProcess {

    private final CertificateService certificateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CertificateManagementProcess(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public JsonNode handle(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            String action = request.has("action") ? request.get("action").asText() : "list";
            
            switch (action.toLowerCase()) {
                case "list":
                    return handleListCertificates();
                case "active":
                    return handleActiveCertificates();
                case "expired":
                    return handleExpiredCertificates();
                case "revoke":
                    return handleRevokeCertificate(request);
                default:
                    return handleListCertificates();
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error in certificate management: " + e.getMessage());
            return response;
        }
    }

    private JsonNode handleListCertificates() {
        ObjectNode response = objectMapper.createObjectNode();
        List<Certificate> certificates = certificateService.getAllCertificates();
        
        ArrayNode certArray = objectMapper.createArrayNode();
        for (Certificate cert : certificates) {
            ObjectNode certNode = createCertificateNode(cert);
            certArray.add(certNode);
        }
        
        response.put("success", true);
        response.put("count", certificates.size());
        response.set("certificates", certArray);
        response.put("message", "Retrieved " + certificates.size() + " certificates");
        
        return response;
    }

    private JsonNode handleActiveCertificates() {
        ObjectNode response = objectMapper.createObjectNode();
        List<Certificate> certificates = certificateService.getActiveCertificates();
        
        ArrayNode certArray = objectMapper.createArrayNode();
        for (Certificate cert : certificates) {
            ObjectNode certNode = createCertificateNode(cert);
            certArray.add(certNode);
        }
        
        response.put("success", true);
        response.put("count", certificates.size());
        response.set("certificates", certArray);
        response.put("message", "Retrieved " + certificates.size() + " active certificates");
        
        return response;
    }

    private JsonNode handleExpiredCertificates() {
        ObjectNode response = objectMapper.createObjectNode();
        List<Certificate> certificates = certificateService.getExpiredCertificates();
        
        ArrayNode certArray = objectMapper.createArrayNode();
        for (Certificate cert : certificates) {
            ObjectNode certNode = createCertificateNode(cert);
            certArray.add(certNode);
        }
        
        response.put("success", true);
        response.put("count", certificates.size());
        response.set("certificates", certArray);
        response.put("message", "Retrieved " + certificates.size() + " expired certificates");
        
        return response;
    }

    private JsonNode handleRevokeCertificate(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        
        if (!request.has("certificateId")) {
            response.put("success", false);
            response.put("message", "Certificate ID is required");
            return response;
        }
        
        Long certificateId = request.get("certificateId").asLong();
        String reason = request.has("reason") ? request.get("reason").asText() : "Manual revocation";
        
        try {
            certificateService.revokeCertificate(certificateId, reason);
            response.put("success", true);
            response.put("message", "Certificate revoked successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to revoke certificate: " + e.getMessage());
        }
        
        return response;
    }

    private ObjectNode createCertificateNode(Certificate cert) {
        ObjectNode certNode = objectMapper.createObjectNode();
        certNode.put("id", cert.getId());
        certNode.put("subjectDN", cert.getSubjectDN());
        certNode.put("issuerDN", cert.getIssuerDN());
        certNode.put("serialNumber", cert.getSerialNumber());
        certNode.put("validFrom", cert.getValidFrom().toString());
        certNode.put("validTo", cert.getValidTo().toString());
        certNode.put("status", cert.getStatus().toString());
        certNode.put("fingerprint", cert.getFingerprintSha256());
        certNode.put("isExpired", cert.isExpired());
        certNode.put("isValid", cert.isValid());
        certNode.put("createdAt", cert.getCreatedAt().toString());
        
        if (cert.getRevokedAt() != null) {
            certNode.put("revokedAt", cert.getRevokedAt().toString());
            certNode.put("revocationReason", cert.getRevocationReason());
        }
        
        return certNode;
    }
}