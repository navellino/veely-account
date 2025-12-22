package com.veely.account.web;

import com.veely.account.domain.Invoice;
import com.veely.account.domain.InvoiceLine;
import com.veely.account.service.InvoiceService;
import com.veely.account.service.LookupService;
import com.veely.account.service.dto.InvoiceTotals;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final LookupService lookupService;

    @GetMapping
    public String list(@RequestParam(name = "direction", required = false) String direction,
                       @RequestParam(name = "status", required = false) Long statusId,
                       @RequestParam(name = "counterpartyId", required = false) Long counterpartyId,
                       @RequestParam(name = "from", required = false) LocalDate from,
                       @RequestParam(name = "to", required = false) LocalDate to,
                       @RequestParam(name = "q", required = false) String q,
                       Model model) {
        List<Invoice> invoices = invoiceService.search(direction, statusId, counterpartyId, from, to, q);
        Map<Long, InvoiceTotals> totals = invoiceService.calculateTotals(invoices);

        populateLookups(model);
        model.addAttribute("invoices", invoices);
        model.addAttribute("totals", totals);
        model.addAttribute("direction", direction);
        model.addAttribute("status", statusId);
        model.addAttribute("counterpartyId", counterpartyId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("q", q);
        model.addAttribute("pageTitle", "Fatture");
        return "invoices/list";
    }

    @GetMapping("/new")
    public String newInvoice(Model model) {
        model.addAttribute("invoice", new Invoice());
        populateLookups(model);
        model.addAttribute("pageTitle", "Nuova fattura");
        return "invoices/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("invoice") Invoice invoice,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateLookups(model);
            model.addAttribute("pageTitle", "Nuova fattura");
            return "invoices/form";
        }

        Invoice saved = invoiceService.create(invoice);
        redirectAttributes.addFlashAttribute("successMessage", "Fattura creata con successo");
        return "redirect:/invoices/" + saved.getId();
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.getOrThrow(id);
            model.addAttribute("invoice", invoice);
            populateLookups(model);
            model.addAttribute("pageTitle", "Modifica fattura");
            return "invoices/form";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fattura non trovata");
            return "redirect:/invoices";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("invoice") Invoice invoice,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateLookups(model);
            model.addAttribute("pageTitle", "Modifica fattura");
            return "invoices/form";
        }

        try {
            invoiceService.updateHeader(id, invoice);
            redirectAttributes.addFlashAttribute("successMessage", "Fattura aggiornata con successo");
            return "redirect:/invoices/" + id;
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fattura non trovata");
            return "redirect:/invoices";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.getOrThrow(id);
            InvoiceTotals totals = invoiceService.calculateTotals(invoice);
            model.addAttribute("invoice", invoice);
            model.addAttribute("totals", totals);
            model.addAttribute("line", new InvoiceLine());
            populateLookups(model);
            model.addAttribute("pageTitle", "Dettaglio fattura");
            return "invoices/detail";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fattura non trovata");
            return "redirect:/invoices";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Fattura eliminata con successo");
        } catch (InvoiceService.InvoiceDeletionException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fattura non trovata");
        }
        return "redirect:/invoices";
    }

    @PostMapping("/{id}/lines")
    public String addLine(@PathVariable Long id,
                          @Valid @ModelAttribute("line") InvoiceLine line,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            try {
                Invoice invoice = invoiceService.getOrThrow(id);
                model.addAttribute("invoice", invoice);
                model.addAttribute("totals", invoiceService.calculateTotals(invoice));
                populateLookups(model);
                model.addAttribute("pageTitle", "Dettaglio fattura");
                return "invoices/detail";
            } catch (EntityNotFoundException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", "Fattura non trovata");
                return "redirect:/invoices";
            }
        }

        try {
            invoiceService.addLine(id, line);
            redirectAttributes.addFlashAttribute("successMessage", "Riga aggiunta con successo");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/invoices";
        }
        return "redirect:/invoices/" + id;
    }

    @GetMapping("/{id}/lines/{lineId}/edit")
    public String editLine(@PathVariable Long id,
                           @PathVariable Long lineId,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.getOrThrow(id);
            Optional<InvoiceLine> line = invoice.getLines().stream()
                    .filter(l -> lineId.equals(l.getId()))
                    .findFirst();
            if (line.isEmpty()) {
                throw new EntityNotFoundException("Riga non trovata: " + lineId);
            }
            model.addAttribute("invoice", invoice);
            model.addAttribute("line", line.get());
            populateLookups(model);
            model.addAttribute("pageTitle", "Modifica riga");
            return "invoices/line-form";
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/invoices";
        }
    }

    @PostMapping("/{id}/lines/{lineId}")
    public String updateLine(@PathVariable Long id,
                             @PathVariable Long lineId,
                             @Valid @ModelAttribute("line") InvoiceLine line,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            try {
                Invoice invoice = invoiceService.getOrThrow(id);
                model.addAttribute("invoice", invoice);
                populateLookups(model);
                model.addAttribute("pageTitle", "Modifica riga");
                return "invoices/line-form";
            } catch (EntityNotFoundException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/invoices";
            }
        }

        try {
            invoiceService.updateLine(id, lineId, line);
            redirectAttributes.addFlashAttribute("successMessage", "Riga aggiornata con successo");
            return "redirect:/invoices/" + id;
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/invoices";
        }
    }

    @PostMapping("/{id}/lines/{lineId}/delete")
    public String deleteLine(@PathVariable Long id, @PathVariable Long lineId, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.deleteLine(id, lineId);
            redirectAttributes.addFlashAttribute("successMessage", "Riga eliminata con successo");
            return "redirect:/invoices/" + id;
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/invoices";
        }
    }

    private void populateLookups(Model model) {
        model.addAttribute("directions", lookupService.listInvoiceDirections());
        model.addAttribute("statuses", lookupService.listInvoiceStatuses());
        model.addAttribute("counterparties", lookupService.listCounterparties());
        model.addAttribute("vatCodes", lookupService.listVatCodes());
        model.addAttribute("accounts", lookupService.listAccounts());
        model.addAttribute("withholdingTypes", lookupService.listWithholdingTypes());
    }
}
