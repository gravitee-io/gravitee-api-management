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

    private String token;

    private String tokenUrl;
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private String accessToken;
    private Instant accessTokenExpiresAt;

    private boolean importUsers;
    private boolean importGroups;
    private Integer intervalSeconds;
    private Instant lastSyncAt;
    private String lastSyncStatus;
    private String lastError;
    private int lastUsersSynced;
    private int lastGroupsSynced;
    private int lastDeleted;
    private Instant createdAt;
    private Instant updatedAt;
}
