package com.veely.account.repository;

import com.veely.account.domain.InvoiceDirection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceDirectionRepository extends JpaRepository<InvoiceDirection, Long> {
}
