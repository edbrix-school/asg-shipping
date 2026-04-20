package com.asg.shipping.linecommission.util;

import com.asg.shipping.containertypes.dto.ContainerTypeDto;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import com.asg.shipping.containertypes.util.ContainerTypeMapper;
import com.asg.shipping.linecommission.dto.ContainerRateDto;
import com.asg.shipping.linecommission.dto.LineCommissionResponse;
import com.asg.shipping.linecommission.dto.LineCommissionRequest;
import com.asg.shipping.linecommission.dto.LocalShareDto;
import com.asg.shipping.linecommission.dto.OtherRemunerationDto;
import com.asg.shipping.linecommission.entity.ShipLineCommCntnrDtlEntity;
import com.asg.shipping.linecommission.entity.ShipLineCommDtlEntity;
import com.asg.shipping.linecommission.entity.ShipLineCommHdrEntity;
import com.asg.shipping.linecommission.entity.ShipLineCommLocalDtlEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LineCommissionMapper {

    private final ContainerTypeMapper containerTypeMapper;

    public ShipLineCommHdrEntity toCreateHeaderEntity(LineCommissionRequest request,
                                                      Long groupPoid,
                                                      Long companyPoid,
                                                      String userId) {
        ShipLineCommHdrEntity hdr = new ShipLineCommHdrEntity();
//        hdr.setTransactionPoid(-999L); // placeholder; trigger overwrites with sequence NEXTVAL
        hdr.setTransactionDate(request.getTransactionDate());
        hdr.setGroupPoid(groupPoid);
        hdr.setCompanyPoid(companyPoid);
        hdr.setLinePoid(request.getLinePoid());
        hdr.setPeriodFrom(request.getPeriodFrom());
        hdr.setPeriodTo(request.getPeriodTo());
        hdr.setRenewalDate(request.getRenewalDate());
        hdr.setCurrencyPoid(request.getCurrencyPoid());
        hdr.setDescription(request.getDescription());
        hdr.setRemarks(request.getRemarks());
        hdr.setDeleted("N");
        return hdr;
    }

    public void applyUpdateHeader(ShipLineCommHdrEntity hdr,
                                 LineCommissionRequest request,
                                 Long companyPoid,
                                 String userId) {
        hdr.setCompanyPoid(companyPoid);
        hdr.setLinePoid(request.getLinePoid());
        hdr.setPeriodFrom(request.getPeriodFrom());
        hdr.setPeriodTo(request.getPeriodTo());
        hdr.setRenewalDate(request.getRenewalDate());
        hdr.setCurrencyPoid(request.getCurrencyPoid());
        hdr.setDescription(request.getDescription());
        hdr.setRemarks(request.getRemarks());
    }

    public List<ShipLineCommCntnrDtlEntity> toContainerEntities(long transactionPoid, LineCommissionRequest request, String userId) {
        List<ShipLineCommCntnrDtlEntity> out = new ArrayList<>();
        List<ContainerRateDto> list = request.getContainerRates() != null ? request.getContainerRates() : List.of();
        for (int i = 0; i < list.size(); i++) {
            ContainerRateDto d = list.get(i);
            if (d == null) continue;
            out.add(toNewContainerEntity(transactionPoid, (long) (i + 1), d, userId));
        }
        return out;
    }

    public ShipLineCommCntnrDtlEntity toNewContainerEntity(long transactionPoid, long detRowId, ContainerRateDto d, String userId) {
        ShipLineCommCntnrDtlEntity e = new ShipLineCommCntnrDtlEntity();
        e.setTransactionPoid(transactionPoid);
        e.setDetRowId(detRowId);
        applyUpdateContainerEntity(e, d, userId);
        return e;
    }

    public void applyUpdateContainerEntity(ShipLineCommCntnrDtlEntity e, ContainerRateDto d, String userId) {
        e.setContainerTypePoid(d.getContainerTypePoid());
        e.setImportBoxRate(d.getImportBoxRate());
        e.setExportBoxRate(d.getExportBoxRate());
        e.setTranshipBoxRate(d.getTranshipBoxRate());
        e.setShortLegAmount(d.getShortLegAmount());
        e.setRemarks(d.getRemarks());
    }

    public List<ShipLineCommDtlEntity> toOtherRemunerationEntities(long transactionPoid, LineCommissionRequest request, String userId) {
        List<ShipLineCommDtlEntity> out = new ArrayList<>();
        List<OtherRemunerationDto> list = request.getOtherRemunerations() != null ? request.getOtherRemunerations() : List.of();
        for (int i = 0; i < list.size(); i++) {
            OtherRemunerationDto d = list.get(i);
            if (d == null) continue;
            out.add(toNewOtherRemunerationEntity(transactionPoid, (long) (i + 1), d, userId));
        }
        return out;
    }

    public ShipLineCommDtlEntity toNewOtherRemunerationEntity(long transactionPoid, long detRowId, OtherRemunerationDto d, String userId) {
        ShipLineCommDtlEntity e = new ShipLineCommDtlEntity();
        e.setTransactionPoid(transactionPoid);
        e.setDetRowId(detRowId);
        applyUpdateOtherRemunerationEntity(e, d, userId);
        return e;
    }

    public void applyUpdateOtherRemunerationEntity(ShipLineCommDtlEntity e, OtherRemunerationDto d, String userId) {
        e.setRemunerationPoid(d.getRemunerationPoid());
        e.setCurrencyPoid(d.getCurrencyPoid());
        e.setAmount(d.getAmount());
        e.setPercent(d.getPercent());
        e.setPaybackPercent(d.getPaybackPercent());
        e.setOurBookingPercentage(d.getOurBookingPercentage());
        e.setDestLoadPercentage(d.getExpImpPreCollection());
        e.setShortLegPercentage(d.getStLegAmount());
        e.setSplEqpPercentage(d.getSplEqpPercentage());
        e.setAmountPerTue(d.getAmountPerTeu());
        e.setPpBookingPercentageCollect(d.getPrincipalBookPutClt());
        e.setRemarks(d.getRemarks());
    }

    public List<ShipLineCommLocalDtlEntity> toLocalShareEntities(long transactionPoid, LineCommissionRequest request, String userId) {
        List<ShipLineCommLocalDtlEntity> out = new ArrayList<>();
        List<LocalShareDto> list = request.getLocalShares() != null ? request.getLocalShares() : List.of();
        for (int i = 0; i < list.size(); i++) {
            LocalShareDto d = list.get(i);
            if (d == null) continue;
            out.add(toNewLocalShareEntity(transactionPoid, (long) (i + 1), d, userId));
        }
        return out;
    }

    public ShipLineCommLocalDtlEntity toNewLocalShareEntity(long transactionPoid, long detRowId, LocalShareDto d, String userId) {
        ShipLineCommLocalDtlEntity e = new ShipLineCommLocalDtlEntity();
        e.setTransactionPoid(transactionPoid);
        e.setDetRowId(detRowId);
        applyUpdateLocalShareEntity(e, d, userId);
        return e;
    }

    public void applyUpdateLocalShareEntity(ShipLineCommLocalDtlEntity e, LocalShareDto d, String userId) {
        e.setChargePoid(d.getChargePoid());
        e.setPercent(d.getPercent());
        e.setShareAmount(d.getAmount());
        e.setRemarks(d.getRemarks());
    }

    public LineCommissionResponse toResponse(ShipLineCommHdrEntity hdr,
                                            List<ShipLineCommCntnrDtlEntity> cntnr,
                                            List<ShipLineCommDtlEntity> other,
                                            List<ShipLineCommLocalDtlEntity> local) {
        LineCommissionResponse r = new LineCommissionResponse();
        r.setTransactionPoid(hdr.getTransactionPoid());
        r.setDocRef(hdr.getDocRef());
        r.setLinePoid(hdr.getLinePoid());
        r.setDescription(hdr.getDescription());
        r.setRemarks(hdr.getRemarks());
        r.setPeriodFrom(hdr.getPeriodFrom());
        r.setPeriodTo(hdr.getPeriodTo());
        r.setRenewalDate(hdr.getRenewalDate());
        r.setTransactionDate(hdr.getTransactionDate());
        r.setCurrencyPoid(hdr.getCurrencyPoid());
        r.setCreatedBy(hdr.getCreatedBy());
        r.setCreatedDate(hdr.getCreatedDate());
        r.setUpdatedBy(hdr.getLastModifiedBy());
        r.setUpdatedDate(hdr.getLastModifiedDate());
        r.setDeleted(hdr.getDeleted() != null ? hdr.getDeleted() : "N");

        r.setContainerRates(cntnr.stream().map(this::toDto).toList());
        r.setOtherRemunerations(other.stream().map(this::toDto).toList());
        r.setLocalShares(local.stream().map(this::toDto).toList());
        return r;
    }

    private ContainerRateDto toDto(ShipLineCommCntnrDtlEntity e) {
        ContainerRateDto d = new ContainerRateDto();
        d.setDetRowId(e.getDetRowId());
        d.setContainerTypePoid(e.getContainerTypePoid());
        d.setImportBoxRate(e.getImportBoxRate());
        d.setExportBoxRate(e.getExportBoxRate());
        d.setTranshipBoxRate(e.getTranshipBoxRate());
        d.setShortLegAmount(e.getShortLegAmount());
        d.setCreatedBy(e.getCreatedBy());
        d.setCreatedDate(e.getCreatedDate());
        d.setUpdatedBy(e.getLastModifiedBy());
        d.setUpdatedDate(e.getLastModifiedDate());
        d.setRemarks(e.getRemarks());
        return d;
    }

    private OtherRemunerationDto toDto(ShipLineCommDtlEntity e) {
        OtherRemunerationDto d = new OtherRemunerationDto();
        d.setDetRowId(e.getDetRowId());
        d.setRemunerationPoid(e.getRemunerationPoid());
        d.setCurrencyPoid(e.getCurrencyPoid());
        d.setAmount(e.getAmount());
        d.setPercent(e.getPercent());
        d.setPaybackPercent(e.getPaybackPercent());
        d.setOurBookingPercentage(e.getOurBookingPercentage());
        d.setExpImpPreCollection(e.getDestLoadPercentage());
        d.setStLegAmount(e.getShortLegPercentage());
        d.setSplEqpPercentage(e.getSplEqpPercentage());
        d.setAmountPerTeu(e.getAmountPerTue());
        d.setPrincipalBookPutClt(e.getPpBookingPercentageCollect());
        d.setCreatedBy(e.getCreatedBy());
        d.setCreatedDate(e.getCreatedDate());
        d.setUpdatedBy(e.getLastModifiedBy());
        d.setUpdatedDate(e.getLastModifiedDate());
        d.setRemarks(e.getRemarks());
        return d;
    }

    private LocalShareDto toDto(ShipLineCommLocalDtlEntity e) {
        LocalShareDto d = new LocalShareDto();
        d.setDetRowId(e.getDetRowId());
        d.setChargePoid(e.getChargePoid());
        d.setPercent(e.getPercent());
        d.setAmount(e.getShareAmount());
        d.setCreatedBy(e.getCreatedBy());
        d.setCreatedDate(e.getCreatedDate());
        d.setUpdatedBy(e.getLastModifiedBy());
        d.setUpdatedDate(e.getLastModifiedDate());
        d.setRemarks(e.getRemarks());
        return d;
    }


    public List<ContainerTypeDto> toContainerTypeDtos(List<ShipContainerTypeMaster> masters) {
        if (masters == null || masters.isEmpty()) return List.of();
        return masters.stream()
                .map(containerTypeMapper::mapToDto)
                .toList();
    }
}


