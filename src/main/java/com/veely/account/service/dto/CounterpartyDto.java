package com.veely.account.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CounterpartyDto {

    private Long id;

    @NotNull(message = "Il tipo è obbligatorio")
    private Long kindId;

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 255)
    private String name;

    @Size(max = 30)
    private String vatNumber;

    @Size(max = 30)
    private String taxCode;

    @Size(max = 255)
    private String pec;

    @Size(max = 20)
    private String sdiCode;

    @Size(max = 50)
    private String iban;

    private String notes;
}
