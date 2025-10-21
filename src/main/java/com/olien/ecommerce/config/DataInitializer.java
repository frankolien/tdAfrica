package com.olien.ecommerce.config;

import com.olien.ecommerce.services.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    private final AuthService authService;
    
    public DataInitializer(AuthService authService) {
        this.authService = authService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");
        
        try {
            authService.initializeRoles();
            log.info("Data initialization completed successfully");
        } catch (Exception e) {
            log.error("Error during data initialization: {}", e.getMessage());
        }
    }
}
