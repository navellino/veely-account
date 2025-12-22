package com.veely.account.web;

import com.veely.account.domain.Counterparty;
import com.veely.account.service.CounterpartyService;
import com.veely.account.service.LookupService;
import com.veely.account.service.dto.CounterpartyDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/counterparties")
@RequiredArgsConstructor
public class CounterpartyController {

    private final CounterpartyService counterpartyService;
    private final LookupService lookupService;

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("counterparties", counterpartyService.searchByName(q));
        model.addAttribute("q", q);
        model.addAttribute("pageTitle", "Controparti");
        return "counterparties/list";
    }

    @GetMapping("/new")
    public String newCounterparty(Model model) {
        model.addAttribute("counterparty", new CounterpartyDto());
        populateLookups(model);
        model.addAttribute("pageTitle", "Nuova controparte");
        return "counterparties/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("counterparty") CounterpartyDto dto,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateLookups(model);
            model.addAttribute("pageTitle", "Nuova controparte");
            return "counterparties/form";
        }

        counterpartyService.create(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Controparte creata con successo");
        return "redirect:/counterparties";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Counterparty counterparty = counterpartyService.getOrThrow(id);
            model.addAttribute("counterparty", counterpartyService.toDto(counterparty));
            populateLookups(model);
            model.addAttribute("pageTitle", "Modifica controparte");
            return "counterparties/form";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Controparte non trovata");
            return "redirect:/counterparties";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("counterparty") CounterpartyDto dto,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateLookups(model);
            model.addAttribute("pageTitle", "Modifica controparte");
            return "counterparties/form";
        }

        try {
            counterpartyService.update(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Controparte aggiornata con successo");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Controparte non trovata");
        }
        return "redirect:/counterparties";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            counterpartyService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Controparte eliminata con successo");
        } catch (CounterpartyService.CounterpartyDeletionException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Controparte non trovata");
        }
        return "redirect:/counterparties";
    }

    private void populateLookups(Model model) {
        model.addAttribute("kinds", lookupService.listCounterpartyKinds());
    }
}