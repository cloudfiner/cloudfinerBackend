package com.aws;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")   //  Yeh line important hai
class CloudFinerApplicationTests {

    @Test
    void contextLoads() {
        System.out.println("✅ Application Context Loaded Successfully in Test!");
    }
}