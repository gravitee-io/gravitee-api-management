import { AppRoutes } from './app/AppRoutes';
import { EnvironmentProvider } from './app/lib/env/EnvironmentContext';
import { resolveEnvironmentId } from './app/lib/env/resolveEnvironmentId';

/**
 * Module Federation entry. Mounted under the host (Gravitee Console) router and
 * shell layout. Story G1: the host MUST pass <code>environmentId</code> so the
 * module scopes every authz REST call to the env the admin is currently looking
 * at; missing prop falls back to <code>?env=</code> URL param then to
 * <code>'DEFAULT'</code> with a one-time WARN (see <code>resolveEnvironmentId</code>).
 *
 * <p>Props are intentionally narrow: just <code>environmentId</code>. The host
 * already provides router, shell, theme, and i18n at higher levels — we don't
 * re-introduce them here. Future host-injected attributes (organizationId,
 * userId, feature flags) can join this prop bag without breaking the contract.
 */
interface FederationEntryProps {
    readonly environmentId?: string;
}

export default function FederationEntry({ environmentId }: FederationEntryProps) {
    const resolved = resolveEnvironmentId({ hostProp: environmentId });
    return (
        <EnvironmentProvider environmentId={resolved}>
            <AppRoutes />
        </EnvironmentProvider>
    );
}
