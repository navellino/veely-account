package com.veely.account.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vat_codes")
public class VatCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "registry_description", length = 255)
    private String registryDescription;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    @Column(name = "operation_type", length = 100)
    private String operationType;

    @Column(length = 100)
    private String category;

    @Column(name = "use_purchases", nullable = false)
    private Boolean usePurchases = Boolean.TRUE;

    @Column(name = "use_sales", nullable = false)
    private Boolean useSales = Boolean.TRUE;

    @Column(name = "use_receipts", nullable = false)
    private Boolean useReceipts = Boolean.TRUE;

    @Column(name = "custom_nature_purchases", length = 50)
    private String customNaturePurchases;

    @Column(name = "custom_nature_sales", length = 50)
    private String customNatureSales;

    @Column(name = "reverse_charge_relevant", nullable = false)
    private Boolean reverseChargeRelevant = Boolean.FALSE;

    @Column(length = 50)
    private String validity;

    @Column(name = "vat_edf_code", length = 50)
    private String vatEdfCode;

    @Column(name = "vat_grouping", length = 100)
    private String vatGrouping;

    @Column(name = "stamp_duty_applicable", nullable = false)
    private Boolean stampDutyApplicable = Boolean.FALSE;

    @Column(name = "agri_comp_rate", precision = 5, scale = 2)
    private BigDecimal agriCompRate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "external_code", length = 50)
    private String externalCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        VatCode vatCode = (VatCode) o;
        return id != null && Objects.equals(id, vatCode.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}