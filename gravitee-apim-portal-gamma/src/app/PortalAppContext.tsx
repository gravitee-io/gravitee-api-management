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
import { createContext, useContext, useMemo, type ReactNode } from 'react';

export interface PortalAppContextValue {
    readonly embeddedInConsole: boolean;
    /**
     * Base URL for the standalone editor app. In console embed mode this is a same-origin path
     * (e.g. `/portal-editor`) proxied to portal-gamma so IndexedDB is shared with the dashboard.
     */
    readonly standaloneEditorBaseUrl: string;
}

const defaultValue: PortalAppContextValue = {
    embeddedInConsole: false,
    standaloneEditorBaseUrl: '',
};

const PortalAppContext = createContext<PortalAppContextValue>(defaultValue);

export function PortalAppProvider({
    children,
    embeddedInConsole = false,
    standaloneEditorBaseUrl = '',
}: {
    readonly children: ReactNode;
    readonly embeddedInConsole?: boolean;
    readonly standaloneEditorBaseUrl?: string;
}) {
    const value = useMemo(
        () => ({
            embeddedInConsole,
            standaloneEditorBaseUrl: standaloneEditorBaseUrl.replace(/\/$/, ''),
        }),
        [embeddedInConsole, standaloneEditorBaseUrl],
    );

    return <PortalAppContext.Provider value={value}>{children}</PortalAppContext.Provider>;
}

export function usePortalApp(): PortalAppContextValue {
    return useContext(PortalAppContext);
}

export function buildStandalonePortalUrl(baseUrl: string, path: string): string {
    if (!baseUrl) {
        return path;
    }

    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    return `${baseUrl.replace(/\/$/, '')}${normalizedPath}`;
}
