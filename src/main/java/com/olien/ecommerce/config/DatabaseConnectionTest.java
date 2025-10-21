package com.olien.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionTest implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionTest.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseConnectionTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public void run(String... args) throws Exception {
        try {
            // Test database connection
            String result = jdbcTemplate.queryForObject("SELECT 'PostgreSQL Connection Successful!' as message", String.class);
            log.info("✅ Database Connection Test: {}", result);
            
            // Test if tables exist
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'", 
                Integer.class
            );
            log.info("📊 Database Tables Count: {}", tableCount);
            
        } catch (Exception e) {
            log.error("❌ Database Connection Failed: {}", e.getMessage());
            throw e;
        }
    }
}
