package com.serenity.monitoring.repository;

import com.serenity.monitoring.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    List<UserAccount> findAllByRoleAndIsActiveTrueOrderByIdAsc(String role);
}

