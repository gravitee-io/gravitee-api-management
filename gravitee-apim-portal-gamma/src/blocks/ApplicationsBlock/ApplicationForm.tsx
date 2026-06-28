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
import { useState } from 'react';

import type { ApplicationTypeConfig } from '../../features/editor/entities/application';
import { getDefaultGrantTypes } from '../../features/editor/services/applications.mock';

import type { ApplicationFormState } from './utils';
import styles from './ApplicationsBlock.module.scss';

interface ApplicationFormProps {
    readonly typeConfig: ApplicationTypeConfig;
    readonly form: ApplicationFormState;
    readonly mode: 'create' | 'edit';
    readonly onChange: (form: ApplicationFormState) => void;
}

export function ApplicationForm({ typeConfig, form, mode, onChange }: ApplicationFormProps) {
    const isSimple = typeConfig.applicationType === 'SIMPLE';
    const isOAuth = !isSimple;

    const update = (patch: Partial<ApplicationFormState>) => onChange({ ...form, ...patch });

    return (
        <div className={styles.formSections}>
            <section className={styles.formSection}>
                <h3 className={styles.formSectionTitle}>General</h3>
                <label className={styles.formField}>
                    <span className={styles.formLabel}>Name *</span>
                    <input
                        className={styles.formInput}
                        value={form.name}
                        onChange={event => update({ name: event.target.value })}
                        placeholder="Application name"
                    />
                </label>
                <label className={styles.formField}>
                    <span className={styles.formLabel}>Description</span>
                    <textarea
                        className={styles.formTextarea}
                        value={form.description}
                        onChange={event => update({ description: event.target.value })}
                        placeholder="Optional description"
                        rows={3}
                    />
                </label>
                {mode === 'edit' && (
                    <label className={styles.formField}>
                        <span className={styles.formLabel}>Domain</span>
                        <input
                            className={styles.formInput}
                            value={form.domain}
                            onChange={event => update({ domain: event.target.value })}
                            placeholder="example.com"
                        />
                    </label>
                )}
            </section>

            <section className={styles.formSection}>
                <h3 className={styles.formSectionTitle}>Security</h3>

                {isSimple && (
                    <>
                        <label className={styles.formField}>
                            <span className={styles.formLabel}>Application type</span>
                            <input
                                className={styles.formInput}
                                value={form.appType}
                                onChange={event => update({ appType: event.target.value })}
                                placeholder="e.g. mobile, internal"
                            />
                        </label>
                        <label className={styles.formField}>
                            <span className={styles.formLabel}>Client ID</span>
                            <input
                                className={styles.formInput}
                                value={form.clientId}
                                onChange={event => update({ clientId: event.target.value })}
                                placeholder="Custom client ID"
                            />
                        </label>
                    </>
                )}

                {isOAuth && (
                    <>
                        {mode === 'edit' && (
                            <>
                                <div className={styles.formField}>
                                    <span className={styles.formLabel}>Client ID</span>
                                    <CopyField value={form.clientId} />
                                </div>
                                <div className={styles.formField}>
                                    <span className={styles.formLabel}>Client secret</span>
                                    <CopyField value="••••••••••••••••" />
                                </div>
                            </>
                        )}

                        {typeConfig.requires_redirect_uris && (
                            <RedirectUriInput
                                uris={form.redirectUris}
                                onChange={redirectUris => update({ redirectUris })}
                            />
                        )}

                        <GrantTypeSelector
                            typeConfig={typeConfig}
                            selected={form.grantTypes}
                            onChange={grantTypes => update({ grantTypes })}
                        />

                        {mode === 'create' && (
                            <MetadataInput
                                entries={form.metadataEntries}
                                onChange={metadataEntries => update({ metadataEntries })}
                            />
                        )}
                    </>
                )}
            </section>
        </div>
    );
}

interface RedirectUriInputProps {
    readonly uris: string[];
    readonly onChange: (uris: string[]) => void;
}

