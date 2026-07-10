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
import { useCallback, useRef, useState } from 'react';
import { Button } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';

import type { UsePortalThemeReturn } from '../hooks/usePortalTheme';
import { getElementDef, resolvePartCssVariant, type PropertyDef } from '../registry/element-registry';
import { getFoundationKeyForProperty } from '../registry/property-mapping';
import { buildElementVarName, customVarCssName, foundationTokenToCssVar } from '../registry/var-names';
import { StyleLayerRow } from './StyleLayerRow';
import styles from './CustomizeStylePanel.module.scss';

interface CustomizeTarget {
    element: HTMLElement;
    elementId: string;
    variant?: string;
    instanceId: string;
    usesBlockStorage: boolean;
}

interface CustomizeStylePanelProps {
    readonly target: CustomizeTarget;
    readonly position: { x: number; y: number };
    readonly themeState: UsePortalThemeReturn;
    readonly editingMode: 'light' | 'dark';
    readonly getInstanceStyleBinding: (instanceId: string, usesBlockStorage: boolean) => Record<string, string>;
    readonly onBindInstanceStyle: (
        instanceId: string,
        prop: string,
        customVarName: string,
        usesBlockStorage: boolean,
    ) => void;
    readonly onUnbindInstanceStyle: (
        instanceId: string,
        prop: string,
        usesBlockStorage: boolean,
    ) => void;
    readonly onClose: () => void;
}

type ActiveTier = 'global' | 'element' | 'custom' | null;

function resolveActiveTier(
    foundationSet: boolean,
    elementSet: boolean,
    customBound: boolean,
): ActiveTier {
    if (customBound) return 'custom';
    if (elementSet) return 'element';
    if (foundationSet) return 'global';
    return null;
}

interface PropertyStyleLayersProps {
    readonly prop: string;
    readonly def: PropertyDef;
    readonly target: CustomizeTarget;
    readonly themeState: UsePortalThemeReturn;
    readonly editingMode: 'light' | 'dark';
    readonly bindingRevision: number;
    readonly getInstanceStyleBinding: (instanceId: string, usesBlockStorage: boolean) => Record<string, string>;
    readonly onBindInstanceStyle: (
        instanceId: string,
        prop: string,
        customVarName: string,
        usesBlockStorage: boolean,
    ) => void;
    readonly onUnbindInstanceStyle: (
        instanceId: string,
        prop: string,
        usesBlockStorage: boolean,
    ) => void;
}

