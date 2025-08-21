package blog.eric231.examples.mtlsprovider.process;

import blog.eric231.examples.mtlsprovider.domain.Certificate;
import blog.eric231.examples.mtlsprovider.service.CertificateService;
import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.application.usecase.BP;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@BP("certificate-register-process")
@Component
public class CertificateRegisterProcess implements BusinessProcess {

    private final CertificateService certificateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CertificateRegisterProcess(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public JsonNode handle(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            if (!request.has("certificate") || request.get("certificate").asText().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Certificate data is required");
                return response;
            }
            
            String certificatePem = request.get("certificate").asText().trim();
            
            // Validate certificate format
            if (!certificatePem.contains("-----BEGIN CERTIFICATE-----") || 
                !certificatePem.contains("-----END CERTIFICATE-----")) {
                response.put("success", false);
                response.put("message", "Invalid certificate format. PEM format required.");
                return response;
            }
            
            Certificate savedCertificate = certificateService.registerCertificate(certificatePem);
            
            response.put("success", true);
            response.put("message", "Certificate registered successfully");
            response.put("certificateId", savedCertificate.getId());
            response.put("subjectDN", savedCertificate.getSubjectDN());
            response.put("issuerDN", savedCertificate.getIssuerDN());
            response.put("serialNumber", savedCertificate.getSerialNumber());
            response.put("validFrom", savedCertificate.getValidFrom().toString());
            response.put("validTo", savedCertificate.getValidTo().toString());
            response.put("status", savedCertificate.getStatus().toString());
            response.put("fingerprint", savedCertificate.getFingerprintSha256());
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error registering certificate: " + e.getMessage());
        }
        
        return response;
    }
}