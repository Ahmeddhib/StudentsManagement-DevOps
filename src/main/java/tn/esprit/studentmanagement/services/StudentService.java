package tn.esprit.studentmanagement.services;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.studentmanagement.entities.Student;
import tn.esprit.studentmanagement.repositories.StudentRepository;

import java.util.List;

@Service
@AllArgsConstructor // Ajouter cette annotation pour l'injection via constructeur
public class StudentService implements IStudentService {
    
    // Utiliser l'injection via constructeur plutôt que @Autowired
    private final StudentRepository studentRepository;
    
    @Override // Ajouter @Override pour clarifier
    public List<Student> getAllStudents() { 
        return studentRepository.findAll(); 
    }
    
    @Override
    public Student getStudentById(Long id) { 
        return studentRepository.findById(id).orElse(null); 
    }
    
    @Override
    public Student saveStudent(Student student) { 
        return studentRepository.save(student); 
    }
    
    @Override
    public void deleteStudent(Long id) { 
        studentRepository.deleteById(id); 
    }
}
