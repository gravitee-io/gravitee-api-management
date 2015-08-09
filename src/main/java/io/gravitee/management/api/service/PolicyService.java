package io.gravitee.management.api.service;

import io.gravitee.management.api.model.PolicyEntity;

import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface PolicyService {

    Set<PolicyEntity> findAll();
}
