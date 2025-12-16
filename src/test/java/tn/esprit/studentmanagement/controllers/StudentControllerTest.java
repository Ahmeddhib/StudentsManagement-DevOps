package tn.esprit.studentmanagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;  // <-- Changement ici
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.studentmanagement.entities.Student;
import tn.esprit.studentmanagement.services.IStudentService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean  // <-- Changement de @Mock à @MockBean
    private IStudentService studentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllStudents() throws Exception {
        // Préparation des données
        Student student1 = Student.builder()
                .idStudent(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@esprit.tn")
                .phone("1234567890")
                .dateOfBirth(LocalDate.of(2000, 5, 15))
                .address("123 Main Street")
                .build();

        Student student2 = Student.builder()
                .idStudent(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@esprit.tn")
                .phone("9876543210")
                .dateOfBirth(LocalDate.of(2001, 6, 20))
                .address("456 Oak Street")
                .build();

        List<Student> students = Arrays.asList(student1, student2);

        // Configuration du mock
        when(studentService.getAllStudents()).thenReturn(students);

        // Exécution et vérification
        mockMvc.perform(get("/api/students") // Mettre à jour l'URL
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idStudent", is(1)))
                .andExpect(jsonPath("$[0].firstName", is("John")))
                .andExpect(jsonPath("$[1].firstName", is("Jane")));

        // Vérification des interactions
        verify(studentService, times(1)).getAllStudents();
    }

    @Test
    void testGetStudentById() throws Exception {
        // Given
        Long studentId = 1L;
        Student student = Student.builder()
                .idStudent(studentId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@esprit.tn")
                .build();

        when(studentService.getStudentById(studentId)).thenReturn(student);

        // When & Then
        mockMvc.perform(get("/api/students/{id}", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idStudent", is(studentId.intValue())))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.email", is("john.doe@esprit.tn")));
    }

    @Test
    void testGetStudentByIdNotFound() throws Exception {
        // Given
        Long studentId = 999L;
        when(studentService.getStudentById(studentId)).thenReturn(null);

        // When & Then - Maintenant le contrôleur lance une exception
        mockMvc.perform(get("/api/students/{id}", studentId))  // <-- Correction URL
                .andExpect(status().isNotFound()); // Attendre 404
    }

    @Test
    void testCreateStudent() throws Exception {
        // Given
        Student newStudent = Student.builder()
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

        when(studentService.saveStudent(any(Student.class))).thenReturn(savedStudent);

        // When & Then
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idStudent", is(1)))
                .andExpect(jsonPath("$.firstName", is("New")));
    }

    @Test
    void testUpdateStudent() throws Exception {
        // Given
        Long studentId = 1L;
        Student updatedStudent = Student.builder()
                .idStudent(studentId)
                .firstName("Updated")
                .lastName("Student")
                .email("updated.student@esprit.tn")
                .build();

        // Simuler que l'étudiant existe
        when(studentService.getStudentById(studentId)).thenReturn(updatedStudent);
        when(studentService.saveStudent(any(Student.class))).thenReturn(updatedStudent);

        // When & Then
        mockMvc.perform(put("/api/students/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")));
    }

    @Test
    void testDeleteStudent() throws Exception {
        // Given
        Long studentId = 1L;
        Student existingStudent = Student.builder()
                .idStudent(studentId)
                .firstName("John")
                .lastName("Doe")
                .build();
        
        when(studentService.getStudentById(studentId)).thenReturn(existingStudent);
        doNothing().when(studentService).deleteStudent(studentId);

        // When & Then
        mockMvc.perform(delete("/api/students/{id}", studentId))
                .andExpect(status().isOk());

        verify(studentService, times(1)).deleteStudent(studentId);
    }

    @Test
    void testCorsHeaders() throws Exception {
        // Créer un étudiant de test pour que la requête réussisse
        Student student = Student.builder()
                .idStudent(1L)
                .firstName("John")
                .lastName("Doe")
                .build();
        
        when(studentService.getAllStudents()).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students")  // <-- Correction URL
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
