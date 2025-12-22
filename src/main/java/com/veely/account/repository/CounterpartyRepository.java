package com.veely.account.repository;

import com.veely.account.domain.Counterparty;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {

	@EntityGraph(attributePaths = "kind")
    List<Counterparty> findAll(Sort sort);

    @EntityGraph(attributePaths = "kind")
    List<Counterparty> findByNameContainingIgnoreCase(String q, Sort sort);

    @EntityGraph(attributePaths = "kind")
    Optional<Counterparty> findWithKindById(Long id);
}
