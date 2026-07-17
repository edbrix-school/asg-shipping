package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.dto.BookingSelectionItemDto;
import java.util.List;

public interface ExportManifestBlLoadBookingRepository {

    void stageBookingSelections(Long groupPoid, Long companyPoid, List<BookingSelectionItemDto> selections);

    String funcLoadBookingToBl(String userPoid, Long voyageTransactionPoid);
}
