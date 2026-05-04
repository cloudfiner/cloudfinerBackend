package com.aws.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aws.entity.UserBudget;
import java.util.Optional;
import java.util.UUID;

public interface UserBudgetRepository extends JpaRepository<UserBudget, UUID> {

	Optional<UserBudget> findByEmail(String email);
}
