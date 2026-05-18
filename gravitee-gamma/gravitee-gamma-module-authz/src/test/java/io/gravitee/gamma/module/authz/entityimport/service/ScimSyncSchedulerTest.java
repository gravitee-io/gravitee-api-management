package io.gravitee.gamma.module.authz.entityimport.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorDocument;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ScimSyncScheduler}.
 *
 * <p>The scheduler itself is tiny — its only job is to walk every registered
 * connector and ask the service to sync. The behaviour worth pinning down is:
 *
 * <ul>
 *   <li>Every connector returned by {@code findAllForScheduler} gets a sync call.</li>
 *   <li>A failure on one connector does NOT short-circuit the loop — the next
 *       connector still gets its turn. (If we ever lose this, a single broken
 *       SCIM endpoint silently freezes every other tenant in the env.)</li>
 *   <li>An empty connector list is a no-op — no spurious calls.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ScimSyncSchedulerTest {

    @Mock
    private ScimConnectorService service;

    @InjectMocks
    private ScimSyncScheduler scheduler;

    private static ScimConnectorDocument connector(String id, String name, String envId) {
        ScimConnectorDocument d = new ScimConnectorDocument();
        d.setId(id);
        d.setName(name);
        d.setEnvironmentId(envId);
        return d;
    }

    @Test
    void syncAll_isNoOp_whenNoConnectors() {
        when(service.findAllForScheduler()).thenReturn(List.of());

        scheduler.syncAll();

        verify(service).findAllForScheduler();
        verify(service, times(0)).runScheduledSync(any());
    }

    @Test
    void syncAll_callsRunScheduledSync_perConnector() {
        ScimConnectorDocument a = connector("a", "okta", "env-1");
        ScimConnectorDocument b = connector("b", "azure", "env-1");
        when(service.findAllForScheduler()).thenReturn(List.of(a, b));

        scheduler.syncAll();

        verify(service).runScheduledSync(a);
        verify(service).runScheduledSync(b);
    }

    @Test
    void syncAll_continuesLoop_whenOneConnectorThrows() {
        // First connector blows up — second one must still be synced. Production
        // SCIM endpoints fail individually all the time (auth rotated, DNS
        // glitch, …) and a per-iteration failure cannot poison the rest.
        ScimConnectorDocument broken = connector("a", "okta", "env-1");
        ScimConnectorDocument healthy = connector("b", "azure", "env-1");
        when(service.findAllForScheduler()).thenReturn(List.of(broken, healthy));
        doThrow(new RuntimeException("connection refused")).doNothing().when(service).runScheduledSync(any());

        scheduler.syncAll();

        verify(service).runScheduledSync(broken);
        verify(service).runScheduledSync(healthy);
    }

    @Test
    void syncAll_callsServiceInOrder() {
        ScimConnectorDocument first = connector("a", "okta", "env-1");
        ScimConnectorDocument second = connector("b", "azure", "env-1");
        when(service.findAllForScheduler()).thenReturn(List.of(first, second));
        doNothing().when(service).runScheduledSync(any());

        scheduler.syncAll();

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(service);
        inOrder.verify(service).findAllForScheduler();
        inOrder.verify(service).runScheduledSync(first);
        inOrder.verify(service).runScheduledSync(second);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void syncAll_doesNotTouchServiceWhenSchedulerIsNotInvoked() {
        // Sanity guard: we should not be calling any service method from
        // construction alone (no @PostConstruct sync, no implicit start).
        verifyNoInteractions(service);
    }
}
