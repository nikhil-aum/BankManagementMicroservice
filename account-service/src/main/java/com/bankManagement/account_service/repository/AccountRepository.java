package com.bankManagement.account_service.repository;


import com.bankManagement.account_service.entity.Account;
import com.bankManagement.account_service.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    boolean existsByCustomerIdAndAccountType(Long customerId, AccountType type);
}
