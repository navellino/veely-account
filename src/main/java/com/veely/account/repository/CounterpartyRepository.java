package com.veely.account.repository;

import com.veely.account.domain.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {

    List<Counterparty> findByNameContainingIgnoreCase(String q);
}
