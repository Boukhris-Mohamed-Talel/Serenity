package com.example.healthcare.repository;

import com.example.healthcare.entity.Role;
import com.example.healthcare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    List<User> findByRole(Role role);

    List<User> findByRoleAndIsActiveTrueOrderByLastNameAscFirstNameAsc(Role role);
}
