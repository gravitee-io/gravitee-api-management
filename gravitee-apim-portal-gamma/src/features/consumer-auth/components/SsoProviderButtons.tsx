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
import { Button } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import { getEnabledIdentityProvidersByPortalId } from '../../settings/storage/portal-identity-providers.storage';
import {
    PORTAL_IDP_TYPE_LABELS,
    type PortalIdentityProvider,
    type PortalIdentityProviderType,
} from '../../settings/types';
import type { ConsumerAuthProvider } from '../types/consumer-auth.types';
import styles from './SsoProviderButtons.module.scss';

const IDP_TYPE_TO_AUTH_PROVIDER: Record<PortalIdentityProviderType, Exclude<ConsumerAuthProvider, 'local'>> = {
    GOOGLE: 'google',
    GITHUB: 'github',
    GRAVITEEIO_AM: 'graviteeio_am',
    OIDC: 'oidc',
};

const IDP_TYPE_GLYPH: Record<PortalIdentityProviderType, string> = {
    GOOGLE: 'G',
    GITHUB: 'GH',
    GRAVITEEIO_AM: 'AM',
    OIDC: 'OIDC',
};

export function mapIdentityProviderToAuthProvider(
    type: PortalIdentityProviderType,
): Exclude<ConsumerAuthProvider, 'local'> {
    return IDP_TYPE_TO_AUTH_PROVIDER[type];
}

interface SsoProviderButtonsProps {
    readonly portalId: string;
    readonly disabled?: boolean;
    readonly onProviderClick: (provider: Exclude<ConsumerAuthProvider, 'local'>, label: string) => void;
}

export function SsoProviderButtons({
    portalId,
    disabled = false,
    onProviderClick,
}: SsoProviderButtonsProps) {
    const [providers, setProviders] = useState<PortalIdentityProvider[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;

        void getEnabledIdentityProvidersByPortalId(portalId)
            .then(enabled => {
                if (!cancelled) {
                    setProviders(enabled);
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [portalId]);

    if (loading || providers.length === 0) {
        return null;
    }

    return (
        <>
            <div className={styles.divider}>or continue with</div>
            <div className={styles.ssoList}>
                {providers.map(provider => {
                    const label = provider.name || PORTAL_IDP_TYPE_LABELS[provider.type];
                    const color = provider.configuration.color.trim();
                    return (
                        <Button
                            key={provider.id}
                            type="button"
                            variant="outline"
                            className={styles.ssoButton}
                            disabled={disabled}
                            onClick={() =>
                                onProviderClick(mapIdentityProviderToAuthProvider(provider.type), label)
                            }
                        >
                            <span
                                className={styles.ssoIcon}
                                aria-hidden="true"
                                style={color ? { background: color, color: '#fff' } : undefined}
                            >
                                {IDP_TYPE_GLYPH[provider.type]}
                            </span>
                            Continue with {label}
                        </Button>
                    );
                })}
            </div>
        </>
    );
}
