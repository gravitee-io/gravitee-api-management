package io.gravitee.management.service;

import io.gravitee.management.model.InstanceEntity;

import java.util.Collection;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public interface InstanceService {

    Collection<InstanceEntity> findInstances();
}
