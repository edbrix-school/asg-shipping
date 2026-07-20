package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.dto.BookingSelectionRowDto;
import java.util.List;

public interface ExportManifestBlBookingSelectionRepository {

    /**
     * Pending mate lines for Select Booking popup (legacy {@code VwPendingMateToBlView1} / {@code VW_PENDING_MATE_TO_BL}).
     *
     * @param issueVesselVoyagePoid Issue Vessel Voyage POID ({@code pVoyageVesselPoid1} / {@code Pvoyagepoid} on view)
     * @param bookingMateVoyageNo    Booking Mate Voyage ({@code inputVoyageLoad} → view {@code VoyageNo})
     */
    List<BookingSelectionRowDto> findPendingMateToBl(
            Long groupPoid,
            Long companyPoid,
            Long issueVesselVoyagePoid,
            String bookingMateVoyageNo,
            Long linePoid,
            String containerNo,
            String bookingNo);
}
