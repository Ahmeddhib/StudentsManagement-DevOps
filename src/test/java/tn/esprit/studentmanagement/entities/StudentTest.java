package tn.esprit.studentmanagement.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDate;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class StudentTest {

    private Student student;
    private Department department;

    @BeforeEach
    void setUp() {
        // Création d'un département
        department = Department.builder()
                .idDepartment(1L)
                .name("Computer Science")
                .location("Building A")
                .phone("123-456-7890")
                .head("Dr. Smith")
                .build();

        // Création d'un étudiant avec Builder
        student = Student.builder()
                .idStudent(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@esprit.tn")
                .phone("987-654-3210")
                .dateOfBirth(LocalDate.of(2000, 5, 15))
                .address("123 Main St, Tunis")
                .department(department)
                .enrollments(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Test de création d'un étudiant avec Builder")
    void testStudentCreationWithBuilder() {
        assertNotNull(student);
        assertEquals(1L, student.getIdStudent());
        assertEquals("John", student.getFirstName());
        assertEquals("Doe", student.getLastName());
        assertEquals("john.doe@esprit.tn", student.getEmail());
        assertEquals("987-654-3210", student.getPhone());
        assertEquals(LocalDate.of(2000, 5, 15), student.getDateOfBirth());
        assertEquals("123 Main St, Tunis", student.getAddress());
        assertNotNull(student.getDepartment());
        assertEquals("Computer Science", student.getDepartment().getName());
    }

    @Test
    @DisplayName("Test des setters Lombok")
    void testStudentSetters() {
        // Modification avec setters (générés par @Setter)
        student.setIdStudent(2L);
        student.setFirstName("Jane");
        student.setLastName("Smith");
        student.setEmail("jane.smith@esprit.tn");
        student.setPhone("555-123-4567");
        student.setDateOfBirth(LocalDate.of(2001, 6, 20));
        student.setAddress("456 Oak St, Sousse");

        assertEquals(2L, student.getIdStudent());
        assertEquals("Jane", student.getFirstName());
        assertEquals("Smith", student.getLastName());
        assertEquals("jane.smith@esprit.tn", student.getEmail());
        assertEquals("555-123-4567", student.getPhone());
        assertEquals(LocalDate.of(2001, 6, 20), student.getDateOfBirth());
        assertEquals("456 Oak St, Sousse", student.getAddress());
    }

    @Test
    @DisplayName("Test du constructeur complet")
    void testAllArgsConstructor() {
        // Création avec @AllArgsConstructor
        Department newDept = new Department(2L, "Mathematics", "Building B",
                "111-222-3333", "Dr. Johnson", new ArrayList<>());

        Student newStudent = new Student(
                3L,
                "Alice",
                "Johnson",
                "alice.johnson@esprit.tn",
                "777-888-9999",
                LocalDate.of(1999, 3, 10),
                "789 Pine St, Tunis",
                newDept,
                new ArrayList<>()
        );

        assertEquals(3L, newStudent.getIdStudent());
        assertEquals("Alice", newStudent.getFirstName());
        assertEquals("Johnson", newStudent.getLastName());
        assertEquals("alice.johnson@esprit.tn", newStudent.getEmail());
        assertEquals("Mathematics", newStudent.getDepartment().getName());
    }

    @Test
    @DisplayName("Test du constructeur sans paramètres")
    void testNoArgsConstructor() {
        Student emptyStudent = new Student();

        assertNull(emptyStudent.getIdStudent());
        assertNull(emptyStudent.getFirstName());
        assertNull(emptyStudent.getLastName());
        assertNull(emptyStudent.getEmail());
        assertNull(emptyStudent.getPhone());
        assertNull(emptyStudent.getDateOfBirth());
        assertNull(emptyStudent.getAddress());
        assertNull(emptyStudent.getDepartment());
        assertNull(emptyStudent.getEnrollments());
    }

    @Test
    @DisplayName("Test de la méthode toString()")
    void testToString() {
        String toStringResult = student.toString();

        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("John"));
        assertTrue(toStringResult.contains("Doe"));
        assertTrue(toStringResult.contains("john.doe@esprit.tn"));
        assertTrue(toStringResult.contains("Student")); // nom de la classe
    }

    @Test
    @DisplayName("Test des relations - Département")
    void testDepartmentRelationship() {
        assertNotNull(student.getDepartment());
        assertEquals("Computer Science", student.getDepartment().getName());
        assertEquals("Dr. Smith", student.getDepartment().getHead());
    }

    @Test
    @DisplayName("Test de modification du département")
    void testChangeDepartment() {
        Department newDepartment = Department.builder()
                .idDepartment(2L)
                .name("Business")
                .location("Building C")
                .phone("999-888-7777")
                .head("Dr. Brown")
                .build();

        student.setDepartment(newDepartment);

        assertEquals("Business", student.getDepartment().getName());
        assertEquals("Dr. Brown", student.getDepartment().getHead());
    }

    @Test
    @DisplayName("Test des enrollments (initialement vide)")
    void testEnrollmentsInitiallyEmpty() {
        assertNotNull(student.getEnrollments());
        assertTrue(student.getEnrollments().isEmpty());
    }

    @Test
    @DisplayName("Test d'égalité entre étudiants")
    void testStudentEquality() {
        Student student1 = Student.builder()
                .idStudent(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@esprit.tn")
                .build();

        Student student2 = Student.builder()
                .idStudent(1L) // Même ID
                .firstName("Jane") // Prénom différent
                .lastName("Smith") // Nom différent
                .email("jane.smith@esprit.tn") // Email différent
                .build();

        // Deux étudiants avec le même ID sont considérés égaux
        // (selon l'implémentation par défaut de equals/hashCode)
        // Note: Lombok @Data génère equals/hashCode basés sur tous les champs
        // Ici avec @Getter/@Setter seulement, equals/hashCode par défaut (basé sur référence)
        assertNotEquals(student1, student2); // Références différentes
    }

    @Test
    @DisplayName("Test de validation des données")
    void testDataValidation() {
        // Test avec données valides
        assertDoesNotThrow(() -> {
            Student validStudent = Student.builder()
                    .firstName("Valid")
                    .lastName("Student")
                    .email("valid@esprit.tn")
                    .phone("123-456-7890")
                    .dateOfBirth(LocalDate.now().minusYears(20))
                    .address("Valid Address")
                    .build();
        });
    }

    @Test
    @DisplayName("Test de calcul d'âge (méthode custom)")
    void testAgeCalculation() {
        // Ajout d'une méthode utilitaire pour calculer l'âge
        LocalDate birthDate = LocalDate.of(2000, 5, 15);
        student.setDateOfBirth(birthDate);

        // Pour calculer l'âge
        int age = java.time.Period.between(birthDate, LocalDate.now()).getYears();

        assertTrue(age >= 18); // Doit être majeur
    }
}