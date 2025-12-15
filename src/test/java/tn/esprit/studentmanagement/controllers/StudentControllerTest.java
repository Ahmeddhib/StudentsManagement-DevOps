package tn.esprit.studentmanagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.studentmanagement.entities.Student;
import tn.esprit.studentmanagement.services.StudentService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Solution 1: Avec @Mock (recommandé pour Spring Boot 3.4.0+)
@ExtendWith(MockitoExtension.class)
@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock  // Utilise @Mock au lieu de @MockBean
    private StudentService studentService;  // Mock de l'implémentation, pas de l'interface

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
        mockMvc.perform(get("/students/getAllStudents")
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
        mockMvc.perform(get("/students/getStudent/{id}", studentId))
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

        // When & Then
        mockMvc.perform(get("/students/getStudent/{id}", studentId))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
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
        mockMvc.perform(post("/students/createStudent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idStudent", is(1)))
                .andExpect(jsonPath("$.firstName", is("New")));
    }

    @Test
    void testUpdateStudent() throws Exception {
        // Given
        Student updatedStudent = Student.builder()
                .idStudent(1L)
                .firstName("Updated")
                .lastName("Student")
                .email("updated.student@esprit.tn")
                .build();

        when(studentService.saveStudent(any(Student.class))).thenReturn(updatedStudent);

        // When & Then
        mockMvc.perform(put("/students/updateStudent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")));
    }

    @Test
    void testDeleteStudent() throws Exception {
        // Given
        Long studentId = 1L;
        doNothing().when(studentService).deleteStudent(studentId);

        // When & Then
        mockMvc.perform(delete("/students/deleteStudent/{id}", studentId))
                .andExpect(status().isOk());

        verify(studentService, times(1)).deleteStudent(studentId);
    }

    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(get("/students/getAllStudents")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}