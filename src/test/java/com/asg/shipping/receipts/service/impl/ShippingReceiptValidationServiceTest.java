package com.asg.shipping.receipts.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
 class ShippingReceiptValidationServiceTest {

        @Mock
        private ShipReceiptProcRepository procRepository;

        @Mock
        private ShipBlManifestHdrRepository manifestHdrRepository;

        @InjectMocks
        private ShippingReceiptValidationService validationService;

        private ReceiptsCreateDto createDto;
        private ReceiptsUpdateDto updateDto;
        private ArShReceiptHdr existingReceipt;

        @BeforeEach
        void setUp() {
                createDto = ReceiptsCreateDto.builder()
                                .blPoid(1001L)
                                .token(12345L)
                                .companyPoid(100L)
                                .transactionDate(LocalDate.now())
                                .printDoCustomerPoid(5001L)
                                .charges(Collections.singletonList(ReceiptCharges.builder()
                                                .amount(new BigDecimal("100.00"))
                                                .amountSelect("Y")
                                                .build()))
                                .paymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                                .pymtType("CASH")
                                                .amount(new BigDecimal("100.00"))
                                                .build()))
                                .build();

                updateDto = ReceiptsUpdateDto.builder()
                                .blPoid(1001L)
                                .token(12345L)
                                .companyPoid(100L)
                                .transactionDate(LocalDate.now())
                                .charges(Collections.singletonList(ReceiptCharges.builder()
                                                .amount(new BigDecimal("100.00"))
                                                .amountSelect("Y")
                                                .build()))
                                .paymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                                .pymtType("CASH")
                                                .amount(new BigDecimal("100.00"))
                                                .build()))
                                .build();

                existingReceipt = new ArShReceiptHdr();
                existingReceipt.setBlPoid(1001L);
                existingReceipt.setTransactionDate(LocalDate.now());
                existingReceipt.setPrintStatus("N");
                existingReceipt.setBlReleaseTypeOffice("ORIGINAL");
                existingReceipt.setOrignalBlReleaseType("ORIGINAL");
        }

        @Test
        void validateReceiptCreation_Success() {
                try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
                        mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(100L);
                        when(manifestHdrRepository.existsById(1001L)).thenReturn(true);
                        assertDoesNotThrow(() -> validationService.validateReceiptCreation(createDto));
                }
        }

        @Test
        void validateReceiptCreation_MissingBlPoid() {
                createDto.setBlPoid(null);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateReceiptCreation_AmountMismatch() {
                createDto.setPaymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                .pymtType("CASH")
                                .amount(new BigDecimal("200.00"))
                                .build()));

                when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateReceiptUpdate_Success() {
                try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
                        mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(100L);
                        when(manifestHdrRepository.existsById(1001L)).thenReturn(true);

                        assertDoesNotThrow(() -> validationService.validateReceiptUpdate(updateDto, existingReceipt));
                }
        }

        @Test
        void validatePrintStatus_Fail() {
                existingReceipt.setPrintStatus("Y");
                when(manifestHdrRepository.existsById(1001L)).thenReturn(true);
                ValidationException exception = assertThrows(ValidationException.class,
                                () -> validationService.validateReceiptUpdate(updateDto, existingReceipt));
                assertEquals(ValidationMessages.RECEIPT_PRINTED, exception.getMessage());
        }

        @Test
        void validateChequePayment_MissingFields() {
                createDto.setPaymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                .pymtType("CHEQUE")
                                .amount(new BigDecimal("100.00"))
                                .build()));
                createDto.setCharges(Collections.singletonList(ReceiptCharges.builder()
                                .amount(new BigDecimal("100.00"))
                                .amountSelect("Y")
                                .build()));

                when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateCashRounding_Invalid() {
                // In 3nd decimal system (0.005 rounding), 100.033 is invalid (100033 % 5 != 0)
                createDto.setPaymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                .pymtType("CASH")
                                .amount(new BigDecimal("100.033"))
                                .build()));
                createDto.setCharges(Collections.singletonList(ReceiptCharges.builder()
                                .amount(new BigDecimal("100.033"))
                                .amountSelect("Y")
                                .build()));

                when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateBlacklistedCustomer_Fail() {
                createDto.setPaymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                .pymtType("CHEQUE")
                                .amount(new BigDecimal("100.00"))
                                .chqCardno("123")
                                .accountNo("ACC123")
                                .bankPoid(1L)
                                .chqDate(LocalDate.now())
                                .build()));
                createDto.setCharges(Collections.singletonList(ReceiptCharges.builder()
                                .amount(new BigDecimal("100.00"))
                                .amountSelect("Y")
                                .build()));

                when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                when(procRepository.validateBlacklistedCustomer(eq("ACC123"), eq(1L))).thenReturn("Y");
                
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateDemurrageAmounts_Mismatch() {
                createDto.setCharges(Collections.singletonList(ReceiptCharges.builder()
                                .amount(new BigDecimal("100.00"))
                                .chargePoid(94L)
                                .amountSelect("Y")
                                .build()));
                createDto.setContainer(Collections.singletonList(ReceiptContainerDto.builder()
                                .dmChargeAmt(new BigDecimal("150.00"))
                                .build()));
                createDto.setPaymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                .pymtType("CASH")
                                .amount(new BigDecimal("100.00"))
                                .build()));

                when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                when(procRepository.getDemurrageChargePoid()).thenReturn(94L);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validatePrintCustomer_Required() {
                createDto.setPrintDoCustomerPoid(null);
                Mockito.lenient().when(procRepository.getReceiptType(anyLong())).thenReturn("NORMAL");
                Mockito.lenient().when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }


        @Test
        void validateReceiptAmount_Zero() {
                createDto.setCharges(null);
                createDto.setPaymentDetail(null);
                createDto.setContainer(null);
                Mockito.lenient().when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateTTPayment_MissingFields() {
                createDto.setPaymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                .pymtType("TT")
                                .amount(new BigDecimal("100.00"))
                                // ttBankPoid is missing
                                .build()));
                createDto.setCharges(Collections.singletonList(ReceiptCharges.builder()
                                .amount(new BigDecimal("100.00"))
                                .amountSelect("Y")
                                .build()));
                Mockito.lenient().when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateRoundoffPayment_LimitExceeded() {
                createDto.setPaymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                .pymtType("ROUNDOFF")
                                .amount(new BigDecimal("101.00"))
                                .build()));
                createDto.setCharges(Collections.singletonList(ReceiptCharges.builder()
                                .amount(new BigDecimal("101.00"))
                                .amountSelect("Y")
                                .build()));
                Mockito.lenient().when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateSplitPayments_Fail() {
                createDto.setPaymentDetail(List.of(
                                ReceiptPaymentDetailDto.builder().pymtType("CHEQUE").amount(new BigDecimal("50.00"))
                                                .chqCardno("123").accountNo("ACC123").bankPoid(1L)
                                                .chqDate(LocalDate.now()).build(),
                                ReceiptPaymentDetailDto.builder().pymtType("TT").amount(new BigDecimal("50.00"))
                                                .ttBankPoid(1L).build()));
                createDto.setCharges(Collections.singletonList(ReceiptCharges.builder()
                                .amount(new BigDecimal("100.00"))
                                .amountSelect("Y")
                                .build()));
                Mockito.lenient().when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateDemurrageAmount_ContainerFail() {
                createDto.setContainer(Collections.singletonList(ReceiptContainerDto.builder()
                                .containerNo("CONT123")
                                .dmChargeAmt(new BigDecimal("100.00"))
                                .build()));
                createDto.setCharges(Collections.singletonList(ReceiptCharges.builder()
                                .amount(new BigDecimal("100.00"))
                                .amountSelect("Y")
                                .build()));
                createDto.setPaymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                .pymtType("CASH")
                                .amount(new BigDecimal("100.00"))
                                .build()));

                Mockito.lenient().when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                Mockito.lenient().when(procRepository.getReceiptType(anyLong())).thenReturn("FINALSPLITSRECEIPT");
                Mockito.lenient().when(procRepository.validateFinancialYear(anyLong(), any(LocalDate.class)))
                                .thenReturn("TRUE");
                Mockito.lenient().when(procRepository.validateTransactionPeriod(anyLong(), any(LocalDate.class)))
                                .thenReturn("TRUE");
                Mockito.lenient()
                                .when(procRepository.validateDemurrageAmount(anyLong(), anyString(),
                                                any(BigDecimal.class)))
                                .thenReturn(new BigDecimal("-1"));

                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateCharges_Negative() {
                createDto.setCharges(Collections.singletonList(ReceiptCharges.builder()
                                .amount(new BigDecimal("-10.00"))
                                .amountSelect("Y")
                                .build()));
                createDto.setPaymentDetail(Collections.singletonList(ReceiptPaymentDetailDto.builder()
                                .pymtType("CASH")
                                .amount(new BigDecimal("-10.00"))
                                .build()));
                Mockito.lenient().when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);
                assertThrows(ValidationException.class, () -> validationService.validateReceiptCreation(createDto));
        }

        @Test
        void validateReleaseType_Mismatch() {
                existingReceipt.setBlReleaseTypeOffice("OFFICE1");
                existingReceipt.setOrignalBlReleaseType("OFFICE2");
                Mockito.lenient().when(manifestHdrRepository.existsById(anyLong())).thenReturn(true);

                assertThrows(ValidationException.class,
                                () -> validationService.validateReceiptUpdate(updateDto, existingReceipt));
        }
}
