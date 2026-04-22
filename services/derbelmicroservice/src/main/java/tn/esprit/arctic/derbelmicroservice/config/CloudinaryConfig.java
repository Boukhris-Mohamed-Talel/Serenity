package tn.esprit.arctic.derbelmicroservice.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:dlfad4ti6}")
    private String cloudName;

    @Value("${cloudinary.api-key:656363357221695}")
    private String apiKey;

    @Value("${cloudinary.api-secret:Rqwdh7rs5VT_oEvELnxcxsZXjUw}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        return new Cloudinary(config);
    }
}
