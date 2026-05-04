package com.aws.repository;
import com.aws.entity.AlertExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertExecutionRepository extends JpaRepository<AlertExecution, UUID> {

    //  MAIN (used in rule engine)
    Optional<AlertExecution> findByRuleIdAndUserId(UUID uuid2, UUID uuid);

    //  Dashboard use
    List<AlertExecution> findByUserId(UUID userId);

    //  Active alerts
    List<AlertExecution> findByLastStateTrue();

    //  Analytics
    List<AlertExecution> findByRuleId(UUID ruleId);
    
    
    Optional<AlertExecution> findByUserIdAndRuleIdIsNull(UUID userId);
    
   
}