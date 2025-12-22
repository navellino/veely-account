package com.veely.account.repository;

import com.veely.account.domain.InvoiceLine;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
	
	@Query("select il from InvoiceLine il join fetch il.invoice inv left join fetch il.vatCode left join fetch il.account left join fetch il.withholdingType where inv.id = :invoiceId order by il.id")
    List<InvoiceLine> findByInvoiceIdWithLookups(@Param("invoiceId") Long invoiceId);

    @Query("select il from InvoiceLine il join fetch il.invoice inv left join fetch il.vatCode left join fetch il.account left join fetch il.withholdingType where inv.id in :invoiceIds")
    List<InvoiceLine> findByInvoiceIdsWithLookups(@Param("invoiceIds") Collection<Long> invoiceIds);
	
}
