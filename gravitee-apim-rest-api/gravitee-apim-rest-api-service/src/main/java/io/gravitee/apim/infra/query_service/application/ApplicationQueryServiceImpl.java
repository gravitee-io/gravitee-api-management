package io.gravitee.apim.infra.query_service.application;

import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.apim.infra.adapter.ApplicationAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApplicationQueryServiceImpl extends AbstractService implements ApplicationQueryService {

    private final ApplicationRepository applicationRepository;

    public ApplicationQueryServiceImpl(@Lazy ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    public Set<BaseApplicationEntity> findByEnvironment(String environmentId) {
        try {
            return applicationRepository.findAllByEnvironment(environmentId, ApplicationStatus.values())
                .stream()
                .map(ApplicationAdapter.INSTANCE::toEntity)
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            log.error("An error occurred while finding applications by environment", e);
            throw new TechnicalManagementException("An error occurred while finding applications by environment id: " + environmentId, e);
        }
    }
}
