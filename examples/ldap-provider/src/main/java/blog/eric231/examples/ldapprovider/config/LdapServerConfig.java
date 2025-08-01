package blog.eric231.examples.ldapprovider.config;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.util.ssl.SSLUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@Configuration
public class LdapServerConfig {

    @Bean
    public InMemoryDirectoryServer directoryServer() throws Exception {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=springframework,dc=org");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 389));
        config.setSchema(null);

        InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);

        String ldifContent = "dn: dc=springframework,dc=org\n" +
                             "objectClass: top\n" +
                             "objectClass: domain\n\n" +
                             "dn: ou=people,dc=springframework,dc=org\n" +
                             "objectclass: top\n" +
                             "objectclass: organizationalUnit\n" +
                             "ou: people\n\n" +
                             "dn: uid=admin,ou=people,dc=springframework,dc=org\n" +
                             "objectclass: top\n" +
                             "objectclass: person\n" +
                             "objectclass: organizationalPerson\n" +
                             "objectclass: inetOrgPerson\n" +
                             "cn: admin\n" +
                             "sn: admin\n" +
                             "uid: admin\n" +
                             "userPassword: {noop}admin\n\n" +
                             "dn: uid=user,ou=people,dc=springframework,dc=org\n" +
                             "objectclass: top\n" +
                             "objectclass: person\n" +
                             "objectclass: organizationalPerson\n" +
                             "objectclass: inetOrgPerson\n" +
                             "cn: user\n" +
                             "sn: user\n" +
                             "uid: user\n" +
                             "userPassword: {noop}password";

        java.io.File tempFile = java.io.File.createTempFile("ldif", ".ldif");
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write(ldifContent);
        }

        server.importFromLDIF(true, tempFile.getAbsolutePath());
        tempFile.delete();

        server.startListening();
        return server;
    }
}
