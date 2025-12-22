package com.veely.account.service;

import com.veely.account.domain.CounterpartyKind;
import com.veely.account.repository.CounterpartyKindRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LookupService {

    private final CounterpartyKindRepository counterpartyKindRepository;

    public List<CounterpartyKind> listCounterpartyKinds() {
        return counterpartyKindRepository.findAll(Sort.by(Sort.Direction.ASC, "description"));
    }
}
