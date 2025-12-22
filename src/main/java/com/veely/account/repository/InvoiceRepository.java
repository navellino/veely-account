package com.veely.account.repository;

import com.veely.account.domain.Invoice;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
	 @EntityGraph(attributePaths = {"direction", "status", "counterparty", "lines", "lines.vatCode", "lines.account", "lines.withholdingType"})
	    Optional<Invoice> findWithLinesAndLookupsById(Long id);
}
