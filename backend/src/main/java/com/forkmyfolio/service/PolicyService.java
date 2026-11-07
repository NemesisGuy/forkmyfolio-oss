package com.forkmyfolio.service;

import com.forkmyfolio.model.Policy;

/**
 * Service for retrieving legal policy documents.
 * Adheres to NAG-01 (DBR): Domain objects only.
 */
public interface PolicyService {

    Policy getTermsOfService();

    Policy getPrivacyPolicy();
}