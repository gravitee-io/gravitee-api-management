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
import { createContext, useContext, useState, type ReactNode } from 'react';
import { useBootstrap } from './bootstrap-context';

interface EnvironmentState {
    organizationId: string;
    environmentId: string;
}

interface EnvironmentContextValue extends EnvironmentState {
    setEnvironment: (org: string, env: string) => void;
}

const EnvironmentContext = createContext<EnvironmentContextValue | null>(null);

export function EnvironmentProvider({ children }: { readonly children: ReactNode }) {
    const { organizationId: bootstrapOrgId } = useBootstrap();
    const [state, setState] = useState<EnvironmentState>({
        organizationId: bootstrapOrgId,
        environmentId: 'DEFAULT',
    });

    const setEnvironment = (org: string, env: string) => {
        setState({ organizationId: org, environmentId: env });
    };

    return <EnvironmentContext.Provider value={{ ...state, setEnvironment }}>{children}</EnvironmentContext.Provider>;
}

export function useEnvironment(): EnvironmentContextValue {
    const ctx = useContext(EnvironmentContext);
    if (!ctx) throw new Error('useEnvironment must be used within EnvironmentProvider');
    return ctx;
}
