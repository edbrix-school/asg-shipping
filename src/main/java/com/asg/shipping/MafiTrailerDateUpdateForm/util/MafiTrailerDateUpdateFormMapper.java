package com.asg.shipping.mafitrailerdateupdateform.util;

import com.asg.shipping.mafitrailerdateupdateform.dto.*;
import com.asg.shipping.mafitrailerdateupdateform.entity.ShipBlMafiDtl;
import com.asg.shipping.mafitrailerdateupdateform.entity.ShipBlMafiHdr;
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

		MafitrailerHeaderDTO responseHeader = MafitrailerHeaderDTO.builder().voyageNo(voyage.getVoyageNo())
				.jobNo(voyage.getJobNo()).linePoid(voyage.getLinePoid())
				.lineDetail(Map.of("poid", voyage.getLinePoid(), "code", voyage.getLineCode(), "description",
						voyage.getLineName()))
				.agentReference(header.getAgentReference()).remarks(header.getRemarks())
				.vesselPoid(voyage.getVesselPoid()).vesselDetail(Map.of("poid", voyage.getVesselPoid(), "code",
						voyage.getVesselCode(), "description", voyage.getVesselName()))
				.build();

		List<MafiDetailDto> detailsDto = new ArrayList<>();
		details.stream().forEach(val -> {
			MafiDetailDto detailDto = new MafiDetailDto();

			detailDto.setTransactionPoid(val.getId().getTransactionPoid());
			detailDto.setDetRowId(val.getId().getDetRowId());
			detailDto.setBlPoid(val.getBlPoid());
//			detailDto.setBldetail(Map.of("poid", val.getBlPoid()));
			detailDto.setMafiRef(val.getMafiRef());
			detailDto.setMafiSize(val.getMafiSize());
			detailDto.setMafiFreeDays(val.getMafiFreeDays());
			detailDto.setRemarks(val.getRemarks());
			detailDto.setMafiEmptyDate(val.getMafiEmptyDate());
			detailDto.setBackLoadDate(val.getBackLoadDate());

			detailsDto.add(detailDto);
		});

		MafiTrailerDateUpdateFormResponse response = MafiTrailerDateUpdateFormResponse.builder()
				.mafiHeader(responseHeader).mafiDetails(detailsDto).build();

		return response;
	}

	public void updateShipBlMafiHdr(ShipBlMafiHdr entity, MafiTrailerDateUpdateFormRequest request, String userId) {
		if (entity == null || request == null || request.getMafiHeader() == null) {
			return;
		}
		entity.setAgentReference(request.getMafiHeader().getAgentReference());
		entity.setRemarks(request.getMafiHeader().getRemarks());

	}

}
