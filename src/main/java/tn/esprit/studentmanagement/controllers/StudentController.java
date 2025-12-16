package tn.esprit.studentmanagement.controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.studentmanagement.entities.Student;
import tn.esprit.studentmanagement.services.IStudentService;

import java.util.List;

@RestController
@RequestMapping("/api/students") // Ajouter /api pour meilleure convention
@CrossOrigin(origins = "http://localhost:4200")
@AllArgsConstructor
public class StudentController {
    
    private final IStudentService studentService; // Utiliser l'interface, pas l'implémentation

    @GetMapping
    public List<Student> getAllStudents() { 
        return studentService.getAllStudents(); 
    }

    @GetMapping("/{id}")
    public Student getStudent(@PathVariable Long id) { 
        Student student = studentService.getStudentById(id);
        if (student == null) {
            return ResponseEntity.notFound().build(); // Returns HTTP 404
        }
        return student; 
    }

    @PostMapping
    public Student createStudent(@RequestBody Student student) { 
        return studentService.saveStudent(student); 
    }

    @PutMapping("/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student student) {
        // Vérifier si l'étudiant existe
        Student existingStudent = studentService.getStudentById(id);
        if (existingStudent == null) {
            return ResponseEntity.notFound().build(); // Returns HTTP 404
        }
        student.setIdStudent(id); // S'assurer que l'ID est correct
        return studentService.saveStudent(student);
    }

    @DeleteMapping("/{id}")
    public void deleteStudent(@PathVariable Long id) { 
        // Vérifier si l'étudiant existe
        Student existingStudent = studentService.getStudentById(id);
        if (existingStudent == null) {
           return ResponseEntity.notFound().build(); // Returns HTTP 404
        }
        studentService.deleteStudent(id); 
    }
}
