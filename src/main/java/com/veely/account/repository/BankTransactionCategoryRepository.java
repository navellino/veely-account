package com.veely.account.repository;

import com.veely.account.domain.BankTransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionCategoryRepository extends JpaRepository<BankTransactionCategory, Long> {
}
