package com.asg.shipping.receipts.service.impl;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importManifestUpdate.respository.ShipBlManifestHdrRepository;
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
		validateFinancialYearAndPeriod(createDto.getCompanyPoid(), LocalDate.from(createDto.getTransactionDate()));
		validateReceiptAmount(createDto.getPaymentDetail(), createDto.getCharges(), createDto.getContainer());
		validatePaymentMethods(createDto.getPaymentDetail());
		validateBlacklistedCustomers(createDto.getPaymentDetail());
		validateCashPayments(createDto.getPaymentDetail());
		validateDemurrageAmounts(createDto.getBlPoid(), createDto.getCharges(), createDto.getContainer());
		validateCharges(createDto.getCharges());

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
		if (token == null) {
			log.error("Mandatory token  is missing");
			throw new ValidationException("Token is required");
		}
		}


	public void validateReceiptUpdate(ReceiptsUpdateDto updateDto, ArShReceiptHdr existingReceipt) {
		log.info("Starting receipt update validation");

		validateMandatoryField(existingReceipt.getBlPoid(), updateDto.getToken());
		validateBlPoid(updateDto.getBlPoid());


		validatePrintStatus(existingReceipt);

		validateReleaseTypeMatching(existingReceipt);

		validateFinancialYearAndPeriod(updateDto.getCompanyPoid(), LocalDate.from(existingReceipt.getTransactionDate()));
		validateFinancialYearAndPeriod(updateDto.getCompanyPoid(), LocalDate.from(updateDto.getTransactionDate()));

		validateReceiptAmount(updateDto.getPaymentDetail(), updateDto.getCharges(), updateDto.getContainer());
		validatePaymentMethods(updateDto.getPaymentDetail());
		validateBlacklistedCustomers(updateDto.getPaymentDetail());
		validateCashPayments(updateDto.getPaymentDetail());
		validateDemurrageAmounts(updateDto.getBlPoid(), updateDto.getCharges(), updateDto.getContainer());
		validateCharges(updateDto.getCharges());

		log.info("Receipt update validation completed successfully");
	}

	private void validatePrintCustomer(Long printDoCustomerPoid, Long blPoid) {
		if (printDoCustomerPoid == null && blPoid != null) {
			String rcptType = procRepository.getReceiptType(blPoid);
			if (rcptType == null || !rcptType.equalsIgnoreCase("FINALSPLITSRECEIPT")) {
				throw new ValidationException(
					"Print DO Customer is required for receipt type " + rcptType);
			}
		}
	}

	private void validatePrintStatus(ArShReceiptHdr existingReceipt) {
		if (existingReceipt.getPrintStatus() != null && 
			existingReceipt.getPrintStatus().equalsIgnoreCase("Y")) {
			throw new ValidationException(ValidationMessages.RECEIPT_PRINTED);
		}
	}

	private void validateFinancialYearAndPeriod(Long companyPoid, LocalDate transactionDate) {
		if (companyPoid == null || transactionDate == null) {
			return;
		}

		// Validate financial year
		String fyResult = procRepository.validateFinancialYear(UserContext.getCompanyPoid(), transactionDate);
		if (fyResult == null || !fyResult.equalsIgnoreCase("TRUE")) {
			throw new ValidationException(
				"Financial year is not valid for transaction date " + transactionDate);
		}

		// Validate transaction period
		String tpResult = procRepository.validateTransactionPeriod(companyPoid, transactionDate);
		if (tpResult == null || !tpResult.equalsIgnoreCase("TRUE")) {
			throw new ValidationException(
				"Transaction period is not valid for transaction date " + transactionDate);
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

	private void validatePaymentMethods(List<ReceiptPaymentDetailDto> payments) {
		if (payments == null || payments.isEmpty()) {
			return;
		}

		for (ReceiptPaymentDetailDto payment : payments) {
			if (payment.getPymtType() == null || payment.getPymtType().trim().isEmpty()) {
				continue;
			}

			String pymtType = payment.getPymtType().toUpperCase();
			if (pymtType.contains("CHEQUE")) {
				validateChequePayment(payment);
			} else if (pymtType.contains("TT")) {
				validateTTPayment(payment);
			} else if (pymtType.contains("ROUNDOFF")) {
				validateRoundoffPayment(payment);
			}
		}
	}

	private void validateChequePayment(ReceiptPaymentDetailDto payment) {
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
	}

	private void validateTTPayment(ReceiptPaymentDetailDto payment) {
		if (payment.getAmount() == null || payment.getTtBankPoid() == null) {
			throw new ValidationException(ValidationMessages.TT_FIELDS_MISSING);
		}
	}

	private void validateRoundoffPayment(ReceiptPaymentDetailDto payment) {
		if (payment.getAmount() == null) {
			return;
		}

		BigDecimal roundLimitValue = new BigDecimal("100");
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
			double cashAmountDouble = cashAmount.doubleValue();

			int intAmount = (int) cashAmountDouble;
			double fraction = cashAmountDouble - intAmount;
			int cents = (int) ((fraction * 1000));

			if ((cents % 5) != 0) {
				throw new ValidationException(
					ValidationMessages.CASH_ROUNDING_INVALID.replace("{0}", cashAmount.toString())
				);
			}
		}
	}

	private void validateSplitPayments(List<ReceiptPaymentDetailDto> payments) {
		int nonCashPaymentCount = 0;

		for (ReceiptPaymentDetailDto payment : payments) {
			if (payment.getPymtType() != null && !payment.getPymtType().equalsIgnoreCase("CASH")) {
				if (payment.getAmount() != null && payment.getAmount().doubleValue() > 0) {
					nonCashPaymentCount++;
				}
			}
		}

		if (nonCashPaymentCount > 1) {
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

	private void validateCharges(List<ReceiptCharges> charges) {
		if (charges == null || charges.isEmpty()) {
			return;
		}

		for (int i = 0; i < charges.size(); i++) {
			ReceiptCharges charge = charges.get(i);
			
			if (charge.getAmount() != null && charge.getAmount().compareTo(BigDecimal.ZERO) < 0) {
				throw new ValidationException(
					ValidationMessages.CHARGE_AMOUNT_NEGATIVE.replace("{0}", String.valueOf(i + 1))
				);
			}

			if (charge.getTaxAmount() != null && charge.getTaxAmount().compareTo(BigDecimal.ZERO) < 0) {
				throw new ValidationException(
					ValidationMessages.CHARGE_TAX_NEGATIVE.replace("{0}", String.valueOf(i + 1))
				);
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


	}

