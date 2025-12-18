package com.veely.account.repository;

import com.veely.account.domain.CounterpartyKind;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterpartyKindRepository extends JpaRepository<CounterpartyKind, Long> {
}