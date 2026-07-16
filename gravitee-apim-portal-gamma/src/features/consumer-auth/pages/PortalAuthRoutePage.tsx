/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

import '../../theming/engine/graphene-bridge.css';

import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { usePortalTheme } from '../../theming/hooks/usePortalTheme';
import { useDarkMode } from '../../theming/hooks/useDarkMode';
import { useThemeInjection } from '../../theming/hooks/useThemeInjection';
import { getPortal } from '../../portals/storage/portals.storage';
import { seedCatalogDataIfEmpty } from '../../portals/storage/seed-catalog-data';
import type { DeveloperPortal } from '../../portals/types';
import { seedPortalTenantsForPortal } from '../../tenants/storage/seed-portal-tenants';
import { ConsumerAuthProvider } from '../context/ConsumerAuthProvider';
import { seedDemoConsumerForPortal } from '../storage/seed-demo-consumer';
import { getStandaloneAuthPaths } from '../utils/portal-auth-paths';
import { InviteAcceptPage } from '../components/InviteAcceptPage';
import { LoginPage } from '../components/LoginPage';
import { SignupPage } from '../components/SignupPage';

type AuthPageVariant = 'login' | 'signup' | 'invite';

interface PortalAuthRoutePageProps {
    readonly variant: AuthPageVariant;
}

function ThemedAuthShell({
    portal,
    children,
}: {
    readonly portal: DeveloperPortal;
    readonly children: React.ReactNode;
}) {
    const themeState = usePortalTheme(portal.id);
    const darkModeState = useDarkMode(themeState.theme.activeMode);
    const [shellEl, setShellEl] = useState<HTMLDivElement | null>(null);

    useThemeInjection(shellEl, themeState.theme, darkModeState.isDark, !themeState.loading);

    return (
        <div ref={setShellEl} className="min-h-screen">
            {children}
        </div>
    );
}

export function PortalAuthRoutePage({ variant }: PortalAuthRoutePageProps) {
    const { id, token } = useParams<{ id: string; token?: string }>();
    const [portal, setPortal] = useState<DeveloperPortal | undefined>();
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!id) {
            setLoading(false);
            return;
        }

        void (async () => {
            await seedCatalogDataIfEmpty();
            await seedPortalTenantsForPortal(id);
            await seedDemoConsumerForPortal(id);
            const result = await getPortal(id);
            setPortal(result);
            setLoading(false);
        })();
    }, [id]);

    if (loading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading…</p>;
    }

    if (!portal || !id) {
        return (
            <NotFoundPage
                homePath="/"
                homeLabel="Back to dashboards"
                title="Portal not found"
                description="This developer portal does not exist or may have been removed."
                className="min-h-screen"
            />
        );
    }

    const paths = getStandaloneAuthPaths(id);

    return (
        <ConsumerAuthProvider portalId={id} consumerAuthGateEnabled={false} previewMode>
            <ThemedAuthShell portal={portal}>
                {variant === 'login' && (
                    <LoginPage
                        portal={portal}
                        loginPath={paths.loginPath}
                        signupPath={paths.signupPath}
                        defaultRedirectPath={paths.defaultRedirectPath}
                    />
                )}
                {variant === 'signup' && (
                    <SignupPage
                        portal={portal}
                        loginPath={paths.loginPath}
                        defaultRedirectPath={paths.defaultRedirectPath}
                    />
                )}
                {variant === 'invite' && token && (
                    <InviteAcceptPage
                        portal={portal}
                        token={token}
                        loginPath={paths.loginPath}
                        signupPath={paths.signupPath}
                        defaultRedirectPath={paths.defaultRedirectPath}
                    />
                )}
                {variant === 'invite' && !token && (
                    <NotFoundPage
                        homePath={paths.loginPath}
                        homeLabel="Back to sign in"
                        title="Invitation not found"
                        description="This invitation link is missing a token."
                        className="min-h-screen"
                    />
                )}
            </ThemedAuthShell>
        </ConsumerAuthProvider>
    );
}
