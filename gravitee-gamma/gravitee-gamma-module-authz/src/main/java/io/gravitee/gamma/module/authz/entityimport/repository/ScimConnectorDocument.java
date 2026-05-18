package io.gravitee.gamma.module.authz.entityimport.repository;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "gamma_scim_connectors")
public class ScimConnectorDocument {

    @Id
    private String id;
    private String environmentId;
    private String name;
    private String url;

    /**
     * SCIM bearer token used to authenticate against the upstream IdP.
     *
     * <p>TODO(authz-scim-token): encrypt at rest. Currently persisted as
     * plaintext in {@code gamma_scim_connectors.token}. Should be encrypted
     * via the platform's {@code DataEncryptor} adapter (see how APIM stores
     * API keys / OAuth client secrets) before save and decrypted on load so
     * a Mongo dump never exposes live IdP credentials.
     */
    private String token;

    private boolean importUsers;
    private boolean importGroups;
    private Instant lastSyncAt;
    private String lastSyncStatus;
    private String lastError;
    private int lastUsersSynced;
    private int lastGroupsSynced;
    private int lastDeleted;
    private Instant createdAt;
    private Instant updatedAt;

}
