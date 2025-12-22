package com.veely.account.service;

import com.veely.account.domain.Counterparty;
import com.veely.account.domain.CounterpartyKind;
import com.veely.account.repository.CounterpartyKindRepository;
import com.veely.account.repository.CounterpartyRepository;
import com.veely.account.service.dto.CounterpartyDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CounterpartyService {

    private final CounterpartyRepository counterpartyRepository;
    private final CounterpartyKindRepository counterpartyKindRepository;

    @Transactional(readOnly = true)
    public List<Counterparty> listAll() {
        return counterpartyRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional(readOnly = true)
    public List<Counterparty> searchByName(String q) {
        if (!StringUtils.hasText(q)) {
            return listAll();
        }
        return counterpartyRepository.findByNameContainingIgnoreCase(q.trim(), Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional(readOnly = true)
    public Counterparty getOrThrow(Long id) {
        return counterpartyRepository.findWithKindById(id)
                .orElseThrow(() -> new EntityNotFoundException("Controparte non trovata: " + id));
    }

    @Transactional
    public Counterparty create(CounterpartyDto dto) {
        CounterpartyKind kind = getKindOrThrow(dto.getKindId());

        Counterparty counterparty = new Counterparty();
        counterparty.setKind(kind);
        applyData(counterparty, dto);

        return counterpartyRepository.save(counterparty);
    }

    @Transactional
    public Counterparty update(Long id, CounterpartyDto dto) {
        Counterparty counterparty = getOrThrow(id);
        counterparty.setKind(getKindOrThrow(dto.getKindId()));
        applyData(counterparty, dto);
        return counterpartyRepository.save(counterparty);
    }

    @Transactional
    public void delete(Long id) {
        Counterparty counterparty = getOrThrow(id);
        try {
            counterpartyRepository.delete(counterparty);
            counterpartyRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new CounterpartyDeletionException("Non puoi eliminare: ci sono fatture/movimenti collegati", ex);
        }
    }

    public CounterpartyDto toDto(Counterparty counterparty) {
        return CounterpartyDto.builder()
                .id(counterparty.getId())
                .kindId(counterparty.getKind() != null ? counterparty.getKind().getId() : null)
                .name(counterparty.getName())
                .vatNumber(counterparty.getVatNumber())
                .taxCode(counterparty.getTaxCode())
                .pec(counterparty.getPec())
                .sdiCode(counterparty.getSdiCode())
                .iban(counterparty.getIban())
                .notes(counterparty.getNotes())
                .build();
    }

    private CounterpartyKind getKindOrThrow(Long kindId) {
        return counterpartyKindRepository.findById(kindId)
                .orElseThrow(() -> new EntityNotFoundException("Tipo controparte non trovato: " + kindId));
    }

    private void applyData(Counterparty counterparty, CounterpartyDto dto) {
        counterparty.setName(dto.getName());
        counterparty.setVatNumber(dto.getVatNumber());
        counterparty.setTaxCode(dto.getTaxCode());
        counterparty.setPec(dto.getPec());
        counterparty.setSdiCode(dto.getSdiCode());
        counterparty.setIban(dto.getIban());
        counterparty.setNotes(dto.getNotes());
    }

    public static class CounterpartyDeletionException extends RuntimeException {
        /**
		 * 
		 */
		private static final long serialVersionUID = 5615509824863357990L;

		public CounterpartyDeletionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
