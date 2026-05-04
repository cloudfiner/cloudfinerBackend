package com.aws.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aws.entity.Role;
import com.aws.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

	
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    
    Optional<Role> findByName(String name);

	List<User> findByRoles_NameAndEnabled(String formattedRole, boolean isActive);

	List<User> findByRoles_Name(String formattedRole);

	List<User> findByEnabled(boolean isActive);
}