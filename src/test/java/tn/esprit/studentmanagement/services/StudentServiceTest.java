package tn.esprit.studentmanagement.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.studentmanagement.entities.Student;
import tn.esprit.studentmanagement.repositories.StudentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void testGetAllStudents() {
        // Given: Préparation des données mockées
        Student student1 = Student.builder()
                .idStudent(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@esprit.tn")
                .build();

        Student student2 = Student.builder()
                .idStudent(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@esprit.tn")
                .build();

        List<Student> students = Arrays.asList(student1, student2);

        // Configuration du mock
        when(studentRepository.findAll()).thenReturn(students);

        // When: Appel de la méthode à tester
        List<Student> result = studentService.getAllStudents();

        // Then: Vérifications
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals("Jane", result.get(1).getFirstName());

        // Vérification que la méthode du repository a été appelée
        verify(studentRepository, times(1)).findAll();
    }

    @Test
    void testGetStudentById_Found() {
        // Given
        Long studentId = 1L;
        Student student = Student.builder()
                .idStudent(studentId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@esprit.tn")
                .build();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        // When
        Student result = studentService.getStudentById(studentId);

        // Then
        assertNotNull(result);
        assertEquals(studentId, result.getIdStudent());
        assertEquals("John", result.getFirstName());
        assertEquals("john.doe@esprit.tn", result.getEmail());

        verify(studentRepository, times(1)).findById(studentId);
    }

    @Test
    void testGetStudentById_NotFound() {
        // Given
        Long studentId = 999L;
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // When
        Student result = studentService.getStudentById(studentId);

        // Then
        assertNull(result);
        verify(studentRepository, times(1)).findById(studentId);
    }

    @Test
    void testSaveStudent() {
        // Given
        Student studentToSave = Student.builder()
                .firstName("New")
                .lastName("Student")
                .email("new.student@esprit.tn")
                .build();

        Student savedStudent = Student.builder()
                .idStudent(1L)
                .firstName("New")
                .lastName("Student")
                .email("new.student@esprit.tn")
                .build();

        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

        // When
        Student result = studentService.saveStudent(studentToSave);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getIdStudent());
        assertEquals("New", result.getFirstName());
        assertEquals("new.student@esprit.tn", result.getEmail());

        verify(studentRepository, times(1)).save(studentToSave);
    }

    @Test
    void testSaveStudent_WithExistingId() {
        // Given: Sauvegarde d'un étudiant existant (mise à jour)
        Student existingStudent = Student.builder()
                .idStudent(1L)
                .firstName("Old")
                .lastName("Name")
                .email("old.email@esprit.tn")
                .build();

        Student updatedStudent = Student.builder()
                .idStudent(1L)
                .firstName("Updated")
                .lastName("Name")
                .email("updated.email@esprit.tn")
                .build();

        when(studentRepository.save(existingStudent)).thenReturn(updatedStudent);

        // When
        Student result = studentService.saveStudent(existingStudent);

        // Then
        assertEquals(1L, result.getIdStudent());
        assertEquals("Updated", result.getFirstName());
        assertEquals("updated.email@esprit.tn", result.getEmail());
    }

    @Test
    void testDeleteStudent() {
        // Given
        Long studentId = 1L;

        // When
        studentService.deleteStudent(studentId);

        // Then
        verify(studentRepository, times(1)).deleteById(studentId);
    }

    @Test
    void testDeleteStudent_VerifyInteraction() {
        // Given
        Long studentId = 1L;

        // Do nothing when deleteById is called
        doNothing().when(studentRepository).deleteById(studentId);

        // When
        studentService.deleteStudent(studentId);

        // Then
        verify(studentRepository, times(1)).deleteById(studentId);
    }

    @Test
    void testGetAllStudents_EmptyList() {
        // Given
        when(studentRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<Student> result = studentService.getAllStudents();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(studentRepository, times(1)).findAll();
    }

    @Test
    void testSaveStudent_NullStudent() {
        // Given
        when(studentRepository.save(null)).thenThrow(new IllegalArgumentException());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            studentService.saveStudent(null);
        });
    }

    @Test
    void testSaveStudent_WithMinimalData() {
        // Given
        Student minimalStudent = Student.builder()
                .firstName("Minimal")
                .lastName("Student")
                .email("minimal@esprit.tn")
                .build();

        Student savedStudent = Student.builder()
                .idStudent(1L)
                .firstName("Minimal")
                .lastName("Student")
                .email("minimal@esprit.tn")
                .build();

        when(studentRepository.save(minimalStudent)).thenReturn(savedStudent);

        // When
        Student result = studentService.saveStudent(minimalStudent);

        // Then
        assertNotNull(result.getIdStudent());
        assertEquals("Minimal", result.getFirstName());
        verify(studentRepository, times(1)).save(minimalStudent);
    }

    @Test
    void testServiceMethodsChain() {
        // Test d'une chaîne d'opérations
        // 1. Créer un étudiant
        Student newStudent = Student.builder()
                .firstName("Chain")
                .lastName("Test")
                .email("chain.test@esprit.tn")
                .build();

        Student savedStudent = Student.builder()
                .idStudent(1L)
                .firstName("Chain")
                .lastName("Test")
                .email("chain.test@esprit.tn")
                .build();

        when(studentRepository.save(newStudent)).thenReturn(savedStudent);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(savedStudent));

        // Sauvegarder
        Student created = studentService.saveStudent(newStudent);
        assertNotNull(created.getIdStudent());

        // Récupérer
        Student retrieved = studentService.getStudentById(1L);
        assertEquals("Chain", retrieved.getFirstName());

        // Supprimer
        studentService.deleteStudent(1L);

        // Vérifications
        verify(studentRepository, times(1)).save(newStudent);
        verify(studentRepository, times(1)).findById(1L);
        verify(studentRepository, times(1)).deleteById(1L);
    }
}