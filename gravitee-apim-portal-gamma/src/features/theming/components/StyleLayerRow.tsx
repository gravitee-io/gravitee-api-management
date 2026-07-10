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
import { PlusIcon } from '@gravitee/graphene-core/icons';

import type { PropertyDef } from '../registry/element-registry';
import { ColorInput } from './ColorInput';
import { SizeControl } from './SizeControl';
import styles from './StyleLayerRow.module.scss';

interface StyleLayerRowProps {
    readonly label: string;
    readonly varName: string;
    readonly value: string;
    readonly isSet: boolean;
    readonly isActive: boolean;
    readonly propertyDef: PropertyDef;
    readonly property: string;
    readonly onChange: (value: string) => void;
    readonly onClear?: () => void;
    readonly customVarNames?: readonly string[];
    readonly selectedCustomVar?: string;
    readonly onCustomVarChange?: (varName: string) => void;
    readonly onAddCustomVar?: () => void;
}

export function StyleLayerRow({
    label,
    varName,
    value,
    isSet,
    isActive,
    propertyDef,
    property,
    onChange,
    onClear,
    customVarNames,
    selectedCustomVar,
    onCustomVarChange,
    onAddCustomVar,
}: StyleLayerRowProps) {
    const isCustomRow = customVarNames !== undefined;

    return (
        <div
            className={`${styles.row} ${isSet ? styles.set : styles.unset} ${isActive ? styles.active : ''}`}
        >
            <div className={styles.header}>
                <span className={styles.label}>{label}</span>
                {isSet && onClear && (
                    <button type="button" className={styles.clearBtn} onClick={onClear}>
                        Clear
                    </button>
                )}
            </div>
            <div className={styles.controls}>
                {propertyDef.type === 'color' ? (
                    <ColorInput value={value} onChange={onChange} label={propertyDef.label} />
                ) : (
                    <SizeControl
                        value={value}
                        property={property}
                        presets={propertyDef.sizePresets}
                        onChange={onChange}
                    />
                )}
                {isCustomRow ? (
                    <div className={styles.customVarGroup}>
                        <select
                            className={styles.customSelect}
                            value={selectedCustomVar ?? ''}
                            onChange={e => onCustomVarChange?.(e.target.value)}
                            aria-label="Custom variable"
                        >
                            <option value="">Select variable…</option>
                            {customVarNames.map(name => (
                                <option key={name} value={name}>{name}</option>
                            ))}
                        </select>
                        <button
                            type="button"
                            className={styles.addBtn}
                            onClick={onAddCustomVar}
                            aria-label="Add custom variable"
                        >
                            <PlusIcon className="size-3.5" />
                        </button>
                    </div>
                ) : (
                    <code className={styles.varName}>{varName}</code>
                )}
            </div>
        </div>
    );
}
