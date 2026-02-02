package com.asg.shipping.shippingofoqv2.mapper;

import com.asg.shipping.shippingofoqv2.dto.*;
import com.asg.shipping.shippingofoqv2.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;

@Component
@Slf4j
public class OFOQMapper {

    public OFOQApiDataHdrDto toHeaderDto(OfoqApiDataHdrEntity entity,String functionalReference) {
        log.info("Header mapping - apiProvisionalMfNo: {}, apiProvisionalStatus: {}, manifestNo: {}, manifestStatus: {}",
                entity.getApiProvisionalMfNo(), entity.getApiProvisionalStatus(), entity.getManifestNo(), entity.getManifestStatus());

        return OFOQApiDataHdrDto.builder()
                .transactionPoid(entity.getTransactionPoid())
                .docRef(entity.getDocRef())
                .transactionDate(entity.getTransactionDate() != null ? new java.util.Date(entity.getTransactionDate().getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null)
                .voyageNo(entity.getVoyageNo())
                .vesselPoid(entity.getVesselPoid())
                .arrivalDate(entity.getArrivalDate() != null ? new java.util.Date(entity.getArrivalDate().getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null)
                .rotationNumber(entity.getRotationNumber())
                .apiProvisionalMfNo(entity.getApiProvisionalMfNo())
                .apiProvisionalStatus(entity.getApiProvisionalStatus())
                .manifestNo(entity.getManifestNo())
                .manifestStatus(entity.getManifestStatus())
                .functionalReference(functionalReference)
                .remarks(entity.getRemarks())
                .deleted(entity.getDeleted())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate() != null ? entity.getCreatedDate().toLocalDateTime() : null)
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate() != null ? entity.getLastModifiedDate().toLocalDateTime() : null)
                .build();
    }

    public OFOQItemDtlDto toLineDetailDto(OFOQItemDtlEntity entity) {
        return OFOQItemDtlDto.builder()
                .detRowId(entity.getDetRowId())
                .vesselVoyagePoid(entity.getVesselVoyagePoid())
                .lineName(entity.getLineName())
                .vesselName(entity.getVesselName())
                .voyageNo(entity.getVoyageNo())
                .jobNo(entity.getJobNo())
                .arrivalDate(entity.getArrivalDate() != null ? new java.util.Date(entity.getArrivalDate().getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null)
                .sailDate(entity.getSailDate() != null ? new java.util.Date(entity.getSailDate().getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null)
                .checked(entity.getChecked())
                .drillDownLinkInfo(entity.getDrilldownLinkInfo())
                .build();
    }

    public OFOQManifestAmendmentResponse toAmendmentResponseDto(OFOQManifestAmendmentResponseDtlEntity entity) {
        if (entity == null) return null;
        return OFOQManifestAmendmentResponse.builder()
                .detRowId(entity.getDetRowId())
                .date(entity.getResponseDate() != null ? new java.util.Date(entity.getResponseDate().getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null)
                .blNumber(entity.getXmlBlNumber())
                .functionalReference(entity.getFunctionalRef())
                .statusCode(entity.getStatusCode())
                .responseMessage(entity.getResponseMsg())
                .processingStatus(entity.getProcessingStatus())
                .build();
    }



    public OFOQAmendBLDto toAmendBLDto(OFOQAmendBlDtlEntity entity) {
        if (entity == null){
            return  null;
        }
        return OFOQAmendBLDto.builder()
                .detRowId(entity.getDetRowId())
                .blNumber(entity.getBlNumber())
                .functionalReference(entity.getFunctionalRef())
                .statusCode(entity.getStatusCode())
                .response(entity.getResponseMsg())
                .processingStatus(entity.getProcessingStatus())
                .amendmentRequest(entity.getAmendmentRequestNo())
                .manifestStatus(entity.getManifestStatus())
                .build();
    }

    public OFOQCheckStatusManifestResponse toManifestResponseDto(OFOQManifestResponseDtlEntity entity) {
        OFOQCheckStatusManifestResponse dto = new OFOQCheckStatusManifestResponse();
        dto.setTransactionPoid(entity.getTransactionPoid());
        dto.setDetRowId(entity.getDetRowId());
        dto.setDate(entity.getResponseDate() != null ?
            entity.getResponseDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null);
        dto.setFunctionalReference(entity.getFunctionalRef());
        dto.setStatusCode(String.valueOf(entity.getStatusCode()));
        dto.setResponseMessage(entity.getResponseMessage());
        dto.setProcessingStatus(entity.getProcessingStatus());
        return dto;
    }

    public OFOQVoyageDataResponse toVoyageDataResponse(OFOQApiDataHdrDto header,
                                                       List<OFOQItemDtlDto> lineDetail,
                                                        List<OFOQManifestAmendmentResponse> manifestAmendmentResponse,
                                                        List<OFOQAmendBLDto> amendBL,
                                                        List<OFOQCheckStatusManifestResponse> manifestResponse) {
        return OFOQVoyageDataResponse.builder()
                .header(header)
                .lineDetail(lineDetail)
                .manifestAmendmentResponse(manifestAmendmentResponse)
                .amendBL(amendBL)
                .manifestResponse(manifestResponse)
                .build();
    }
}
