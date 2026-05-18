package com.asg.shipping.MafiTrailerDateUpdateForm.util;

import com.asg.shipping.MafiTrailerDateUpdateForm.dto.*;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiDtl;
import com.asg.shipping.MafiTrailerDateUpdateForm.entity.ShipBlMafiHdr;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MafiTrailerDateUpdateFormMapper {

	public MafiTrailerDateUpdateFormResponse toMafiTrailerResponse(ShipBlMafiHdr header, VoyageProjection voyage,
			List<ShipBlMafiDtl> details) {
		if (header == null || voyage == null || details == null) {
			return null;
		}

		MafitrailerHeaderDTO responseHeader = MafitrailerHeaderDTO.builder()
                .transactionPoid(header.getTransactionPoid())
                .transactionDate(header.getTransactionDate()).docRef(header.getDocRef())
                .voyageTransactionPoid(header.getVoyageTransactionPoid())
                .voyageNo(voyage.getVoyageNo()).jobNo(voyage.getJobNo())
                .linePoid(voyage.getLinePoid())
				.lineDetail(Map.of("poid", voyage.getLinePoid(), "code", voyage.getLineCode(), "description",
						voyage.getLineName()))
				.agentReference(header.getAgentReference()).remarks(header.getRemarks())
				.vesselPoid(voyage.getVesselPoid()).vesselDetail(Map.of("poid", voyage.getVesselPoid(), "code",
						voyage.getVesselCode(), "description", voyage.getVesselName()))
                .createdDate(header.getCreatedDate()).createdBy(header.getCreatedBy())
                .lastModifiedBy(header.getLastModifiedBy()).lastModifiedDate(header.getLastModifiedDate())
				.build();

		List<MafiDetailDto> detailsDto = new ArrayList<>();
		details.forEach(val -> {
			MafiDetailDto detailDto = new MafiDetailDto();

			detailDto.setDetRowId(val.getDetRowId());
			detailDto.setBlPoid(val.getBlPoid());
			detailDto.setMafiRef(val.getMafiRef());
			detailDto.setMafiSize(val.getMafiSize());
			detailDto.setMafiFreeDays(val.getMafiFreeDays());
			detailDto.setRemarks(val.getRemarks());
			detailDto.setMafiEmptyDate(val.getMafiEmptyDate());
			detailDto.setBackLoadDate(val.getBackLoadDate());

			detailsDto.add(detailDto);
		});

		return MafiTrailerDateUpdateFormResponse.builder()
                .mafiHeader(responseHeader).mafiDetails(detailsDto).build();
	}

	public void updateShipBlMafiHdr(ShipBlMafiHdr entity, MafiTrailerDateUpdateFormRequest request, String userId) {
		if (entity == null || request == null || request.getMafiHeader() == null) {
			return;
		}
		entity.setAgentReference(request.getMafiHeader().getAgentReference());
		entity.setRemarks(request.getMafiHeader().getRemarks());
		entity.setTransactionDate(request.getMafiHeader().getTransactionDate());

	}

}
