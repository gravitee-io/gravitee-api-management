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
import { useCallback, useState, type ReactNode } from 'react';

import type { UsePortalThemeReturn } from '../hooks/usePortalTheme';
import { getElementDef } from '../registry/element-registry';
import { resolveCustomizeInstanceTarget } from '../utils/instance-style';
import { CustomizeStylePanel } from './CustomizeStylePanel';
import styles from './CustomizeOverlay.module.scss';

interface CustomizeTarget {
    element: HTMLElement;
    elementId: string;
    variant?: string;
    instanceId: string;
    usesBlockStorage: boolean;
}

interface CustomizeOverlayProps {
    readonly children: ReactNode;
    readonly themeState: UsePortalThemeReturn;
    readonly enabled: boolean;
    readonly editingMode?: 'light' | 'dark';
    readonly getBlockInstanceStyle?: (blockId: string) => Record<string, string>;
    readonly onBindBlockInstanceStyle?: (blockId: string, prop: string, customVarName: string) => void;
    readonly onUnbindBlockInstanceStyle?: (blockId: string, prop: string) => void;
}

export function CustomizeOverlay({
    children,
    themeState,
    enabled,
    editingMode = 'light',
    getBlockInstanceStyle,
    onBindBlockInstanceStyle,
    onUnbindBlockInstanceStyle,
}: CustomizeOverlayProps) {
    const [target, setTarget] = useState<CustomizeTarget | null>(null);
    const [panelPosition, setPanelPosition] = useState({ x: 0, y: 0 });

    const handleContextMenu = useCallback((event: React.MouseEvent) => {
        if (!enabled) return;

        const el = (event.target as HTMLElement).closest('[data-style-target]') as HTMLElement | null;
        if (!el) return;

        event.preventDefault();
        const elementId = el.getAttribute('data-style-target') ?? '';
        const variant = el.getAttribute('data-style-variant')
            ?? el.getAttribute('data-style-part')
            ?? undefined;
        if (!getElementDef(elementId)) return;

        const { instanceId, usesBlockStorage } = resolveCustomizeInstanceTarget(el, elementId);
        setTarget({ element: el, elementId, variant, instanceId, usesBlockStorage });
        setPanelPosition({ x: event.clientX, y: event.clientY });
    }, [enabled]);

    const handleClose = useCallback(() => setTarget(null), []);

    const getInstanceStyleBinding = useCallback((instanceId: string, usesBlockStorage: boolean) => {
        if (usesBlockStorage) {
            return getBlockInstanceStyle?.(instanceId) ?? {};
        }
        return themeState.getInstanceOverride(instanceId);
    }, [getBlockInstanceStyle, themeState]);

    const handleBindInstanceStyle = useCallback((
        instanceId: string,
        prop: string,
        customVarName: string,
        usesBlockStorage: boolean,
    ) => {
        if (usesBlockStorage) {
            onBindBlockInstanceStyle?.(instanceId, prop, customVarName);
            return;
        }
        themeState.bindInstanceOverride(instanceId, prop, customVarName);
    }, [onBindBlockInstanceStyle, themeState]);

    const handleUnbindInstanceStyle = useCallback((
        instanceId: string,
        prop: string,
        usesBlockStorage: boolean,
    ) => {
        if (usesBlockStorage) {
            onUnbindBlockInstanceStyle?.(instanceId, prop);
            return;
        }
        themeState.unbindInstanceOverride(instanceId, prop);
    }, [onUnbindBlockInstanceStyle, themeState]);

    return (
        <div className={styles.overlay} onContextMenu={handleContextMenu}>
            {children}
            {target && (
                <CustomizeStylePanel
                    target={target}
                    position={panelPosition}
                    themeState={themeState}
                    editingMode={editingMode}
                    getInstanceStyleBinding={getInstanceStyleBinding}
                    onBindInstanceStyle={handleBindInstanceStyle}
                    onUnbindInstanceStyle={handleUnbindInstanceStyle}
                    onClose={handleClose}
                />
            )}
        </div>
    );
}