function PropertyStyleLayers({
    prop,
    def,
    target,
    themeState,
    editingMode,
    bindingRevision,
    getInstanceStyleBinding,
    onBindInstanceStyle,
    onUnbindInstanceStyle,
}: PropertyStyleLayersProps) {
    const {
        theme,
        getResolvedFoundation,
        updateFoundationToken,
        clearFoundationToken,
        updateElementToken,
        clearElementToken,
        addCustomVariable,
        updateCustomVariable,
    } = themeState;

    const partId = target.variant ?? (getElementDef(target.elementId)?.parts ? 'default' : undefined);
    const foundationKey = getFoundationKeyForProperty(prop, def);
    const elementVarName = buildElementVarName(
        target.elementId,
        resolvePartCssVariant(partId ?? 'default'),
        prop,
    );

    const foundationExplicit = foundationKey
        ? theme.foundation[editingMode][foundationKey] !== undefined
        : false;
    const foundationValue = foundationKey
        ? getResolvedFoundation(editingMode)[foundationKey]
        : '';

    const getElementValue = (): string => {
        const entry = theme.elements[target.elementId];
        if (!entry) return '';
        if (partId) {
            const variantEntry = (entry as Record<string, { light: Record<string, string>; dark: Record<string, string> }>)[partId];
            return variantEntry?.[editingMode]?.[prop] ?? '';
        }
        const direct = entry as { light: Record<string, string>; dark: Record<string, string> };
        return direct[editingMode]?.[prop] ?? '';
    };

    const elementValue = getElementValue();
    const elementSet = elementValue !== '';

    const instanceBinding = target.usesBlockStorage
        ? getInstanceStyleBinding(target.instanceId, true)
        : (theme.instanceOverrides[target.instanceId] ?? {});
    const boundCustomVar = instanceBinding[prop];
    const customVarDef = boundCustomVar
        ? theme.customVariables.find(cv => cv.name === boundCustomVar)
        : undefined;
    const customValue = customVarDef
        ? (editingMode === 'light' ? customVarDef.lightValue : customVarDef.darkValue)
        : '';
    const customBound = Boolean(boundCustomVar);

    const activeTier = resolveActiveTier(foundationExplicit, elementSet, customBound);
    const customVarNames = theme.customVariables.map(cv => cv.name);

    const handleAddCustomVar = () => {
        const baseName = `${target.elementId}-${prop}`;
        let name = baseName;
        let suffix = 1;
        while (theme.customVariables.some(cv => cv.name === name)) {
            name = `${baseName}-${suffix}`;
            suffix += 1;
        }
        addCustomVariable({ name, lightValue: '', darkValue: '' });
        onBindInstanceStyle(target.instanceId, prop, name, target.usesBlockStorage);
    };

    const unsetTierValue = def.type === 'color' ? foundationValue : '';

    return (
        <div key={bindingRevision} className={styles.layers}>
            <p className={styles.layersTitle}>Style layers</p>
            {foundationKey && (
                <StyleLayerRow
                    label="Global"
                    varName={foundationTokenToCssVar(foundationKey)}
                    value={foundationExplicit ? foundationValue : unsetTierValue}
                    isSet={foundationExplicit}
                    isActive={activeTier === 'global'}
                    propertyDef={def}
                    property={prop}
                    onChange={value => {
                        if (value === '') {
                            clearFoundationToken(editingMode, foundationKey);
                        } else {
                            updateFoundationToken(editingMode, foundationKey, value);
                        }
                    }}
                    onClear={foundationExplicit
                        ? () => clearFoundationToken(editingMode, foundationKey)
                        : undefined}
                />
            )}
            <StyleLayerRow
                label="Element"
                varName={elementVarName}
                value={elementSet ? elementValue : ''}
                isSet={elementSet}
                isActive={activeTier === 'element'}
                propertyDef={def}
                property={prop}
                onChange={value => {
                    if (value === '') {
                        clearElementToken(target.elementId, editingMode, prop, partId);
                    } else {
                        updateElementToken(target.elementId, editingMode, prop, value, partId);
                    }
                }}
                onClear={elementSet
                    ? () => clearElementToken(target.elementId, editingMode, prop, partId)
                    : undefined}
            />
            <StyleLayerRow
                label="Custom"
                varName={boundCustomVar ? customVarCssName(boundCustomVar) : '--portal-custom-*'}
                value={customBound ? customValue : ''}
                isSet={customBound}
                isActive={activeTier === 'custom'}
                propertyDef={def}
                property={prop}
                customVarNames={customVarNames}
                selectedCustomVar={boundCustomVar}
                onCustomVarChange={varName => {
                    if (varName) {
                        onBindInstanceStyle(target.instanceId, prop, varName, target.usesBlockStorage);
                    } else if (boundCustomVar) {
                        onUnbindInstanceStyle(target.instanceId, prop, target.usesBlockStorage);
                    }
                }}
                onAddCustomVar={handleAddCustomVar}
                onChange={value => {
                    if (!boundCustomVar) {
                        const baseName = `${target.elementId}-${prop}`;
                        let name = baseName;
                        let suffix = 1;
                        while (theme.customVariables.some(cv => cv.name === name)) {
                            name = `${baseName}-${suffix}`;
                            suffix += 1;
                        }
                        addCustomVariable({
                            name,
                            lightValue: editingMode === 'light' ? value : '',
                            darkValue: editingMode === 'dark' ? value : '',
                        });
                        onBindInstanceStyle(target.instanceId, prop, name, target.usesBlockStorage);
                        return;
                    }
                    updateCustomVariable(boundCustomVar, editingMode === 'light'
                        ? { lightValue: value }
                        : { darkValue: value });
                }}
                onClear={customBound
                    ? () => onUnbindInstanceStyle(target.instanceId, prop, target.usesBlockStorage)
                    : undefined}
            />
        </div>
    );
}

