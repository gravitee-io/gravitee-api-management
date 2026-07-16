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
import { createContext, useContext } from 'react';

import type { PortalConsumer } from '../types/consumer-auth.types';

export interface ConsumerAuthContextValue {
    readonly portalId: string;
    readonly consumer: PortalConsumer | null;
    readonly isAuthenticated: boolean;
    readonly isLoading: boolean;
    readonly consumerAuthGateEnabled: boolean;
    readonly previewMode: boolean;
    readonly refreshSession: () => Promise<void>;
    readonly login: (identifier: string, password: string) => Promise<void>;
    readonly logout: () => void;
}

export const ConsumerAuthContext = createContext<ConsumerAuthContextValue | null>(null);

export function useConsumerAuth(): ConsumerAuthContextValue {
    const context = useContext(ConsumerAuthContext);
    if (!context) {
        throw new Error('useConsumerAuth must be used within ConsumerAuthProvider');
    }

    return context;
}

export function useOptionalConsumerAuth(): ConsumerAuthContextValue | null {
    return useContext(ConsumerAuthContext);
}
