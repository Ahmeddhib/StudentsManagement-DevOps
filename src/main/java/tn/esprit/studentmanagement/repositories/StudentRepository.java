package tn.esprit.studentmanagement.repositories;

import tn.esprit.studentmanagement.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Recherche par email (exact match)
    Student findByEmail(String email);

    // Recherche par prénom (contient, insensible à la casse)
    List<Student> findByFirstNameContainingIgnoreCase(String firstName);

    // Recherche par nom de famille
    List<Student> findByLastName(String lastName);

    // Recherche par département
    List<Student> findByDepartment_IdDepartment(Long departmentId);

    // Recherche par email contenant un mot
    List<Student> findByEmailContaining(String domain);

    // Compte le nombre d'étudiants par département
    Long countByDepartment_IdDepartment(Long departmentId);

    // Recherche étudiants nés après une certaine date
    List<Student> findByDateOfBirthAfter(LocalDate date);
}