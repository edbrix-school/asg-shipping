package com.asg.shipping.service;

public interface GlPostingService {
    String performGlPosting(String docId, Long transactionPoid, String docRef);
}

