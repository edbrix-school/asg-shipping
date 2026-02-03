package com.asg.shipping.receipts.util;

public class ValidationMessages {
	
	// UPDATE-specific messages
	public static final String RECEIPT_PRINTED = "Receipt has been printed and cannot be modified. Please contact administrator if changes are required.";
	
	// Release Type messages
	public static final String RELEASE_TYPE_NOT_SELECTED = "Release Type must be selected before proceeding.";
	public static final String RELEASE_TYPE_MISMATCH = "Release Type does not match the original release type. Please verify and correct.";
	public static final String ORIGINAL_RELEASE_TYPE_MISSING = "Original Release Type is missing. Cannot validate release type matching.";
	
	// DO Release Customer messages
	public static final String DO_RELEASE_CUSTOMER_NOT_SELECTED = "DO Release Customer must be selected before proceeding.";
	
	// Transaction Date messages
	public static final String TRANSACTION_DATE_CANNOT_CHANGE = "Transaction date cannot be modified. Original date must be preserved during update.";
	
	// Payment validation messages
	public static final String PAYMENT_REQUIRED = "At least one payment method must be provided.";
	public static final String RECEIPT_AMOUNT_INVALID = "Receipt amount must be greater than zero.";
	
	// Cheque payment messages
	public static final String CHEQUE_FIELDS_MISSING = "Cheque payment is incomplete. Please provide: Amount, Cheque Number, Account Number, Bank, and Cheque Date.";
	public static final String PDC_DATE_INVALID = "Cheque date must be greater than or equal to {0}. Please enter a valid cheque date.";
	
	// TT payment messages
	public static final String TT_FIELDS_MISSING = "TT payment is incomplete. Please provide: Amount and Bank.";
	
	// Roundoff payment messages
	public static final String ROUNDOFF_LIMIT_EXCEEDED = "Roundoff amount exceeds maximum limit of {0}. Please enter a valid amount.";
	
	// Cash payment messages
	public static final String CASH_ROUNDING_INVALID = "Cash amount {0} must be rounded to the nearest 5 cents.";
	
	// Split payment messages
	public static final String SPLIT_PAYMENT_NOT_ALLOWED = "Multiple payment methods cannot be combined. Please use a single payment method.";
	
	// Blacklist messages
	public static final String CUSTOMER_BLACKLISTED = "Customer with Account Number {0} is blacklisted and cannot process payments. Please contact administrator.";
	
	// Demurrage messages

	// Charge messages
	public static final String CHARGE_AMOUNT_NEGATIVE = "Charge amount at row {0} cannot be negative. Please enter a valid amount.";
	public static final String CHARGE_TAX_NEGATIVE = "Tax amount at row {0} cannot be negative. Please enter a valid amount.";
}
