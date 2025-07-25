package blog.eric231.framework.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VaultService {

    @Value("${my-secret}")
    private String mySecret;

    public String getMySecret() {
        return mySecret;
    }
}
