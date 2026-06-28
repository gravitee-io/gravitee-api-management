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

import type { PortalNavigationApi, PortalNavigationItem } from '../../portals/types';
import { findApiAncestor } from '../utils/find-api-ancestor';

export interface PortalPageContextValue {
    portalId: string;
    selectedNavItemId: string | null;
    navItems: readonly PortalNavigationItem[];
    apiNavItem: PortalNavigationApi | null;
    savePage?: () => Promise<void>;
}

const PortalPageContext = createContext<PortalPageContextValue | null>(null);

interface PortalPageProviderProps {
    readonly portalId: string;
    readonly selectedNavItemId: string | null;
    readonly navItems: readonly PortalNavigationItem[];
    readonly savePage?: () => Promise<void>;
    readonly children: ReactNode;
}

export function PortalPageProvider({
    portalId,
    selectedNavItemId,
    navItems,
    savePage,
    children,
}: PortalPageProviderProps) {
    const value = useMemo<PortalPageContextValue>(
        () => ({
            portalId,
            selectedNavItemId,
            navItems,
            apiNavItem: findApiAncestor(navItems, selectedNavItemId),
            savePage,
        }),
        [portalId, selectedNavItemId, navItems, savePage],
    );

    return <PortalPageContext.Provider value={value}>{children}</PortalPageContext.Provider>;
}

export function usePortalPage(): PortalPageContextValue {
    const context = useContext(PortalPageContext);
    if (!context) {
        throw new Error('usePortalPage must be used within PortalPageProvider');
    }
    return context;
}

export function usePortalPageOptional(): PortalPageContextValue | null {
    return useContext(PortalPageContext);
}
