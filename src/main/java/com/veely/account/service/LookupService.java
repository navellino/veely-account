package com.veely.account.service;

import com.veely.account.domain.Account;
import com.veely.account.domain.Counterparty;
import com.veely.account.domain.CounterpartyKind;
import com.veely.account.domain.InvoiceDirection;
import com.veely.account.domain.InvoiceStatus;
import com.veely.account.domain.VatCode;
import com.veely.account.domain.WithholdingType;
import com.veely.account.repository.AccountRepository;
import com.veely.account.repository.CounterpartyKindRepository;
import com.veely.account.repository.CounterpartyRepository;
import com.veely.account.repository.InvoiceDirectionRepository;
import com.veely.account.repository.InvoiceStatusRepository;
import com.veely.account.repository.VatCodeRepository;
import com.veely.account.repository.WithholdingTypeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LookupService {
	private final AccountRepository accountRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final CounterpartyKindRepository counterpartyKindRepository;
    private final InvoiceDirectionRepository invoiceDirectionRepository;
    private final InvoiceStatusRepository invoiceStatusRepository;
    private final VatCodeRepository vatCodeRepository;
    private final WithholdingTypeRepository withholdingTypeRepository;
    
    

    public List<CounterpartyKind> listCounterpartyKinds() {
        return counterpartyKindRepository.findAll(Sort.by(Sort.Direction.ASC, "description"));
    }
    
    public List<Counterparty> listCounterparties() {
        return counterpartyRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<InvoiceDirection> listInvoiceDirections() {
        return invoiceDirectionRepository.findAll(Sort.by(Sort.Direction.ASC, "description"));
    }

    public List<InvoiceStatus> listInvoiceStatuses() {
        return invoiceStatusRepository.findAll(Sort.by(Sort.Direction.ASC, "description"));
    }

    public List<VatCode> listVatCodes() {
        return vatCodeRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
    }

    public List<Account> listAccounts() {
        return accountRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
    }

    public List<WithholdingType> listWithholdingTypes() {
        return withholdingTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
    }
}
