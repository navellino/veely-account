package com.veely.account.service;

import com.veely.account.domain.Account;
import com.veely.account.domain.Counterparty;
import com.veely.account.domain.Invoice;
import com.veely.account.domain.InvoiceDirection;
import com.veely.account.domain.InvoiceLine;
import com.veely.account.domain.InvoiceStatus;
import com.veely.account.domain.VatCode;
import com.veely.account.domain.WithholdingType;
import com.veely.account.repository.AccountRepository;
import com.veely.account.repository.CounterpartyRepository;
import com.veely.account.repository.InvoiceDirectionRepository;
import com.veely.account.repository.InvoiceLineRepository;
import com.veely.account.repository.InvoiceRepository;
import com.veely.account.repository.InvoiceStatusRepository;
import com.veely.account.repository.VatCodeRepository;
import com.veely.account.repository.WithholdingTypeRepository;
import com.veely.account.service.dto.InvoiceTotals;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceDirectionRepository invoiceDirectionRepository;
    private final InvoiceStatusRepository invoiceStatusRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final VatCodeRepository vatCodeRepository;
    private final AccountRepository accountRepository;
    private final WithholdingTypeRepository withholdingTypeRepository;

    @Transactional(readOnly = true)
    public List<Invoice> search(String directionCode, Long statusId, Long counterpartyId, LocalDate from, LocalDate to, String q) {
        Specification<Invoice> spec = Specification.where(null);

        if (StringUtils.hasText(directionCode)) {
            String normalized = directionCode.trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.join("direction").get("code")), normalized));
        }

        if (statusId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("status").get("id"), statusId));
        }

        if (counterpartyId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("counterparty").get("id"), counterpartyId));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("issueDate"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("issueDate"), to));
        }

        if (StringUtils.hasText(q)) {
            String trimmed = q.trim();
            spec = spec.and((root, query, cb) -> {
                var predicates = cb.disjunction();
                predicates.getExpressions().add(cb.like(cb.lower(root.get("number")), "%" + trimmed.toLowerCase() + "%"));
                if (trimmed.chars().allMatch(Character::isDigit)) {
                    predicates.getExpressions().add(cb.equal(root.get("year"), Integer.valueOf(trimmed)));
                }
                return predicates;
            });
        }

        return invoiceRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "issueDate"));
    }

    @Transactional(readOnly = true)
    public Invoice getOrThrow(Long id) {
        return invoiceRepository.findWithLinesAndLookupsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fattura non trovata: " + id));
    }

    @Transactional
    public Invoice create(Invoice dto) {
        Invoice invoice = new Invoice();
        applyHeaderData(invoice, dto);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice updateHeader(Long id, Invoice dto) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fattura non trovata: " + id));
        applyHeaderData(invoice, dto);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void delete(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fattura non trovata: " + id));
        try {
            invoiceRepository.delete(invoice);
            invoiceRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new InvoiceDeletionException("Non puoi eliminare: ci sono dati collegati", ex);
        }
    }

    @Transactional
    public InvoiceLine addLine(Long invoiceId, InvoiceLine dto) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Fattura non trovata: " + invoiceId));

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        applyLineData(line, dto);

        return invoiceLineRepository.save(line);
    }

    @Transactional
    public InvoiceLine updateLine(Long invoiceId, Long lineId, InvoiceLine dto) {
        InvoiceLine line = invoiceLineRepository.findById(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata: " + lineId));

        if (!Objects.equals(line.getInvoice().getId(), invoiceId)) {
            throw new EntityNotFoundException("Riga non trovata per questa fattura");
        }

        applyLineData(line, dto);
        return invoiceLineRepository.save(line);
    }

    @Transactional
    public void deleteLine(Long invoiceId, Long lineId) {
        InvoiceLine line = invoiceLineRepository.findById(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata: " + lineId));

        if (!Objects.equals(line.getInvoice().getId(), invoiceId)) {
            throw new EntityNotFoundException("Riga non trovata per questa fattura");
        }

        invoiceLineRepository.delete(line);
    }

    @Transactional(readOnly = true)
    public Map<Long, InvoiceTotals> calculateTotals(List<Invoice> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            return Map.of();
        }
        Set<Long> invoiceIds = invoices.stream()
                .map(Invoice::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceIdsWithLookups(invoiceIds);
        Map<Long, List<InvoiceLine>> linesByInvoice = lines.stream()
                .collect(Collectors.groupingBy(line -> line.getInvoice().getId()));

        Map<Long, InvoiceTotals> totals = new HashMap<>();
        for (Invoice invoice : invoices) {
            List<InvoiceLine> invoiceLines = linesByInvoice.getOrDefault(invoice.getId(), List.of());
            totals.put(invoice.getId(), computeTotals(invoice, invoiceLines));
        }
        return totals;
    }

    @Transactional(readOnly = true)
    public InvoiceTotals calculateTotals(Invoice invoice) {
        return computeTotals(invoice, invoice.getLines());
    }

    private void applyHeaderData(Invoice invoice, Invoice dto) {
        invoice.setDirection(getDirection(dto.getDirection()));
        invoice.setStatus(getStatus(dto.getStatus()));
        invoice.setCounterparty(getCounterparty(dto.getCounterparty()));
        invoice.setNumber(dto.getNumber());
        invoice.setYear(dto.getYear());
        invoice.setIssueDate(dto.getIssueDate());
        invoice.setDueDate(dto.getDueDate());
        invoice.setNotes(dto.getNotes());
    }

    private InvoiceDirection getDirection(InvoiceDirection direction) {
        Long directionId = Optional.ofNullable(direction).map(InvoiceDirection::getId)
                .orElseThrow(() -> new EntityNotFoundException("Direzione obbligatoria"));
        return invoiceDirectionRepository.findById(directionId)
                .orElseThrow(() -> new EntityNotFoundException("Direzione non trovata: " + directionId));
    }

    private InvoiceStatus getStatus(InvoiceStatus status) {
        Long statusId = Optional.ofNullable(status).map(InvoiceStatus::getId)
                .orElseThrow(() -> new EntityNotFoundException("Stato obbligatorio"));
        return invoiceStatusRepository.findById(statusId)
                .orElseThrow(() -> new EntityNotFoundException("Stato non trovato: " + statusId));
    }

    private Counterparty getCounterparty(Counterparty counterparty) {
        Long counterpartyId = Optional.ofNullable(counterparty).map(Counterparty::getId)
                .orElseThrow(() -> new EntityNotFoundException("Controparte obbligatoria"));
        return counterpartyRepository.findById(counterpartyId)
                .orElseThrow(() -> new EntityNotFoundException("Controparte non trovata: " + counterpartyId));
    }

    private VatCode getVatCode(VatCode vatCode) {
        if (vatCode == null || vatCode.getId() == null) {
            return null;
        }
        return vatCodeRepository.findById(vatCode.getId())
                .orElseThrow(() -> new EntityNotFoundException("Codice IVA non trovato: " + vatCode.getId()));
    }

    private Account getAccount(Account account) {
        if (account == null || account.getId() == null) {
            return null;
        }
        return accountRepository.findById(account.getId())
                .orElseThrow(() -> new EntityNotFoundException("Conto non trovato: " + account.getId()));
    }

    private WithholdingType getWithholdingType(WithholdingType withholdingType) {
        if (withholdingType == null || withholdingType.getId() == null) {
            return null;
        }
        return withholdingTypeRepository.findById(withholdingType.getId())
                .orElseThrow(() -> new EntityNotFoundException("Tipologia ritenuta non trovata: " + withholdingType.getId()));
    }

    private void applyLineData(InvoiceLine target, InvoiceLine dto) {
        target.setDescription(dto.getDescription());
        target.setNetAmount(dto.getNetAmount());
        target.setVatCode(getVatCode(dto.getVatCode()));
        target.setAccount(getAccount(dto.getAccount()));
        target.setWithholdingType(getWithholdingType(dto.getWithholdingType()));
    }

    private InvoiceTotals computeTotals(Invoice invoice, Collection<InvoiceLine> lines) {
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        BigDecimal withholdingTotal = BigDecimal.ZERO;

        for (InvoiceLine line : lines) {
            BigDecimal lineNet = line.getNetAmount() != null ? line.getNetAmount() : BigDecimal.ZERO;
            netTotal = netTotal.add(lineNet);

            BigDecimal vatRate = Optional.ofNullable(line.getVatCode())
                    .map(VatCode::getRate)
                    .orElse(BigDecimal.ZERO);
            if (vatRate != null) {
                vatTotal = vatTotal.add(lineNet.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }

            WithholdingType wt = line.getWithholdingType();
            if (wt != null && wt.getRate() != null) {
                BigDecimal taxablePercent = Optional.ofNullable(wt.getTaxablePercent()).orElse(BigDecimal.valueOf(100));
                BigDecimal withholdingBase = lineNet.multiply(taxablePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                withholdingTotal = withholdingTotal.add(withholdingBase.multiply(wt.getRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        }

        BigDecimal grossTotal = netTotal.add(vatTotal);
        boolean isPassive = Optional.ofNullable(invoice.getDirection())
                .map(InvoiceDirection::getCode)
                .map(String::toUpperCase)
                .map("PASSIVE"::equals)
                .orElse(false);
        BigDecimal payableTotal = isPassive ? grossTotal.subtract(withholdingTotal) : grossTotal;

        return InvoiceTotals.builder()
                .netTotal(netTotal)
                .vatTotal(vatTotal)
                .grossTotal(grossTotal)
                .withholdingTotal(withholdingTotal)
                .payableTotal(payableTotal)
                .build();
    }

    public static class InvoiceDeletionException extends RuntimeException {
        private static final long serialVersionUID = 536026287147341070L;

        public InvoiceDeletionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}