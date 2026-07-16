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
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';

import { notify } from '../../../shared/notify/notify';
import {
    getAuthenticatedConsumer,
    loginConsumer,
    logoutConsumer,
} from '../services/consumer-auth.service';
import type { PortalConsumer } from '../types/consumer-auth.types';
import { ConsumerAuthContext, type ConsumerAuthContextValue } from '../hooks/useConsumerAuth';

interface ConsumerAuthProviderProps {
    readonly portalId: string;
    readonly consumerAuthGateEnabled: boolean;
    readonly previewMode: boolean;
    readonly children: ReactNode;
}

export function ConsumerAuthProvider({
    portalId,
    consumerAuthGateEnabled,
    previewMode,
    children,
}: ConsumerAuthProviderProps) {
    const [consumer, setConsumer] = useState<PortalConsumer | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const refreshSession = useCallback(async () => {
        setIsLoading(true);
        try {
            const authenticated = await getAuthenticatedConsumer(portalId);
            setConsumer(authenticated);
        } finally {
            setIsLoading(false);
        }
    }, [portalId]);

    useEffect(() => {
        void refreshSession();
    }, [refreshSession]);

    const login = useCallback(
        async (identifier: string, password: string) => {
            const authenticated = await loginConsumer(portalId, identifier, password);
            setConsumer(authenticated);
        },
        [portalId],
    );

    const logout = useCallback(() => {
        logoutConsumer(portalId);
        setConsumer(null);
    }, [portalId]);

    const value = useMemo<ConsumerAuthContextValue>(
        () => ({
            portalId,
            consumer,
            isAuthenticated: consumer !== null,
            isLoading,
            consumerAuthGateEnabled,
            previewMode,
            refreshSession,
            login,
            logout,
        }),
        [consumer, consumerAuthGateEnabled, isLoading, login, logout, portalId, previewMode, refreshSession],
    );

    return <ConsumerAuthContext.Provider value={value}>{children}</ConsumerAuthContext.Provider>;
}

export function notifySsoSimulated(provider: string): void {
    notify.success(`SSO simulated for POC — signed in as ${provider} user`);
}
