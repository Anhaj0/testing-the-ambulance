package com.ambulance.dispatch_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ContextLoadTest {

    @Test
    void contextLoads() {
        // Test fails if Spring ApplicationContext cannot start
        // e.g., if there's an invalid @Query in a repository
    }

}
