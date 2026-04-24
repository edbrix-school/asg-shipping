package com.asg.shipping.receipts.service.impl;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importmanifestupdate.respository.ShipBlManifestHdrRepository;
import com.asg.shipping.receipts.dto.ReceiptCharges;
import com.asg.shipping.receipts.dto.ReceiptContainerDto;
import com.asg.shipping.receipts.dto.ReceiptPaymentDetailDto;
import com.asg.shipping.receipts.dto.ReceiptsCreateDto;
import com.asg.shipping.receipts.dto.ReceiptsUpdateDto;
import com.asg.shipping.receipts.entity.ArShReceiptHdr;
import com.asg.shipping.receipts.repository.ShipReceiptProcRepository;
import com.asg.shipping.receipts.util.ValidationMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShippingReceiptValidationService {

	private final ShipReceiptProcRepository procRepository;
	private final ShipBlManifestHdrRepository manifestHdrRepository;

	public void validateReceiptCreation(ReceiptsCreateDto createDto) {
		log.info("Starting receipt creation validation");

        validateMandatoryField(createDto.getBlPoid(),createDto.getToken());
		validateBlPoid(createDto.getBlPoid());
		validatePrintCustomer(createDto.getPrintDoCustomerPoid(), createDto.getBlPoid());
		validateAmount(createDto.getCharges(),createDto.getPaymentDetail());
		validateReceiptAmount(createDto.getPaymentDetail(), createDto.getCharges(), createDto.getContainer());
		validatePaymentMethods(createDto.getPaymentDetail(), createDto.getBlPoid(), createDto.getCompanyPoid());
		validateBlacklistedCustomers(createDto.getPaymentDetail());
		validateCashPayments(createDto.getPaymentDetail());
		validateDemurrageAmounts(createDto.getBlPoid(), createDto.getCharges(), createDto.getContainer());
        validateDuplicateBlReceipt(createDto.getBlPoid(), createDto.getRemarks());

		log.info("Receipt creation validation completed successfully");
	}

	private void validateAmount(List<ReceiptCharges> charges,
								List<ReceiptPaymentDetailDto> paymentDetails) {

		BigDecimal chargesAmount = sumAmounts(
				charges,
				ReceiptCharges::getAmount
		);

		BigDecimal paymentDetailAmount = sumAmounts(
				paymentDetails,
				ReceiptPaymentDetailDto::getAmount
		);

		if (chargesAmount.compareTo(paymentDetailAmount) != 0) {
			throw new ValidationException(
					"Total charges amount must equal the payment detail amount"
			);
		}
	}
	private <T> BigDecimal sumAmounts(List<T> list,
									  Function<T, BigDecimal> amountExtractor) {

		if (list == null || list.isEmpty()) {
			return BigDecimal.ZERO;
		}

		return list.stream()
				.map(amountExtractor)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}


	private void validateMandatoryField(Long blPoid, Long token) {
		if (blPoid == null) {
			log.error("Mandatory field blPoid is missing");
			throw new ValidationException("BL POID is required");
		}
	}


	public void validateReceiptUpdate(ReceiptsUpdateDto updateDto, ArShReceiptHdr existingReceipt) {
		log.info("Starting receipt update validation");

		validateMandatoryField(existingReceipt.getBlPoid(), updateDto.getToken());
		validateBlPoid(updateDto.getBlPoid());


		validatePrintStatus(existingReceipt);

		validateReleaseTypeMatching(existingReceipt);

		validateReceiptAmount(updateDto.getPaymentDetail(), updateDto.getCharges(), updateDto.getContainer());
		validatePaymentMethods(updateDto.getPaymentDetail(), updateDto.getBlPoid(), updateDto.getCompanyPoid());
		validateBlacklistedCustomers(updateDto.getPaymentDetail());
		validateCashPayments(updateDto.getPaymentDetail());
		validateDemurrageAmounts(updateDto.getBlPoid(), updateDto.getCharges(), updateDto.getContainer());
        validateDuplicateBlReceipt(updateDto.getBlPoid(), updateDto.getRemarks());

		log.info("Receipt update validation completed successfully");
	}

	private void validatePrintCustomer(Long printDoCustomerPoid, Long blPoid) {
		if (printDoCustomerPoid == null && blPoid != null) {
			throw new ValidationException("Print DO Customer is required");
		}
	}

	private void validatePrintStatus(ArShReceiptHdr existingReceipt) {
		if (existingReceipt.getPrintStatus() != null && 
			existingReceipt.getPrintStatus().equalsIgnoreCase("Y")) {
			throw new ValidationException(ValidationMessages.RECEIPT_PRINTED);
		}
	}


	private void validateReceiptAmount(List<ReceiptPaymentDetailDto> payments, 
										   List<ReceiptCharges> charges, 
										   List<ReceiptContainerDto> containers) {
		BigDecimal totalAmount = BigDecimal.ZERO;

		if (payments != null) {
			for (ReceiptPaymentDetailDto payment : payments) {
				if (payment.getAmount() != null) {
					totalAmount = totalAmount.add(payment.getAmount());
				}
			}
		}

		if (charges != null) {
			for (ReceiptCharges charge : charges) {
				if (charge.getAmount() != null && "Y".equalsIgnoreCase(charge.getAmountSelect())) {
					totalAmount = totalAmount.add(charge.getAmount());
				}
			}
		}

		if (containers != null) {
			for (ReceiptContainerDto container : containers) {
				if (container.getDmChargeAmt() != null) {
					totalAmount = totalAmount.add(container.getDmChargeAmt());
				}
			}
		}

		if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ValidationException(ValidationMessages.RECEIPT_AMOUNT_INVALID);
		}
	}


	private void validateReleaseTypeMatching(ArShReceiptHdr existingReceipt) {

		if (!existingReceipt.getBlReleaseTypeOffice().equalsIgnoreCase(
				existingReceipt.getOrignalBlReleaseType())) {
			throw new ValidationException(ValidationMessages.RELEASE_TYPE_MISMATCH);
		}
	}

	private void validatePaymentMethods(List<ReceiptPaymentDetailDto> payments, Long blPoid, Long companyPoid) {
		if (payments == null || payments.isEmpty()) {
			return;
		}

		for (ReceiptPaymentDetailDto payment : payments) {
			if (payment.getPymtType() == null || payment.getPymtType().trim().isEmpty()) {
				continue;
			}

			String pymtType = payment.getPymtType().toUpperCase();
			if (pymtType.contains("CHEQUE")) {
				validateChequePayment(payment, blPoid);
			} else if (pymtType.contains("TT")) {
				validateTTPayment(payment, companyPoid);
			} else if (pymtType.contains("ROUNDOFF")) {
				validateRoundoffPayment(payment, companyPoid);
			}
		}
	}

	private void validateChequePayment(ReceiptPaymentDetailDto payment, Long blPoid) {
		if (payment.getAmount() == null && payment.getBankPoid() == null) {
			return;
		}

		if (payment.getAmount() == null || 
			payment.getChqCardno() == null || payment.getChqCardno().trim().isEmpty() ||
			payment.getAccountNo() == null || payment.getAccountNo().trim().isEmpty() ||
			payment.getBankPoid() == null || 
			payment.getChqDate() == null) {
			throw new ValidationException(ValidationMessages.CHEQUE_FIELDS_MISSING);
		}

        // Legacy: PDC Date against Invoice check
        String pdcResult = procRepository.validatePdcDateAgainstInvoice(blPoid, payment.getChqDate());
        if ("1".equals(pdcResult)) {
            throw new ValidationException("Check Post dated cheque date for chqno=" + payment.getChqCardno() + ", " + payment.getChqDate());
        }

        // Legacy: PDC range check (SHPOSTCHQ)
        String postChqResult = procRepository.validateChequeDate(payment.getChqDate(), "SHPOSTCHQ");
        if ("-1".equals(postChqResult)) {
            throw new ValidationException("Check Post dated cheque date for chqno=" + payment.getChqCardno() + ", " + payment.getChqDate());
        }

        // Legacy: Pre-dated range check (SHPRECHQ)
        String preChqResult = procRepository.validateChequeDate(payment.getChqDate(), "SHPRECHQ");
        if ("1".equals(preChqResult)) {
            throw new ValidationException("Check Pre dated cheque date for chqno=" + payment.getChqCardno() + ", " + payment.getChqDate());
        }
	}

	private void validateTTPayment(ReceiptPaymentDetailDto payment, Long companyPoid) {
		if (payment.getAmount() == null || payment.getTtBankPoid() == null) {
			throw new ValidationException(ValidationMessages.TT_FIELDS_MISSING);
		}

        // Legacy: TT Bank company matching
        Long bankCompany = procRepository.getBankCompany(payment.getTtBankPoid());
        if (bankCompany != null && !bankCompany.equals(companyPoid)) {
            throw new ValidationException("TT Bank company not matching with selected BL company");
        }
	}

	private void validateRoundoffPayment(ReceiptPaymentDetailDto payment, Long companyPoid) {
		if (payment.getAmount() == null) {
			return;
		}

		String roundLimitStr = procRepository.getGlobalParameter("ROUNDING_LIMIT", "GROUP", companyPoid, "0");
        BigDecimal roundLimitValue = new BigDecimal(roundLimitStr != null ? roundLimitStr : "0");
		if (payment.getAmount().doubleValue() > roundLimitValue.doubleValue()) {
			throw new ValidationException(
				ValidationMessages.ROUNDOFF_LIMIT_EXCEEDED.replace("{0}", roundLimitValue.toString())
			);
		}
	}

	private void validateBlacklistedCustomers(List<ReceiptPaymentDetailDto> payments) {
		if (payments == null || payments.isEmpty()) {
			return;
		}

		for (ReceiptPaymentDetailDto payment : payments) {
			if (payment.getPymtType() != null && payment.getPymtType().toUpperCase().contains("CHEQUE")) {
				if (payment.getAccountNo() != null && !payment.getAccountNo().trim().isEmpty() &&
						payment.getBankPoid() != null) {
					String isBlacklisted = procRepository.validateBlacklistedCustomer(
							payment.getAccountNo(), payment.getBankPoid());
					
					if (isBlacklisted != null && isBlacklisted.contains("Y")) {
						throw new ValidationException(
							ValidationMessages.CUSTOMER_BLACKLISTED.replace("{0}", payment.getAccountNo())
						);
					}
				}
			}
		}
	}

	private void validateCashPayments(List<ReceiptPaymentDetailDto> payments) {
		if (payments == null || payments.isEmpty()) {
			return;
		}

		for (ReceiptPaymentDetailDto payment : payments) {
			if (payment.getPymtType() != null && payment.getPymtType().equalsIgnoreCase("CASH")) {
				validateCashRounding(payment);
			}
		}

		validateSplitPayments(payments);
	}

	private void validateCashRounding(ReceiptPaymentDetailDto payment) {
		if (payment.getAmount() != null) {
			BigDecimal cashAmount = payment.getAmount();
			// Convert to cents (hundredths) and check if divisible by 5
			BigDecimal cents = cashAmount.multiply(new BigDecimal("100")).setScale(0, java.math.RoundingMode.HALF_UP);
			if (cents.remainder(new BigDecimal("5")).compareTo(BigDecimal.ZERO) != 0) {
				throw new ValidationException(
					ValidationMessages.CASH_ROUNDING_INVALID.replace("{0}", cashAmount.toString())
				);
			}
		}
	}

	private void validateSplitPayments(List<ReceiptPaymentDetailDto> payments) {
		int paymentCount = 0;

		for (ReceiptPaymentDetailDto payment : payments) {
			if (payment.getAmount() != null && payment.getAmount().doubleValue() > 0) {
				paymentCount++;
			}
		}

		if (paymentCount > 1) {
			throw new ValidationException(ValidationMessages.SPLIT_PAYMENT_NOT_ALLOWED);
		}
	}

	private void validateDemurrageAmounts(Long blPoid, List<ReceiptCharges> charges, List<ReceiptContainerDto> containers) {
		if ((charges == null || charges.isEmpty()) && (containers == null || containers.isEmpty())) {
			return;
		}

		BigDecimal totalDemChargeAdded = BigDecimal.ZERO;
		BigDecimal totalDemBlRcptCollected = BigDecimal.ZERO;

		if (charges != null) {
			for (ReceiptCharges charge : charges) {
				if (charge.getAmount() != null) {
					totalDemChargeAdded = totalDemChargeAdded.add(charge.getAmount());
				}
			}
		}

		if (containers != null) {
			for (int i = 0; i < containers.size(); i++) {
				ReceiptContainerDto container = containers.get(i);
				if (container.getDmChargeAmt() != null) {
					totalDemBlRcptCollected = totalDemBlRcptCollected.add(container.getDmChargeAmt());
					
					// Validate each container's demurrage amount
					if (blPoid != null && container.getContainerNo() != null && container.getDmChargeAmt() != null) {
						BigDecimal validatedAmount = procRepository.validateDemurrageAmount(
								blPoid, container.getContainerNo(), container.getDmChargeAmt());
						
						if (validatedAmount == null || validatedAmount.compareTo(BigDecimal.ZERO) < 0) {
							throw new ValidationException(
								"Demurrage validation failed for container " + container.getContainerNo() + 
								" with amount " + container.getDmChargeAmt());
						}
					}
				}
			}
		}

		if (totalDemChargeAdded.doubleValue() > 0 && totalDemBlRcptCollected.doubleValue() > 0) {
			if (totalDemChargeAdded.doubleValue() != totalDemBlRcptCollected.doubleValue()) {
				throw new ValidationException(
					"Demurrage amount mismatch. Charges total: " + totalDemChargeAdded +
					", Container demurrage total: " + totalDemBlRcptCollected);
			}
		}
	}


	private void validateBlPoid(Long blPoid) {


		if (!manifestHdrRepository.existsById(blPoid)) {
			log.warn("BL POID validation failed: {} not found", blPoid);
			throw new ResourceNotFoundException(
					"BL",
					"transactionPoid",
					blPoid
			);
		}
	}

    private void validateDuplicateBlReceipt(Long blPoid, String remarks) {
        if (blPoid == null) return;
        String result = procRepository.validateDuplicateBlReceipt(blPoid, remarks);
        if ("TRUE".equalsIgnoreCase(result)) {
            throw new ValidationException("BL already invoiced with remarks (" + remarks + ") reference, please check remarks...");
        }
    }


	}

