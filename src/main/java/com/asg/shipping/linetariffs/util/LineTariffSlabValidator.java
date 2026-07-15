package com.asg.shipping.linetariffs.util;

import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.linetariffs.dto.TariffDetailCreateDTO;
import com.asg.shipping.linetariffs.dto.TariffDetailUpdateDTO;

public final class LineTariffSlabValidator {

    private static final String SLAB1 = "Slab 1";
    private static final String SLAB2 = "Slab 2";
    private static final String SLAB3 = "Slab 3";
    private static final String SLAB4 = "Slab 4";
    private static final String SLAB5 = "Slab 5";
    private static final String SLAB6 = "Slab 6";
    private static final String SLAB7 = "Slab 7";

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
        validateSlabSequence(slab1Tilldays, slab2Tilldays, SLAB1, SLAB2);
        validateSlabSequence(slab2Tilldays, slab3Tilldays, SLAB2, SLAB3);
        validateSlabSequence(slab3Tilldays, slab4Tilldays, SLAB3, SLAB4);
        validateSlabSequence(slab4Tilldays, slab5Tilldays, SLAB4, SLAB5);
        validateSlabSequence(slab5Tilldays, slab6Tilldays, SLAB5, SLAB6);
        validateSlabSequence(slab6Tilldays, slab7Tilldays, SLAB6, SLAB7);
    }

    private static void validateSlabSequence(Integer currentSlab, Integer nextSlab, String currentName, String nextName) {
        if (currentSlab != null && nextSlab != null && nextSlab <= currentSlab) {
            throw new ValidationException(
                    String.format("Slab days should be in incremental order. %s days (%d) must be greater than %s days (%d)",
                            nextName, nextSlab, currentName, currentSlab));
        }
    }
}
