package io.gravitee.gamma.module.authz.entityimport.service;

import io.gravitee.apim.authorization.api.AuthzCallerContext;
import io.gravitee.apim.authorization.api.EntityAdminApi;
import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.service.EntityFilter;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.gamma.module.authz.entityimport.model.ScimConnectorRequest;
import io.gravitee.gamma.module.authz.entityimport.model.ScimConnectorResponse;
import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorDocument;
import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorRepository;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScimConnectorService {

    private final ScimConnectorRepository repo;
    private final EntityAdminApi entityApi;
    private final ScimSyncEngine syncEngine;
    private final DataEncryptor dataEncryptor;

    @Autowired
    public ScimConnectorService(
        ScimConnectorRepository repo,
        EntityAdminApi entityApi,
        ScimSyncEngine syncEngine,
        DataEncryptor dataEncryptor
    ) {
        this.repo = repo;
        this.entityApi = entityApi;
        this.syncEngine = syncEngine;
        this.dataEncryptor = dataEncryptor;
    }

    public ScimConnectorResponse create(String environmentId, ScimConnectorRequest request) {
        repo
            .findByEnvAndName(environmentId, request.name())
            .ifPresent(existing -> {
                throw new AuthzValidationException("Connector '" + request.name() + "' already exists");
            });
        ScimConnectorDocument doc = new ScimConnectorDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setEnvironmentId(environmentId);
        applyRequest(doc, request);
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(doc.getCreatedAt());
        return toResponse(repo.save(doc));
    }

    public List<ScimConnectorResponse> list(String environmentId) {
        return repo.findByEnvironment(environmentId).stream().map(this::toResponse).toList();
    }

    public Optional<ScimConnectorResponse> get(String environmentId, String id) {
        return repo.findById(id, environmentId).map(this::toResponse);
    }

    public ScimConnectorResponse update(String environmentId, String id, ScimConnectorRequest request) {
        ScimConnectorDocument existing = repo
            .findById(id, environmentId)
            .orElseThrow(() -> new NotFoundException("Connector not found: " + id));
        if (!existing.getName().equals(request.name())) {
            repo
                .findByEnvAndName(environmentId, request.name())
                .ifPresent(other -> {
                    if (!other.getId().equals(id)) {
                        throw new AuthzValidationException("Connector '" + request.name() + "' already exists");
                    }
                });
        }
        applyRequest(existing, request);
        existing.setUpdatedAt(Instant.now());
        return toResponse(repo.save(existing));
    }

    /**
     * Delete the connector and sweep its mirror entities (those with source=scim and
     * matching {@code _connector} attribute). Sweep goes through {@link EntityAdminApi}
     * so each delete fires audit + gateway sync events.
     */
    public boolean delete(String environmentId, String id) {
        Optional<ScimConnectorDocument> existing = repo.findById(id, environmentId);
        if (existing.isEmpty()) return false;
        String connectorName = existing.get().getName();
        AuthzCallerContext caller = AuthzCallerContext.system(environmentId);
        List<Entity> mirrors = entityApi.find(environmentId, new EntityFilter(null, ScimSyncEngine.SOURCE_SCIM, null));
        for (Entity e : mirrors) {
            Object owner = e.attributes().get("_connector");
            if (owner != null && connectorName.equals(owner.toString())) {
                try {
                    entityApi.delete(caller, e.entityId());
                } catch (Exception ignored) {
                    // best-effort sweep
                }
            }
        }
        return repo.deleteById(id, environmentId);
    }

    /** Run sync now for one connector, persisting status fields on the document. */
    public ScimConnectorResponse syncNow(String environmentId, String id) {
        ScimConnectorDocument doc = repo.findById(id, environmentId).orElseThrow(() -> new NotFoundException("Connector not found: " + id));
        applySyncResult(doc, syncEngine.sync(doc, decryptToken(doc.getToken())));
        return toResponse(repo.save(doc));
    }

    /** Called by the scheduler — runs sync against an already-loaded document. */
    public void runScheduledSync(ScimConnectorDocument doc) {
        applySyncResult(doc, syncEngine.sync(doc, decryptToken(doc.getToken())));
        repo.save(doc);
    }

    public List<ScimConnectorDocument> findAllForScheduler() {
        return repo.findAll();
    }

    private void applySyncResult(ScimConnectorDocument doc, ScimSyncEngine.SyncResult r) {
        doc.setLastSyncAt(Instant.now());
        doc.setLastUsersSynced(r.users);
        doc.setLastGroupsSynced(r.groups);
        doc.setLastDeleted(r.deleted);
        if (r.error != null) {
            doc.setLastSyncStatus("ERROR");
            doc.setLastError(r.error);
        } else if (!r.warnings.isEmpty()) {
            doc.setLastSyncStatus("PARTIAL");
            doc.setLastError(String.join("; ", r.warnings));
        } else {
            doc.setLastSyncStatus("OK");
            doc.setLastError(null);
        }
        doc.setUpdatedAt(Instant.now());
    }

    private void applyRequest(ScimConnectorDocument doc, ScimConnectorRequest req) {
        doc.setName(req.name());
        doc.setUrl(req.url());
        if (req.token() != null && !req.token().isBlank()) {
            doc.setToken(encryptToken(req.token()));
        }
        doc.setImportUsers(req.importUsers() == null || req.importUsers());
        doc.setImportGroups(req.importGroups() == null || req.importGroups());
    }

    private String encryptToken(String plaintext) {
        try {
            return dataEncryptor.encrypt(plaintext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt SCIM token", e);
        }
    }

    private String decryptToken(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) return null;
        try {
            return dataEncryptor.decrypt(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt SCIM token", e);
        }
    }

    private ScimConnectorResponse toResponse(ScimConnectorDocument d) {
        return new ScimConnectorResponse(
            d.getId(),
            d.getEnvironmentId(),
            d.getName(),
            d.getUrl(),
            d.isImportUsers(),
            d.isImportGroups(),
            d.getLastSyncAt(),
            d.getLastSyncStatus(),
            d.getLastError(),
            d.getLastUsersSynced(),
            d.getLastGroupsSynced(),
            d.getLastDeleted(),
            d.getCreatedAt(),
            d.getUpdatedAt()
        );
    }
}
