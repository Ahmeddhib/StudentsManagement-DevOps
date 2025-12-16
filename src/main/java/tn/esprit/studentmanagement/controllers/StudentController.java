package tn.esprit.studentmanagement.controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.studentmanagement.entities.Student;
import tn.esprit.studentmanagement.services.IStudentService;
import org.springframework.http.ResponseEntity;

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
public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student student) {
    Student existingStudent = studentService.getStudentById(id);
    if (existingStudent == null) {
        return ResponseEntity.notFound().build(); // HTTP 404
    }
    student.setIdStudent(id);
    return ResponseEntity.ok(studentService.saveStudent(student)); // HTTP 200
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteStudent(@PathVariable Long id) { 
    Student existingStudent = studentService.getStudentById(id);
    if (existingStudent == null) {
        return ResponseEntity.notFound().build(); // HTTP 404
    }
    studentService.deleteStudent(id);
    return ResponseEntity.ok().build(); // HTTP 200
}
}
