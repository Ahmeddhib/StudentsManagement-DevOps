package tn.esprit.studentmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")  // Utilise le profil "test" avec H2
class StudentManagementApplicationTests {

    @Test
    void contextLoads() {
        // Test que le contexte Spring démarre
        assertTrue(true, "Le contexte Spring devrait démarrer");
    }

    @Test
    void basicMathTest() {
        int result = 2 + 2;
        assertTrue(result == 4, "2+2 devrait être 4");
    }
}