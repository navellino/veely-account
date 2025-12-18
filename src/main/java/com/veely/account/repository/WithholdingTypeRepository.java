package com.veely.account.repository;

import com.veely.account.domain.WithholdingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WithholdingTypeRepository extends JpaRepository<WithholdingType, Long> {

    Optional<WithholdingType> findByCode(String code);
}