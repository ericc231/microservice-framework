package blog.eric231.examples.mtlsprovider.process;

import blog.eric231.examples.mtlsprovider.service.CertificateService;
import blog.eric231.framework.application.usecase.BusinessProcess;
import blog.eric231.framework.application.usecase.BP;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.cert.X509Certificate;

@BP("mtls-auth-process")
@Component
public class MtlsAuthProcess implements BusinessProcess {

    private final CertificateService certificateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MtlsAuthProcess(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public JsonNode handle(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        
        try {
            HttpServletRequest httpRequest = getCurrentHttpRequest();
            if (httpRequest == null) {
                response.put("authenticated", false);
                response.put("message", "No HTTP request context available");
                return response;
            }

            X509Certificate[] certificates = (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
            
            if (certificates == null || certificates.length == 0) {
                response.put("authenticated", false);
                response.put("message", "No client certificate provided");
                response.put("authType", "mtls");
                return response;
            }

            X509Certificate clientCert = certificates[0];
            String subjectDN = clientCert.getSubjectX500Principal().getName();
            
            // Convert certificate to PEM format for validation
            String certPem = convertToPem(clientCert);
            boolean isValid = certificateService.validateCertificate(certPem);

            if (isValid) {
                response.put("authenticated", true);
                response.put("subjectDN", subjectDN);
                response.put("issuerDN", clientCert.getIssuerX500Principal().getName());
                response.put("serialNumber", clientCert.getSerialNumber().toString());
                response.put("validFrom", clientCert.getNotBefore().toString());
                response.put("validTo", clientCert.getNotAfter().toString());
                response.put("authType", "mtls");
                response.put("message", "Client certificate validated successfully");
            } else {
                response.put("authenticated", false);
                response.put("subjectDN", subjectDN);
                response.put("message", "Client certificate validation failed");
                response.put("authType", "mtls");
            }

        } catch (Exception e) {
            response.put("authenticated", false);
            response.put("message", "Error during mTLS authentication: " + e.getMessage());
            response.put("authType", "mtls");
        }

        return response;
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String convertToPem(X509Certificate certificate) throws Exception {
        java.util.Base64.Encoder encoder = java.util.Base64.getMimeEncoder(64, "\n".getBytes());
        String encoded = encoder.encodeToString(certificate.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + encoded + "\n-----END CERTIFICATE-----";
    }
}