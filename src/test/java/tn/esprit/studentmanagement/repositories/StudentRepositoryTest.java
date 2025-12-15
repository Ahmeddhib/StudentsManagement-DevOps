package tn.esprit.studentmanagement.repositories;

import tn.esprit.studentmanagement.entities.Student;
import tn.esprit.studentmanagement.entities.Department;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void testFindByEmail_Success() {
        // Given : Préparation des données
        Department department = Department.builder()
                .name("Computer Science")
                .location("Building A")
                .phone("123-456-7890")
                .head("Dr. Smith")
                .build();

        entityManager.persist(department);

        Student student = Student.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@esprit.tn")
                .phone("987-654-3210")
                .dateOfBirth(LocalDate.of(2000, 5, 15))
                .address("123 Main St, Tunis")
                .department(department)
                .build();

        entityManager.persist(student);
        entityManager.flush();

        // When : Exécution de la méthode
        Student found = studentRepository.findByEmail("john.doe@esprit.tn");

        // Then : Vérification des résultats
        assertNotNull(found);
        assertEquals("John", found.getFirstName());
        assertEquals("Doe", found.getLastName());
        assertEquals("john.doe@esprit.tn", found.getEmail());
    }

    @Test
    void testFindByEmail_NotFound() {
        // When
        Student found = studentRepository.findByEmail("nonexistent@esprit.tn");

        // Then
        assertNull(found);
    }

    @Test
    void testFindByFirstNameContainingIgnoreCase() {
        // Given
        Student student1 = Student.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john1@esprit.tn")
                .build();

        Student student2 = Student.builder()
                .firstName("Johnny")
                .lastName("Smith")
                .email("johnny@esprit.tn")
                .build();

        Student student3 = Student.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice@esprit.tn")
                .build();

        entityManager.persist(student1);
        entityManager.persist(student2);
        entityManager.persist(student3);
        entityManager.flush();

        // When
        List<Student> johns = studentRepository.findByFirstNameContainingIgnoreCase("john");

        // Then
        assertEquals(2, johns.size());
        assertTrue(johns.stream().anyMatch(s -> s.getFirstName().equals("John")));
        assertTrue(johns.stream().anyMatch(s -> s.getFirstName().equals("Johnny")));
    }

    @Test
    void testFindByDepartment_IdDepartment() {
        // Given
        Department dept1 = Department.builder()
                .name("Computer Science")
                .build();

        Department dept2 = Department.builder()
                .name("Mathematics")
                .build();

        entityManager.persist(dept1);
        entityManager.persist(dept2);

        Student student1 = Student.builder()
                .firstName("Student1")
                .lastName("Dept1")
                .email("student1@esprit.tn")
                .department(dept1)
                .build();

        Student student2 = Student.builder()
                .firstName("Student2")
                .lastName("Dept1")
                .email("student2@esprit.tn")
                .department(dept1)
                .build();

        Student student3 = Student.builder()
                .firstName("Student3")
                .lastName("Dept2")
                .email("student3@esprit.tn")
                .department(dept2)
                .build();

        entityManager.persist(student1);
        entityManager.persist(student2);
        entityManager.persist(student3);
        entityManager.flush();

        // When
        List<Student> dept1Students = studentRepository.findByDepartment_IdDepartment(dept1.getIdDepartment());

        // Then
        assertEquals(2, dept1Students.size());
        assertTrue(dept1Students.stream().allMatch(s ->
                s.getDepartment().getIdDepartment().equals(dept1.getIdDepartment())));
    }

    @Test
    void testSaveStudent() {
        // Given
        Student student = Student.builder()
                .firstName("New")
                .lastName("Student")
                .email("new.student@esprit.tn")
                .phone("555-123-4567")
                .dateOfBirth(LocalDate.of(2001, 1, 1))
                .address("New Address")
                .build();

        // When
        Student savedStudent = studentRepository.save(student);

        // Then
        assertNotNull(savedStudent);
        assertNotNull(savedStudent.getIdStudent()); // ID généré
        assertEquals("New", savedStudent.getFirstName());
        assertEquals("new.student@esprit.tn", savedStudent.getEmail());

        // Vérification en base de données
        Student retrieved = entityManager.find(Student.class, savedStudent.getIdStudent());
        assertNotNull(retrieved);
        assertEquals(savedStudent.getEmail(), retrieved.getEmail());
    }

    @Test
    void testUpdateStudent() {
        // Given
        Student student = Student.builder()
                .firstName("Original")
                .lastName("Name")
                .email("original@esprit.tn")
                .build();

        entityManager.persist(student);
        entityManager.flush();

        // When : Mise à jour
        student.setFirstName("Updated");
        student.setEmail("updated@esprit.tn");
        Student updatedStudent = studentRepository.save(student);

        // Then
        assertEquals("Updated", updatedStudent.getFirstName());
        assertEquals("updated@esprit.tn", updatedStudent.getEmail());

        // Vérification en base
        Student retrieved = entityManager.find(Student.class, student.getIdStudent());
        assertEquals("Updated", retrieved.getFirstName());
    }

    @Test
    void testDeleteStudent() {
        // Given
        Student student = Student.builder()
                .firstName("ToDelete")
                .lastName("Student")
                .email("delete@esprit.tn")
                .build();

        entityManager.persist(student);
        entityManager.flush();
        Long studentId = student.getIdStudent();

        // When
        studentRepository.deleteById(studentId);
        entityManager.flush();

        // Then
        Student deleted = entityManager.find(Student.class, studentId);
        assertNull(deleted);
    }

    @Test
    void testFindAllStudents() {
        // Given
        Student student1 = Student.builder()
                .firstName("Student1")
                .lastName("One")
                .email("one@esprit.tn")
                .build();

        Student student2 = Student.builder()
                .firstName("Student2")
                .lastName("Two")
                .email("two@esprit.tn")
                .build();

        entityManager.persist(student1);
        entityManager.persist(student2);
        entityManager.flush();

        // When
        List<Student> allStudents = studentRepository.findAll();

        // Then
        assertTrue(allStudents.size() >= 2);
        assertTrue(allStudents.stream()
                .anyMatch(s -> s.getEmail().equals("one@esprit.tn")));
        assertTrue(allStudents.stream()
                .anyMatch(s -> s.getEmail().equals("two@esprit.tn")));
    }

    @Test
    void testFindById() {
        // Given
        Student student = Student.builder()
                .firstName("Find")
                .lastName("ById")
                .email("findbyid@esprit.tn")
                .build();

        entityManager.persist(student);
        entityManager.flush();

        // When
        Optional<Student> found = studentRepository.findById(student.getIdStudent());

        // Then
        assertTrue(found.isPresent());
        assertEquals("Find", found.get().getFirstName());
        assertEquals("findbyid@esprit.tn", found.get().getEmail());
    }
}