function RedirectUriInput({ uris, onChange }: RedirectUriInputProps) {
    const [input, setInput] = useState('');

    const addUri = () => {
        const trimmed = input.trim();
        if (!trimmed || uris.includes(trimmed)) return;
        onChange([...uris, trimmed]);
        setInput('');
    };

    const removeUri = (uri: string) => onChange(uris.filter(item => item !== uri));

    return (
        <div className={styles.formField}>
            <span className={styles.formLabel}>Redirect URIs *</span>
            <div className={styles.chipInputRow}>
                <input
                    className={styles.formInput}
                    value={input}
                    onChange={event => setInput(event.target.value)}
                    onKeyDown={event => {
                        if (event.key === 'Enter') {
                            event.preventDefault();
                            addUri();
                        }
                    }}
                    placeholder="https://example.com/callback"
                />
                <button type="button" className={styles.secondaryBtn} onClick={addUri}>
                    Add
                </button>
            </div>
            {uris.length > 0 && (
                <div className={styles.chipList}>
                    {uris.map(uri => (
                        <span key={uri} className={styles.chip}>
                            {uri}
                            <button type="button" className={styles.chipRemove} onClick={() => removeUri(uri)} aria-label={`Remove ${uri}`}>
                                ×
                            </button>
                        </span>
                    ))}
                </div>
            )}
        </div>
    );
}

interface GrantTypeSelectorProps {
    readonly typeConfig: ApplicationTypeConfig;
    readonly selected: string[];
    readonly onChange: (grantTypes: string[]) => void;
}

function GrantTypeSelector({ typeConfig, selected, onChange }: GrantTypeSelectorProps) {
    if (typeConfig.allowed_grant_types.length === 0) return null;

    const mandatory = new Set(typeConfig.mandatory_grant_types.map(grant => grant.type));

    const toggle = (type: string) => {
        if (mandatory.has(type)) return;
        if (selected.includes(type)) {
            onChange(selected.filter(item => item !== type));
        } else {
            onChange([...selected, type]);
        }
    };

    return (
        <div className={styles.formField}>
            <span className={styles.formLabel}>Grant types</span>
            <div className={styles.grantTypeList}>
                {typeConfig.allowed_grant_types.map(grant => {
                    const isMandatory = mandatory.has(grant.type);
                    const isSelected = selected.includes(grant.type);

                    return (
                        <label key={grant.type} className={styles.grantTypeItem}>
                            <input
                                type="checkbox"
                                checked={isSelected}
                                disabled={isMandatory}
                                onChange={() => toggle(grant.type)}
                            />
                            <span>
                                {grant.name}
                                {isMandatory ? ' (Mandatory)' : ''}
                            </span>
                        </label>
                    );
                })}
            </div>
        </div>
    );
}

interface MetadataInputProps {
    readonly entries: ApplicationFormState['metadataEntries'];
    readonly onChange: (entries: ApplicationFormState['metadataEntries']) => void;
}

function MetadataInput({ entries, onChange }: MetadataInputProps) {
    const updateEntry = (index: number, patch: Partial<ApplicationFormState['metadataEntries'][number]>) => {
        onChange(entries.map((entry, i) => (i === index ? { ...entry, ...patch } : entry)));
    };

    const addEntry = () => onChange([...entries, { key: '', value: '' }]);
    const removeEntry = (index: number) => onChange(entries.filter((_, i) => i !== index));

    return (
        <div className={styles.formField}>
            <span className={styles.formLabel}>Additional client metadata</span>
            {entries.map((entry, index) => (
                <div key={index} className={styles.metadataRow}>
                    <input
                        className={styles.formInput}
                        value={entry.key}
                        onChange={event => updateEntry(index, { key: event.target.value })}
                        placeholder="Key"
                    />
                    <input
                        className={styles.formInput}
                        value={entry.value}
                        onChange={event => updateEntry(index, { value: event.target.value })}
                        placeholder="Value"
                    />
                    <button type="button" className={styles.iconBtn} onClick={() => removeEntry(index)} aria-label="Remove metadata row">
                        ×
                    </button>
                </div>
            ))}
            <button type="button" className={styles.linkBtn} onClick={addEntry}>
                + Add metadata
            </button>
        </div>
    );
}

interface CopyFieldProps {
    readonly value: string;
}

function CopyField({ value }: CopyFieldProps) {
    const [copied, setCopied] = useState(false);

    const handleCopy = async () => {
        try {
            await navigator.clipboard.writeText(value);
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        } catch {
            // ignore clipboard errors in tests
        }
    };

    return (
        <div className={styles.copyField}>
            <code className={styles.copyValue}>{value}</code>
            <button type="button" className={styles.secondaryBtn} onClick={handleCopy}>
                {copied ? 'Copied' : 'Copy'}
            </button>
        </div>
    );
}

export function initializeGrantTypesForType(typeConfig: ApplicationTypeConfig): string[] {
    return getDefaultGrantTypes(typeConfig);
}
