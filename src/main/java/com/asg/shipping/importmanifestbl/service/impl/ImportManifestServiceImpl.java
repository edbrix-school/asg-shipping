package com.asg.shipping.importmanifestbl.service.impl;

import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importManifestUpdate.entity.*;
import com.asg.shipping.importManifestUpdate.respository.*;
import com.asg.shipping.importManifestUpdate.util.ImportManifestBlMapper;
import com.asg.shipping.importmanifestbl.service.ImportManifestBlService;
import com.asg.shipping.address.entity.AddressDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class ImportManifestServiceImpl implements ImportManifestBlService {

    private final ShipBlManifestHdrRepository headerRepository;
    private final ImportManifestBlProcRepository procRepository;
    private final AddressDetailsRepository addressDetailsRepository;
    private final com.asg.shipping.importManifestUpdate.service.ImportManifestBlServiceImpl updateService;
    private final ImportManifestBlMapper mapper;

    @Override
    public ImportManifestBlRequestDto getImportManifest(Long transactionPoId) {
        ShipBlManifestHdr entity = findEntityById(transactionPoId);
        log.info("Getting Import Manifest BL with id: {}", transactionPoId);


        if (entity.getBlType() != null && !"IMPORT".equalsIgnoreCase(entity.getBlType())) {
            throw new ResourceNotFoundException("Import Manifest BL", "transactionPoid", transactionPoId.toString());
        }

        ImportManifestBlRequestDto requestDto = mapper.mapToDto(entity);

        ImportManifestBlRequestDto dto =   updateService.loadDetailTables(requestDto,transactionPoId);



        log.info("Successfully retrieved Import Manifest BL with id: {}", transactionPoId);
        return dto;
    }

    @Override
    @Transactional
    public void delete(Long transactionPoId) {
        try {
            ShipBlManifestHdr entity = findEntityById(transactionPoId);
            entity.setDeleted("Y");
            headerRepository.save(entity);
            log.info("Soft deleted header for transactionPoId: {}", transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to delete: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error deleting Import Manifest BL for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request) {
        try {
            findEntityById(transactionPoId);
            return procRepository.updateEmailVerification(transactionPoId, request);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to update: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error updating email verification for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public ResendCanResponseDto resendCan(Long transactionPoId) {
        try {
            ShipBlManifestHdr entity = findEntityById(transactionPoId);
            return procRepository.resendCan(entity.getVoyageTransactionPoid(), transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to resend CAN: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error resending CAN for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId) {
        try {
            findEntityById(transactionPoId);
            return procRepository.sendEdiEmails(transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to send EDI emails: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error sending EDI emails for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public LoadEmailFaxResponseDto loadEmailFax(Long transactionPoId, LoadEmailFaxRequestDto request) {
        try {
            findEntityById(transactionPoId);
            var addressDetails = addressDetailsRepository.findByAddressMasterPoidAndAddressType(
                    request.getAddressMasterPoid(), "CAN");
            var emailFaxDetails = addressDetails.stream()
                    .map(ad -> EmailFaxDetailDto.builder()
                            .addressPoid(Long.valueOf(ad.getAddressPoid()))
                            .email1(ad.getEmail())
                            .email2(ad.getEmail2())
                            .fax(ad.getFax())
                            .addressType(request.getAddressType())
                            .build())
                    .toList();
            log.info("Loaded email/fax data for transactionPoId: {}, count: {}", transactionPoId, emailFaxDetails.size());
            return LoadEmailFaxResponseDto.builder().emailFaxDetails(emailFaxDetails).build();
        } catch (ResourceNotFoundException e) {
            log.error("Failed to load email/fax: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error loading email/fax data for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    public BlStatusResponseDto getBlStatus(Long transactionPoId) {
        try {
            findEntityById(transactionPoId);
            return procRepository.getBlStatus(transactionPoId);
        } catch (ResourceNotFoundException e) {
            log.error("Failed to get BL status: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error getting BL status for transactionPoId: {}", transactionPoId, e);
            throw e;
        }
    }

    @Override
    public ImportManifestBlRequestDto createImportManifestBl(ImportManifestBlCreateDto request, Long companyPoid, Long groupPoid) {
      return updateService.createImportManifestBl(request);
    }

    private ShipBlManifestHdr findEntityById(Long transactionPoId) {
        return headerRepository.findById(transactionPoId)
                .orElseThrow(() -> new ResourceNotFoundException("Ship BL Manifest", "transactionPoId", transactionPoId));
    }



}
