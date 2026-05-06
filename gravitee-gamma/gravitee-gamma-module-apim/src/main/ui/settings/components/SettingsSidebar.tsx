/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ContextSidebar, ContextToggleButton, useLayoutConfig, useLayoutSlots } from '@gravitee/graphene-core';
import type { NavGroup } from '@gravitee/graphene-core';
import { useCallback, useEffect, useMemo, useRef } from 'react';

import { useSettingsContext } from '../context/SettingsContext';
import { useSettingsPermissions } from '../hooks/useSettingsPermissions';

export function SettingsSidebar() {
    const { state, resetSelection } = useSettingsContext();
    const { visibleGroups, getFirstAccessibleEnvForItem, userEnvironments } = useSettingsPermissions();
    const { slots, setSlots } = useLayoutSlots();
    const contextExpandedRef = useRef(slots.contextExpanded);
    contextExpandedRef.current = slots.contextExpanded;

    const allVisibleItems = useMemo(() => visibleGroups.flatMap(g => g.items), [visibleGroups]);

    useEffect(() => {
        setSlots({ contextExpanded: true });
    }, [setSlots]);

    const toggleExpanded = useCallback(() => {
        setSlots({ contextExpanded: !contextExpandedRef.current });
    }, [setSlots]);

    useEffect(() => {
        if (allVisibleItems.length > 0 && !allVisibleItems.some(i => i.key === state.selectedItemKey)) {
            const first = allVisibleItems[0]!;
            const envId =
                first.scope === 'env' ? (getFirstAccessibleEnvForItem(first.key) ?? userEnvironments[0]?.id ?? null) : null;
            resetSelection(first.key, envId);
        }
    }, [allVisibleItems, state.selectedItemKey, resetSelection, getFirstAccessibleEnvForItem, userEnvironments]);

    const handleSelect = useCallback(
        (key: string) => {
            const item = allVisibleItems.find(i => i.key === key);
            const envId =
                item?.scope === 'env' ? (getFirstAccessibleEnvForItem(key) ?? userEnvironments[0]?.id ?? null) : null;
            resetSelection(key, envId);
        },
        [allVisibleItems, resetSelection, getFirstAccessibleEnvForItem, userEnvironments],
    );

    const navGroups: NavGroup[] = useMemo(
        () =>
            visibleGroups.map(g => ({
                label: g.label,
                items: g.items.map(i => ({ key: i.key, title: i.label })),
            })),
        [visibleGroups],
    );

    useLayoutConfig(
        {
            contextSidebar: (
                <ContextSidebar groups={navGroups} activeItemKey={state.selectedItemKey} onItemSelect={handleSelect} />
            ),
            viewMode: 'context',
            leading: <ContextToggleButton expanded={slots.contextExpanded} onToggle={toggleExpanded} />,
        },
        [navGroups, state.selectedItemKey, handleSelect, toggleExpanded, slots.contextExpanded],
    );

    return null;
}
