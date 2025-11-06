package com.cgv.mega;

import com.cgv.mega.containers.TestContainerManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("test")
@SpringBootTest
class MegaApplicationTests {

    @Test
    void contextLoads() {
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
    }

}
