package tn.esprit.studentmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class StudentManagementApplicationTests {

    @Test
    void contextLoads() {
        // Basic test to verify context loads
        assertTrue(true);
    }
}