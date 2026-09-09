package eu.nordlyse.eudi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GenerateEudiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GenerateEudiApplication.class, args);
    }
}
