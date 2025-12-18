package com.veely.account.repository;

import com.veely.account.domain.VatCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VatCodeRepository extends JpaRepository<VatCode, Long> {

    Optional<VatCode> findByCode(String code);
}
