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
import { createContext, use, useContext, type ReactNode } from 'react';

import { type BootstrapConfig, getBootstrapConfig } from './bootstrap.service';
import { setManagementBase } from '../auth/auth.api';

const BootstrapContext = createContext<BootstrapConfig | null>(null);

const configPromise = getBootstrapConfig();

export function BootstrapProvider({ children }: { readonly children: ReactNode }) {
    const config = use(configPromise);
    // Idempotent — safe to call on every render until the module-level state is replaced (see FIXME in auth.api.ts)
    setManagementBase(`${config.managementBaseURL}/organizations/${config.organizationId}`);
    return <BootstrapContext value={config}>{children}</BootstrapContext>;
}

export function useBootstrap(): BootstrapConfig {
    const ctx = useContext(BootstrapContext);
    if (!ctx) throw new Error('useBootstrap must be used within BootstrapProvider');
    return ctx;
}
