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
import { useCallback } from 'react';

import type { UserMenuItem } from '../../portals/types';

function createMenuItemId(): string {
    return `menu-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 9)}`;
}

export interface UseUserMenuResult {
    readonly items: readonly UserMenuItem[];
    addPageItem: (page: Pick<UserMenuItem, 'label' | 'url'>) => void;
    updateItem: (id: string, patch: Partial<Pick<UserMenuItem, 'label' | 'url'>>) => void;
    removeItem: (id: string) => void;
}

export function useUserMenu(
    items: readonly UserMenuItem[],
    onChange: (items: UserMenuItem[]) => void,
): UseUserMenuResult {
    const addPageItem = useCallback(
        (page: Pick<UserMenuItem, 'label' | 'url'>) => {
            onChange([
                ...items,
                {
                    id: createMenuItemId(),
                    label: page.label,
                    url: page.url,
                },
            ]);
        },
        [items, onChange],
    );

    const updateItem = useCallback(
        (id: string, patch: Partial<Pick<UserMenuItem, 'label' | 'url'>>) => {
            onChange(items.map(item => (item.id === id ? { ...item, ...patch } : item)));
        },
        [items, onChange],
    );

    const removeItem = useCallback(
        (id: string) => {
            onChange(items.filter(item => item.id !== id));
        },
        [items, onChange],
    );

    return {
        items,
        addPageItem,
        updateItem,
        removeItem,
    };
}
