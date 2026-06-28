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
import { useCallback, useState } from 'react';
import { Button } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';

import type { CustomVariable } from '../types';
import styles from './CustomVariablesEditor.module.scss';

interface CustomVariablesEditorProps {
    readonly variables: readonly CustomVariable[];
    readonly onAdd: (variable: CustomVariable) => void;
    readonly onUpdate: (id: string, patch: Partial<Omit<CustomVariable, 'id'>>) => void;
    readonly onRemove: (id: string) => void;
}

export function CustomVariablesEditor({ variables, onAdd, onUpdate, onRemove }: CustomVariablesEditorProps) {
    const [newName, setNewName] = useState('');

    const handleAdd = useCallback(() => {
        const trimmed = newName.trim();
        if (!trimmed) return;
        onAdd({
            id: `custom-${Date.now()}`,
            name: trimmed,
            lightValue: '',
            darkValue: '',
        });
        setNewName('');
    }, [newName, onAdd]);

    return (
        <div className={styles.container}>
            <table className={styles.table}>
                <thead>
                    <tr>
                        <th className={styles.th}>Name</th>
                        <th className={styles.th}>Light Value</th>
                        <th className={styles.th}>Dark Value</th>
                        <th className={styles.thAction} />
                    </tr>
                </thead>
                <tbody>
                    {variables.map(v => (
                        <tr key={v.id}>
                            <td className={styles.td}>
                                <code className={styles.varName}>--portal-custom-{v.name}</code>
                            </td>
                            <td className={styles.td}>
                                <input
                                    type="text"
                                    className={styles.input}
                                    value={v.lightValue}
                                    onChange={e => onUpdate(v.id, { lightValue: e.target.value })}
                                    placeholder="value"
                                />
                            </td>
                            <td className={styles.td}>
                                <input
                                    type="text"
                                    className={styles.input}
                                    value={v.darkValue}
                                    onChange={e => onUpdate(v.id, { darkValue: e.target.value })}
                                    placeholder="value"
                                />
                            </td>
                            <td className={styles.tdAction}>
                                <button
                                    type="button"
                                    className={styles.removeBtn}
                                    onClick={() => onRemove(v.id)}
                                    aria-label={`Remove ${v.name}`}
                                >
                                    <XIcon className="size-3.5" aria-hidden="true" />
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>

            <div className={styles.addRow}>
                <input
                    type="text"
                    className={styles.input}
                    placeholder="Variable name"
                    value={newName}
                    onChange={e => setNewName(e.target.value)}
                    onKeyDown={e => {
                        if (e.key === 'Enter') handleAdd();
                    }}
                />
                <Button size="sm" variant="outline" onClick={handleAdd} disabled={!newName.trim()}>
                    Add Variable
                </Button>
            </div>

            {variables.length === 0 && (
                <p className={styles.empty}>
                    No custom variables. Add one to create reusable design tokens.
                </p>
            )}
        </div>
    );
}
