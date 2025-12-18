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
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "withholding_types")
public class WithholdingType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_percent", precision = 5, scale = 2)
    private BigDecimal taxablePercent;

    @Column(name = "tribute_code", length = 50)
    private String tributeCode;

    @Column(name = "tribute_description", length = 255)
    private String tributeDescription;

    @Column(name = "due_date_description", length = 255)
    private String dueDateDescription;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    @Column(name = "short_rent", nullable = false)
    private Boolean shortRent = Boolean.FALSE;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        WithholdingType that = (WithholdingType) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
