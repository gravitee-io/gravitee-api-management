package io.gravitee.apim.infra.domain_service.api_key;

import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RevokeApiKeyDomainServiceImpl implements RevokeApiKeyDomainService {

    private final ApiKeyRepository apiKeyRepository;
    private final AuditDomainService auditService;

    public RevokeApiKeyDomainServiceImpl(ApiKeyRepository apiKeyRepository, AuditDomainService auditService) {
        this.apiKeyRepository = apiKeyRepository;
        this.auditService = auditService;
    }

    @Override
    public Set<ApiKey> revokeAllSubscriptionsApiKeys(
        ExecutionContext executionContext,
        String apiId,
        String subscriptionId,
        AuditActor currentUser
    ) {
        try {
            return apiKeyRepository
                .findBySubscription(subscriptionId)
                .stream()
                .filter(apiKey -> !apiKey.isRevoked() && !apiKey.isExpired())
                .map(apiKey -> {
                    var revokedApiKey = apiKey.revoke();

                    try {
                        apiKeyRepository.update(revokedApiKey);

                        createAuditLog(executionContext, apiId, currentUser, apiKey, revokedApiKey);
                        return revokedApiKey;
                    } catch (TechnicalException e) {
                        throw new TechnicalManagementException(e);
                    }
                })
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private void createAuditLog(
        ExecutionContext executionContext,
        String apiId,
        AuditActor currentUser,
        ApiKey apiKey,
        ApiKey revokedApiKey
    ) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(executionContext.getOrganizationId())
                .environmentId(executionContext.getEnvironmentId())
                .apiId(apiId)
                .event(ApiKeyAuditEvent.APIKEY_REVOKED)
                .actor(currentUser)
                .oldValue(apiKey)
                .newValue(revokedApiKey)
                .createdAt(ZonedDateTime.ofInstant(revokedApiKey.getRevokedAt().toInstant(), ZoneId.systemDefault()))
                .properties(
                    Map.of(
                        AuditProperties.API_KEY,
                        apiKey.getKey(),
                        AuditProperties.API,
                        apiId,
                        AuditProperties.APPLICATION,
                        apiKey.getApplication()
                    )
                )
                .build()
        );
    }
}