export function CustomizeStylePanel({
    target,
    position,
    themeState,
    editingMode,
    getInstanceStyleBinding,
    onBindInstanceStyle,
    onUnbindInstanceStyle,
    onClose,
}: CustomizeStylePanelProps) {
    const elementDef = getElementDef(target.elementId);
    const panelRef = useRef<HTMLDivElement>(null);
    const dragRef = useRef<{ startX: number; startY: number; originX: number; originY: number } | null>(null);
    const [pos, setPos] = useState(position);
    const [activeProp, setActiveProp] = useState<string | null>(null);
    const [bindingRevision, setBindingRevision] = useState(0);

    const handleBindInstanceStyle = useCallback((
        instanceId: string,
        prop: string,
        customVarName: string,
        usesBlockStorage: boolean,
    ) => {
        onBindInstanceStyle(instanceId, prop, customVarName, usesBlockStorage);
        setBindingRevision(revision => revision + 1);
    }, [onBindInstanceStyle]);

    const handleUnbindInstanceStyle = useCallback((
        instanceId: string,
        prop: string,
        usesBlockStorage: boolean,
    ) => {
        onUnbindInstanceStyle(instanceId, prop, usesBlockStorage);
        setBindingRevision(revision => revision + 1);
    }, [onUnbindInstanceStyle]);

    const onDragStart = useCallback((e: React.MouseEvent) => {
        dragRef.current = { startX: e.clientX, startY: e.clientY, originX: pos.x, originY: pos.y };
    }, [pos]);

    const onDragMove = useCallback((e: React.MouseEvent) => {
        if (!dragRef.current) return;
        const dx = e.clientX - dragRef.current.startX;
        const dy = e.clientY - dragRef.current.startY;
        setPos({ x: dragRef.current.originX + dx, y: dragRef.current.originY + dy });
    }, []);

    const onDragEnd = useCallback(() => {
        dragRef.current = null;
    }, []);

    if (!elementDef) return null;

    const partId = target.variant ?? (elementDef.parts ? 'default' : undefined);
    const activePart = elementDef.parts?.find(part => part.id === partId);
    const editableProperties = activePart?.properties ?? elementDef.properties;
    const partLabel = activePart?.label;

    return (
        <div
            ref={panelRef}
            className={styles.panel}
            style={{ left: pos.x, top: pos.y }}
            onMouseMove={onDragMove}
            onMouseUp={onDragEnd}
            onMouseLeave={onDragEnd}
        >
            <div className={styles.header} onMouseDown={onDragStart}>
                <div>
                    <strong>{elementDef.label}</strong>
                    {(partLabel ?? target.variant) && (
                        <span className={styles.variant}> · {partLabel ?? target.variant}</span>
                    )}
                </div>
                <button type="button" className={styles.closeBtn} onClick={onClose} aria-label="Close">
                    <XIcon className="size-4" />
                </button>
            </div>

            <div className={styles.body}>
                <p className={styles.hint}>Customize styles — changes apply live</p>
                {Object.entries(editableProperties).map(([prop, def]) => (
                    <div key={prop} className={styles.property}>
                        <button
                            type="button"
                            className={`${styles.propertyBtn} ${activeProp === prop ? styles.propertyBtnActive : ''}`}
                            onClick={() => setActiveProp(activeProp === prop ? null : prop)}
                        >
                            {def.label}
                        </button>
                        {activeProp === prop && (
                            <PropertyStyleLayers
                                prop={prop}
                                def={def}
                                target={target}
                                themeState={themeState}
                                editingMode={editingMode}
                                bindingRevision={bindingRevision}
                                getInstanceStyleBinding={getInstanceStyleBinding}
                                onBindInstanceStyle={handleBindInstanceStyle}
                                onUnbindInstanceStyle={handleUnbindInstanceStyle}
                            />
                        )}
                    </div>
                ))}
            </div>

            <div className={styles.footer}>
                <Button size="sm" variant="outline" onClick={onClose}>Done</Button>
            </div>
        </div>
    );
}
