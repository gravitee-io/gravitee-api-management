package io.gravitee.apim.core.application.query_service;

import io.gravitee.rest.api.model.BaseApplicationEntity;
import java.util.Set;

public interface ApplicationQueryService {

    Set<BaseApplicationEntity> findByEnvironment(String environmentId);
}
