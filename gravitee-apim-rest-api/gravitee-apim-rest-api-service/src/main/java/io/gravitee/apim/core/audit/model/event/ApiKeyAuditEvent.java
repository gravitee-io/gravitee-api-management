package io.gravitee.apim.core.audit.model.event;

public enum ApiKeyAuditEvent implements AuditEvent {
    APIKEY_CREATED,
    APIKEY_RENEWED,
    APIKEY_REVOKED,
    APIKEY_EXPIRED,
    APIKEY_REACTIVATED,
}
