package com.asg.shipping.linetariffs.util;

import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.linetariffs.dto.TariffDetailCreateDTO;
import com.asg.shipping.linetariffs.dto.TariffDetailUpdateDTO;

public final class LineTariffSlabValidator {

    private static final String[] SLAB_NAMES = {
            "Slab 1", "Slab 2", "Slab 3", "Slab 4", "Slab 5", "Slab 6", "Slab 7"
    };

    private LineTariffSlabValidator() {
    }

    public static void validate(TariffDetailCreateDTO dto) {
        validate(
                dto.getSlab1Tilldays(),
                dto.getSlab2Tilldays(),
                dto.getSlab3Tilldays(),
                dto.getSlab4Tilldays(),
                dto.getSlab5Tilldays(),
                dto.getSlab6Tilldays(),
                dto.getSlab7Tilldays());
    }

    public static void validate(TariffDetailUpdateDTO dto) {
        validate(
                dto.getSlab1Tilldays(),
                dto.getSlab2Tilldays(),
                dto.getSlab3Tilldays(),
                dto.getSlab4Tilldays(),
                dto.getSlab5Tilldays(),
                dto.getSlab6Tilldays(),
                dto.getSlab7Tilldays());
    }

    private static void validate(Integer slab1Tilldays,
                                 Integer slab2Tilldays,
                                 Integer slab3Tilldays,
                                 Integer slab4Tilldays,
                                 Integer slab5Tilldays,
                                 Integer slab6Tilldays,
                                 Integer slab7Tilldays) {
        Integer[] tillDays = {
                slab1Tilldays, slab2Tilldays, slab3Tilldays, slab4Tilldays,
                slab5Tilldays, slab6Tilldays, slab7Tilldays
        };

        Integer previousDays = null;
        String previousName = null;
        for (int i = 0; i < tillDays.length; i++) {
            Integer days = effectiveSlabDays(tillDays[i]);
            if (days == null) {
                continue;
            }
            if (previousDays != null && days <= previousDays) {
                throw new ValidationException(
                        String.format("Slab days should be in incremental order. %s days (%d) must be greater than %s days (%d)",
                                SLAB_NAMES[i], days, previousName, previousDays));
            }
            previousDays = days;
            previousName = SLAB_NAMES[i];
        }
    }

    /**
     * Unused slab columns are often stored or sent as 0; treat those like null so only entered slabs are validated.
     */
    private static Integer effectiveSlabDays(Integer tillDays) {
        if (tillDays == null || tillDays == 0) {
            return null;
        }
        return tillDays;
    }
}
