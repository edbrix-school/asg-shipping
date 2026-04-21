package com.asg.shipping.linetariffs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new Line Tariff
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineTariffCreateDTO {

    @NotNull(message = "Line is required")
    private Long linePoid;

    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;

    @NotNull(message = "Period from date is required")
    private LocalDate periodFrom;

    @NotNull(message = "Period to date is required")
    private LocalDate periodTo;

    @Pattern(regexp = "^[YN]?$", message = "DMG from same day must be Y or N")
    private String dmgFromSameday;

    @Pattern(regexp = "^[YN]?$", message = "DMG from next day must be Y or N")
    private String dmgFromNextday;

    @Pattern(regexp = "^[YN]?$", message = "DMG skip holidays must be Y or N")
    private String dmgSkipHolidays;

    @Pattern(regexp = "^[YN]?$", message = "DMG skip weekends must be Y or N")
    private String dmgSkipWeekends;

    @Pattern(regexp = "^[YN]?$", message = "DMG base slab after free must be Y or N")
    private String dmgBaseslabAfterFree;

    @Pattern(regexp = "^[YN]?$", message = "DTN from same day must be Y or N")
    private String dtnFromSameday;

    @Pattern(regexp = "^[YN]?$", message = "DTN from next day must be Y or N")
    private String dtnFromNextday;

    @Pattern(regexp = "^[YN]?$", message = "DTN skip holidays must be Y or N")
    private String dtnSkipHolidays;

    @Pattern(regexp = "^[YN]?$", message = "DTN skip weekends must be Y or N")
    private String dtnSkipWeekends;

    @Pattern(regexp = "^[YN]?$", message = "DTN base slab after free must be Y or N")
    private String dtnBaseslabAfterFree;

    @Size(max = 25, message = "Payable currency must not exceed 25 characters")
    private String payableCurrency;

    @Size(max = 25, message = "Receivable currency must not exceed 25 characters")
    private String receivableCurrency;

    @Size(max = 25, message = "Document reference must not exceed 25 characters")
    private String docRef;

    private Long companyPoid;

    private Integer seqno;

    private String extraTariff;

    @Valid
    private List<TariffDetailCreateDTO> importDemurrageCollectable;

    @Valid
    private List<TariffDetailCreateDTO> importDemurragePayable;

    @Valid
    private List<TariffDetailCreateDTO> exportDetentionCollectable;

    @Valid
    private List<TariffDetailCreateDTO> exportDetentionPayable;
}

