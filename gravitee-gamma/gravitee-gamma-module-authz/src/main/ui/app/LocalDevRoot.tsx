import { LayoutSlotsProvider } from '@gravitee/graphene-core';
import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './AppRoutes';
import { LocalDevShell } from './LocalDevShell';
import { EnvironmentProvider } from './lib/env/EnvironmentContext';
import { resolveEnvironmentId } from './lib/env/resolveEnvironmentId';

/**
 * Standalone entry only: not used when this package is loaded as a federated remote.
 * Story G1: env id resolved via <code>?env=</code> URL param (admin override) or
 * fallback to <code>'DEFAULT'</code>. Resolved once at mount; switching envs in
 * local dev requires a page reload (matches the federated host behaviour).
 */
export default function LocalDevRoot() {
    const environmentId = resolveEnvironmentId();
    return (
        <BrowserRouter>
            <LayoutSlotsProvider>
                <EnvironmentProvider environmentId={environmentId}>
                    <LocalDevShell>
                        <AppRoutes />
                    </LocalDevShell>
                </EnvironmentProvider>
            </LayoutSlotsProvider>
        </BrowserRouter>
    );
}
