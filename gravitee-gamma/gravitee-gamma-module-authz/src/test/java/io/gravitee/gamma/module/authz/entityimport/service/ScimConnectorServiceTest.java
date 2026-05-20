package io.gravitee.gamma.module.authz.entityimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.EntityAdminApi;
import io.gravitee.gamma.authorization.domain.Entity;
import io.gravitee.gamma.authorization.domain.EntityKind;
import io.gravitee.gamma.authorization.service.CascadeResult;
import io.gravitee.gamma.authorization.service.EntityFilter;
import io.gravitee.gamma.module.authz.entityimport.model.ScimConnectorRequest;
import io.gravitee.gamma.module.authz.entityimport.model.ScimConnectorResponse;
import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorDocument;
import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorRepository;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ScimConnectorService}.
 *
 * <p>Covers the orchestration logic: persistence rules (unique name per env,
 * not-found on update), mirror-entity sweep on delete (which must go through
 * {@link EntityAdminApi} so audit + gateway-sync events fire), and sync-status
 * persistence (OK / PARTIAL / ERROR mapping from {@link ScimSyncEngine.SyncResult}).
 *
 * <p>Repository, EntityAdminApi, sync engine and token provider are mocked — no
 * Mongo, no HTTP. The pure {@code toResponse} mapping is exercised implicitly on
 * every test that asserts a returned {@link ScimConnectorResponse}.
 *
 * <p>Methods are named {@code <category>_<scenario>} (create_, read_, update_,
 * delete_, syncNow_, …) instead of grouped via {@code @Nested}: surefire 3.5.5
 * with the current gravitee-parent inclusion pattern ({@code **\/*Test.java})
 * does not discover nested classes, so they would silently run zero tests.
 */
@ExtendWith(MockitoExtension.class)
class ScimConnectorServiceTest {

    private static final String ENV = "env-1";

    @Mock
    private ScimConnectorRepository repo;

    @Mock
    private EntityAdminApi entityApi;

    @Mock
    private ScimSyncEngine syncEngine;

    @Mock
    private ScimTokenProvider tokenProvider;

    @Mock
    private DataEncryptor dataEncryptor;

    @InjectMocks
    private ScimConnectorService service;

    @BeforeEach
    void stubEncryptor() throws GeneralSecurityException {
        lenient()
            .when(dataEncryptor.encrypt(anyString()))
            .thenAnswer(inv -> "enc:" + inv.getArgument(0));
        lenient()
            .when(dataEncryptor.decrypt(anyString()))
            .thenAnswer(inv -> {
                String s = inv.getArgument(0);
                return s.startsWith("enc:") ? s.substring(4) : s;
            });
    }

    private static ScimConnectorRequest request(String name, String url, String token, Boolean importUsers, Boolean importGroups) {
        return new ScimConnectorRequest(name, url, token, null, null, null, null, importUsers, importGroups, null);
    }

    private static ScimConnectorRequest request(
        String name,
        String url,
        String token,
        Boolean importUsers,
        Boolean importGroups,
        Integer intervalSeconds
    ) {
        return new ScimConnectorRequest(name, url, token, null, null, null, null, importUsers, importGroups, intervalSeconds);
    }

    private static ScimConnectorRequest refreshRequest(
        String name,
        String url,
        String tokenUrl,
        String clientId,
        String clientSecret,
        String refreshToken
    ) {
        return new ScimConnectorRequest(name, url, null, tokenUrl, clientId, clientSecret, refreshToken, true, true, null);
    }

    private static ScimConnectorDocument existing(String id, String name) {
        ScimConnectorDocument d = new ScimConnectorDocument();
        d.setId(id);
        d.setEnvironmentId(ENV);
        d.setName(name);
        d.setUrl("https://idp/scim");
        d.setToken("tok");
        d.setImportUsers(true);
        d.setImportGroups(true);
        d.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));
        d.setUpdatedAt(d.getCreatedAt());
        return d;
    }

    private static Entity scimEntity(String entityId, String connectorName) {
        return new Entity(
            "row-" + entityId,
            entityId,
            EntityKind.PRINCIPAL,
            Map.of("_connector", connectorName, "_kind", entityId.split("\\.")[0]),
            List.of(),
            "scim",
            ENV,
            Instant.now(),
            Instant.now()
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // create()
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void create_assignsRandomId_andStamps_createdEqualsUpdated() {
        when(repo.findByEnvAndName(ENV, "okta")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScimConnectorResponse res = service.create(ENV, request("okta", "https://idp/scim", "tok", true, true));

        ArgumentCaptor<ScimConnectorDocument> captor = ArgumentCaptor.forClass(ScimConnectorDocument.class);
        verify(repo).save(captor.capture());
        ScimConnectorDocument saved = captor.getValue();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getEnvironmentId()).isEqualTo(ENV);
        assertThat(saved.getName()).isEqualTo("okta");
        assertThat(saved.getUrl()).isEqualTo("https://idp/scim");
        assertThat(saved.getToken()).isEqualTo("enc:tok");
        assertThat(saved.isImportUsers()).isTrue();
        assertThat(saved.isImportGroups()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());

        assertThat(res.id()).isEqualTo(saved.getId());
        assertThat(res.name()).isEqualTo("okta");
        assertThat(res.environmentId()).isEqualTo(ENV);
    }

    @Test
    void create_importFlagsDefaultToTrue_whenNullInRequest() {
        when(repo.findByEnvAndName(ENV, "okta")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(ENV, request("okta", "https://idp/scim", "tok", null, null));

        ArgumentCaptor<ScimConnectorDocument> captor = ArgumentCaptor.forClass(ScimConnectorDocument.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isImportUsers()).isTrue();
        assertThat(captor.getValue().isImportGroups()).isTrue();
    }

    @Test
    void create_blankToken_isNotPersisted() {
        when(repo.findByEnvAndName(ENV, "okta")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(ENV, request("okta", "https://idp/scim", "   ", true, true));

        ArgumentCaptor<ScimConnectorDocument> captor = ArgumentCaptor.forClass(ScimConnectorDocument.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNull();
    }

    @Test
    void create_encryptsTokenBeforePersist() throws GeneralSecurityException {
        when(repo.findByEnvAndName(ENV, "okta")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(ENV, request("okta", "https://idp/scim", "plain-tok", true, true));

        verify(dataEncryptor).encrypt("plain-tok");
        ArgumentCaptor<ScimConnectorDocument> captor = ArgumentCaptor.forClass(ScimConnectorDocument.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNotEqualTo("plain-tok").startsWith("enc:");
    }

    @Test
    void create_wrapsEncryptionFailure_asIllegalState() throws GeneralSecurityException {
        when(repo.findByEnvAndName(ENV, "okta")).thenReturn(Optional.empty());
        when(dataEncryptor.encrypt("plain-tok")).thenThrow(new GeneralSecurityException("bad key"));

        assertThatThrownBy(() -> service.create(ENV, request("okta", "https://idp/scim", "plain-tok", true, true)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("encrypt SCIM token");

        verify(repo, never()).save(any());
    }

    @Test
    void create_persistsCustomInterval_whenSetInRequest() {
        when(repo.findByEnvAndName(ENV, "okta")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScimConnectorResponse res = service.create(ENV, request("okta", "https://idp/scim", "tok", true, true, 900));

        ArgumentCaptor<ScimConnectorDocument> captor = ArgumentCaptor.forClass(ScimConnectorDocument.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getIntervalSeconds()).isEqualTo(900);
        assertThat(res.intervalSeconds()).isEqualTo(900);
    }

    @Test
    void create_appliesDefaultInterval_whenNullInRequest() {
        when(repo.findByEnvAndName(ENV, "okta")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScimConnectorResponse res = service.create(ENV, request("okta", "https://idp/scim", "tok", true, true, null));

        assertThat(res.intervalSeconds()).isEqualTo(ScimConnectorService.DEFAULT_INTERVAL_SECONDS);
    }

    @Test
    void create_rejectsDuplicateName_inSameEnv() {
        when(repo.findByEnvAndName(ENV, "okta")).thenReturn(Optional.of(existing("id-1", "okta")));

        assertThatThrownBy(() -> service.create(ENV, request("okta", "https://idp/scim", "tok", true, true)))
            .isInstanceOf(AuthzValidationException.class)
            .hasMessageContaining("okta");

        verify(repo, never()).save(any());
    }

    @Test
    void create_persistsOAuth2RefreshFields_andEncryptsSecrets() {
        when(repo.findByEnvAndName(ENV, "okta")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScimConnectorResponse res = service.create(
            ENV,
            refreshRequest("okta", "https://idp/scim", "https://idp/oauth/token", "cid", "csec", "rtok")
        );

        ArgumentCaptor<ScimConnectorDocument> captor = ArgumentCaptor.forClass(ScimConnectorDocument.class);
        verify(repo).save(captor.capture());
        ScimConnectorDocument saved = captor.getValue();
        assertThat(saved.getTokenUrl()).isEqualTo("https://idp/oauth/token");
        assertThat(saved.getClientId()).isEqualTo("cid");
        assertThat(saved.getClientSecret()).isEqualTo("enc:csec");
        assertThat(saved.getRefreshToken()).isEqualTo("enc:rtok");
        // Cache cleared so the first sync after persist refreshes against the IdP.
        assertThat(saved.getAccessToken()).isNull();
        assertThat(saved.getAccessTokenExpiresAt()).isNull();

        assertThat(res.tokenUrl()).isEqualTo("https://idp/oauth/token");
        assertThat(res.clientId()).isEqualTo("cid");
    }

    // ─────────────────────────────────────────────────────────────────────
    // list() / get()
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void list_mapsAllByEnvironment() {
        when(repo.findByEnvironment(ENV)).thenReturn(List.of(existing("a", "okta"), existing("b", "azure")));

        List<ScimConnectorResponse> out = service.list(ENV);

        assertThat(out).extracting(ScimConnectorResponse::name).containsExactly("okta", "azure");
        assertThat(out).extracting(ScimConnectorResponse::id).containsExactly("a", "b");
    }

    @Test
    void get_returnsEmpty_whenMissing() {
        when(repo.findById("x", ENV)).thenReturn(Optional.empty());
        assertThat(service.get(ENV, "x")).isEmpty();
    }

    @Test
    void get_returnsMappedResponse_whenFound() {
        when(repo.findById("a", ENV)).thenReturn(Optional.of(existing("a", "okta")));

        Optional<ScimConnectorResponse> out = service.get(ENV, "a");

        assertThat(out).hasValueSatisfying(r -> {
            assertThat(r.id()).isEqualTo("a");
            assertThat(r.name()).isEqualTo("okta");
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // update()
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void update_throwsNotFound_whenIdMissing() {
        when(repo.findById("missing", ENV)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ENV, "missing", request("okta", "u", "t", true, true))).isInstanceOf(
            NotFoundException.class
        );

        verify(repo, never()).save(any());
    }

    @Test
    void update_updatesFields_andStampsUpdatedAt() {
        ScimConnectorDocument doc = existing("a", "okta");
        when(repo.findById("a", ENV)).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = doc.getUpdatedAt();
        ScimConnectorResponse out = service.update(ENV, "a", request("okta", "https://new", "new-tok", false, true));

        assertThat(doc.getUrl()).isEqualTo("https://new");
        assertThat(doc.getToken()).isEqualTo("enc:new-tok");
        assertThat(doc.isImportUsers()).isFalse();
        assertThat(doc.isImportGroups()).isTrue();
        assertThat(doc.getUpdatedAt()).isAfter(before);
        assertThat(out.url()).isEqualTo("https://new");
    }

    @Test
    void update_rotatingRefreshToken_clearsAccessTokenCache() {
        // Rotating the refresh token means any previously cached access token
        // was minted against the old refresh chain and must not be reused.
        ScimConnectorDocument doc = existing("a", "okta");
        doc.setAccessToken("enc:stale-access");
        doc.setAccessTokenExpiresAt(Instant.now().plusSeconds(3600));
        when(repo.findById("a", ENV)).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(ENV, "a", refreshRequest("okta", "https://idp/scim", "https://idp/oauth/token", "cid", "csec", "new-refresh"));

        assertThat(doc.getRefreshToken()).isEqualTo("enc:new-refresh");
        assertThat(doc.getAccessToken()).isNull();
        assertThat(doc.getAccessTokenExpiresAt()).isNull();
    }

    @Test
    void update_allowsRenameToOwnName_noDuplicateCheck() {
        // When the request name matches the existing one, the rename branch
        // is short-circuited and findByEnvAndName must not be invoked.
        ScimConnectorDocument doc = existing("a", "okta");
        when(repo.findById("a", ENV)).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(ENV, "a", request("okta", "https://new", "tok", true, true));

        verify(repo, never()).findByEnvAndName(any(), any());
    }

    @Test
    void update_rejectsRename_whenTargetNameTakenByAnotherConnector() {
        when(repo.findById("a", ENV)).thenReturn(Optional.of(existing("a", "okta")));
        when(repo.findByEnvAndName(ENV, "azure")).thenReturn(Optional.of(existing("b", "azure")));

        assertThatThrownBy(() -> service.update(ENV, "a", request("azure", "u", "t", true, true)))
            .isInstanceOf(AuthzValidationException.class)
            .hasMessageContaining("azure");

        verify(repo, never()).save(any());
    }

    @Test
    void update_allowsRename_whenTargetNameIsTheSameDocumentRow() {
        // Edge case: findByEnvAndName returns a doc with the same id —
        // that's the document being updated, not a real collision.
        ScimConnectorDocument self = existing("a", "okta");
        when(repo.findById("a", ENV)).thenReturn(Optional.of(self));
        when(repo.findByEnvAndName(ENV, "azure")).thenReturn(Optional.of(self));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScimConnectorResponse out = service.update(ENV, "a", request("azure", "u", "t", true, true));

        assertThat(out.name()).isEqualTo("azure");
    }

    // ─────────────────────────────────────────────────────────────────────
    // delete() — mirror entity sweep through EntityAdminApi
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void delete_returnsFalse_andSkipsSweep_whenIdMissing() {
        when(repo.findById("missing", ENV)).thenReturn(Optional.empty());

        boolean result = service.delete(ENV, "missing");

        assertThat(result).isFalse();
        verifyNoInteractions(entityApi);
        verify(repo, never()).deleteById(any(), any());
    }

    @Test
    void delete_sweepsScimMirrors_owningConnector_throughEntityAdminApi() {
        when(repo.findById("a", ENV)).thenReturn(Optional.of(existing("a", "okta")));
        when(repo.deleteById("a", ENV)).thenReturn(true);
        when(entityApi.find(eq(ENV), any(EntityFilter.class))).thenReturn(
            List.of(
                scimEntity("user.okta.alice", "okta"),
                scimEntity("group.okta.eng", "okta"),
                scimEntity("user.azure.bob", "azure") // owned by another connector
            )
        );

        boolean result = service.delete(ENV, "a");

        assertThat(result).isTrue();
        verify(entityApi).delete(any(AuthzCallerContext.class), eq("user.okta.alice"));
        verify(entityApi).delete(any(AuthzCallerContext.class), eq("group.okta.eng"));
        verify(entityApi, never()).delete(any(AuthzCallerContext.class), eq("user.azure.bob"));
        verify(entityApi, times(2)).delete(any(AuthzCallerContext.class), any());
        verify(repo).deleteById("a", ENV);
    }

    @Test
    void delete_usesSystemCaller_forSweep() {
        when(repo.findById("a", ENV)).thenReturn(Optional.of(existing("a", "okta")));
        when(entityApi.find(eq(ENV), any(EntityFilter.class))).thenReturn(List.of(scimEntity("user.okta.alice", "okta")));

        service.delete(ENV, "a");

        ArgumentCaptor<AuthzCallerContext> caller = ArgumentCaptor.forClass(AuthzCallerContext.class);
        verify(entityApi).delete(caller.capture(), eq("user.okta.alice"));
        assertThat(caller.getValue().isSystem()).isTrue();
        assertThat(caller.getValue().environmentId()).isEqualTo(ENV);
    }

    @Test
    void delete_scopesFindToScimSource() {
        when(repo.findById("a", ENV)).thenReturn(Optional.of(existing("a", "okta")));
        when(entityApi.find(eq(ENV), any(EntityFilter.class))).thenReturn(List.of());

        service.delete(ENV, "a");

        ArgumentCaptor<EntityFilter> filter = ArgumentCaptor.forClass(EntityFilter.class);
        verify(entityApi).find(eq(ENV), filter.capture());
        assertThat(filter.getValue().source()).isEqualTo(ScimSyncEngine.SOURCE_SCIM);
    }

    @Test
    void delete_continuesSweep_whenIndividualEntityDeleteFails() {
        when(repo.findById("a", ENV)).thenReturn(Optional.of(existing("a", "okta")));
        when(repo.deleteById("a", ENV)).thenReturn(true);
        when(entityApi.find(eq(ENV), any(EntityFilter.class))).thenReturn(
            List.of(scimEntity("user.okta.alice", "okta"), scimEntity("group.okta.eng", "okta"))
        );
        // First delete throws — sweep must absorb it and keep going.
        // EntityAdminApi.delete returns CascadeResult (non-void) so the 2nd
        // call is stubbed via doReturn rather than doNothing.
        Mockito.doThrow(new RuntimeException("boom"))
            .doReturn(new CascadeResult(List.of(), List.of()))
            .when(entityApi)
            .delete(any(AuthzCallerContext.class), any());

        boolean result = service.delete(ENV, "a");

        assertThat(result).isTrue();
        verify(entityApi).delete(any(AuthzCallerContext.class), eq("user.okta.alice"));
        verify(entityApi).delete(any(AuthzCallerContext.class), eq("group.okta.eng"));
        verify(repo).deleteById("a", ENV);
    }

    @Test
    void delete_ignoresEntitiesWithoutConnectorAttribute() {
        when(repo.findById("a", ENV)).thenReturn(Optional.of(existing("a", "okta")));
        Entity withoutConnector = new Entity(
            "id-x",
            "user.foreign.someone",
            EntityKind.PRINCIPAL,
            Map.of(),
            List.of(),
            "scim",
            ENV,
            Instant.now(),
            Instant.now()
        );
        when(entityApi.find(eq(ENV), any(EntityFilter.class))).thenReturn(List.of(withoutConnector));

        service.delete(ENV, "a");

        verify(entityApi, never()).delete(any(AuthzCallerContext.class), any());
    }

    // ─────────────────────────────────────────────────────────────────────
    // syncNow() / runScheduledSync() — SyncResult → status mapping
    // ─────────────────────────────────────────────────────────────────────

    @Test
    void syncNow_resolvesTokenViaProvider_beforePassingToEngine() {
        ScimConnectorDocument doc = existing("a", "okta");
        when(repo.findById("a", ENV)).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.resolve(doc)).thenReturn("resolved-access");
        when(syncEngine.sync(eq(doc), eq("resolved-access"))).thenReturn(new ScimSyncEngine.SyncResult());

        service.syncNow(ENV, "a");

        verify(tokenProvider).resolve(doc);
        verify(syncEngine).sync(eq(doc), eq("resolved-access"));
    }

    @Test
    void syncNow_persistsStatusError_whenTokenRefreshFails_andDoesNotInvokeEngine() {
        // Refresh failure must surface in lastSyncStatus/lastError exactly like
        // a sync-time failure would — the dashboard treats both as the same
        // "this connector is broken" signal.
        ScimConnectorDocument doc = existing("a", "okta");
        when(repo.findById("a", ENV)).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.resolve(doc)).thenThrow(new ScimTokenProvider.TokenRefreshException("Token endpoint returned HTTP 401"));

        ScimConnectorResponse out = service.syncNow(ENV, "a");

        assertThat(out.lastSyncStatus()).isEqualTo("ERROR");
        assertThat(out.lastError()).contains("Token endpoint returned HTTP 401");
        verifyNoInteractions(syncEngine);
    }

    @Test
    void syncNow_persistsStatusOk_whenNoErrorNoWarnings() {
        ScimConnectorDocument doc = existing("a", "okta");
        when(repo.findById("a", ENV)).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ScimSyncEngine.SyncResult result = new ScimSyncEngine.SyncResult();
        result.users = 5;
        result.groups = 2;
        result.deleted = 1;
        when(syncEngine.sync(eq(doc), any())).thenReturn(result);

        ScimConnectorResponse out = service.syncNow(ENV, "a");

        assertThat(out.lastSyncStatus()).isEqualTo("OK");
        assertThat(out.lastError()).isNull();
        assertThat(out.lastUsersSynced()).isEqualTo(5);
        assertThat(out.lastGroupsSynced()).isEqualTo(2);
        assertThat(out.lastDeleted()).isEqualTo(1);
    }

    @Test
    void syncNow_persistsStatusPartial_whenWarningsPresent() {
        ScimConnectorDocument doc = existing("a", "okta");
        when(repo.findById("a", ENV)).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ScimSyncEngine.SyncResult result = new ScimSyncEngine.SyncResult();
        result.warnings.add("user alice: HTTP 500");
        result.warnings.add("user bob: invalid");
        when(syncEngine.sync(eq(doc), any())).thenReturn(result);

        ScimConnectorResponse out = service.syncNow(ENV, "a");

        assertThat(out.lastSyncStatus()).isEqualTo("PARTIAL");
        assertThat(out.lastError()).contains("user alice: HTTP 500").contains("user bob: invalid");
    }

    @Test
    void syncNow_persistsStatusError_whenEngineReportsError() {
        ScimConnectorDocument doc = existing("a", "okta");
        when(repo.findById("a", ENV)).thenReturn(Optional.of(doc));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ScimSyncEngine.SyncResult result = new ScimSyncEngine.SyncResult();
        result.error = "Failed to fetch /Users: connection refused";
        result.warnings.add("group eng: HTTP 503"); // ignored when error is set
        when(syncEngine.sync(eq(doc), any())).thenReturn(result);

        ScimConnectorResponse out = service.syncNow(ENV, "a");

        assertThat(out.lastSyncStatus()).isEqualTo("ERROR");
        assertThat(out.lastError()).isEqualTo("Failed to fetch /Users: connection refused");
    }

    @Test
    void syncNow_throwsNotFound_whenConnectorMissing() {
        when(repo.findById("missing", ENV)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.syncNow(ENV, "missing")).isInstanceOf(NotFoundException.class);

        verifyNoInteractions(syncEngine);
    }

    @Test
    void runScheduledSync_persistsResult_withoutRefetch() {
        ScimConnectorDocument doc = existing("a", "okta");
        ScimSyncEngine.SyncResult result = new ScimSyncEngine.SyncResult();
        result.users = 1;
        when(syncEngine.sync(eq(doc), any())).thenReturn(result);

        service.runScheduledSync(doc);

        verify(repo, never()).findById(any(), any());
        verify(repo).save(doc);
        assertThat(doc.getLastSyncStatus()).isEqualTo("OK");
        assertThat(doc.getLastUsersSynced()).isEqualTo(1);
    }

    @Test
    void findAllForScheduler_delegatesToRepo() {
        ScimConnectorDocument a = existing("a", "okta");
        ScimConnectorDocument b = existing("b", "azure");
        when(repo.findAll()).thenReturn(List.of(a, b));

        assertThat(service.findAllForScheduler()).containsExactly(a, b);
    }
}
