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
import { Button } from '@gravitee/graphene-core';
import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { notify } from '../../../shared/notify/notify';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { ApiMappingPanel } from '../components/ApiMappingPanel';
import { FormBuilderPanel } from '../components/FormBuilderPanel';
import { usePortal } from '../hooks/usePortal';
import { usePortalSubscriptionForm } from '../hooks/usePortalSubscriptionForms';
import {
    FIELD_TYPE_LABELS,
    type FormField,
    type FormFieldPatch,
    type FormFieldType,
    type MappedApi,
} from '../types';
import { downloadBuilderConfig, readBuilderConfigFile } from '../utils/subscription-form-config';

function createFieldId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `field-${Date.now()}`;
}

function createDraftField(type: FormFieldType): FormField {
    return {
        id: createFieldId(),
        type,
        label: FIELD_TYPE_LABELS[type],
        required: false,
        options: type === 'dropdown' || type === 'radio' ? ['Option 1', 'Option 2'] : [],
        validation: '',
        expression: '',
    };
}

export function SubscriptionFormDetailPage() {
    const { portalId = '', formId = '' } = useParams<{ portalId: string; formId: string }>();
    const navigate = useNavigate();
    const importInputRef = useRef<HTMLInputElement>(null);
    const { homePath, portalSettingsSectionPath } = usePortalsNavigation();
    const { portal, loading: portalLoading, missing: portalMissing } = usePortal(portalId);
    const {
        form,
        loading: formLoading,
        missing: formMissing,
        updateMappedApis,
        saveFields,
    } = usePortalSubscriptionForm(portalId, formId);

    const [draftFields, setDraftFields] = useState<FormField[]>([]);
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        if (form) {
            setDraftFields([...form.fields]);
        }
    }, [form]);

    if (portalLoading || formLoading) {
        return <p className="p-6 text-sm text-muted-foreground">Loading subscription form…</p>;
    }

    if (portalMissing || !portal) {
        return (
            <NotFoundPage
                homePath={homePath}
                homeLabel="Back to portals"
                title="Portal not found"
                description="This developer portal does not exist or may have been removed."
            />
        );
    }

    const formListPath = portalSettingsSectionPath(portal.id, 'subscription-forms');

    if (formMissing || !form) {
        return (
            <NotFoundPage
                homePath={formListPath}
                homeLabel="Back to subscription forms"
                title="Subscription form not found"
                description="This subscription form does not exist or may have been removed."
            />
        );
    }

    const handleAddField = (type: FormFieldType, index?: number) => {
        setDraftFields(current => {
            const next = [...current];
            const insertAt = index === undefined || index < 0 || index > next.length ? next.length : index;
            next.splice(insertAt, 0, createDraftField(type));
            return next;
        });
    };

    const handleUpdateField = (fieldId: string, patch: FormFieldPatch) => {
        setDraftFields(current => current.map(field => (field.id === fieldId ? { ...field, ...patch } : field)));
    };

    const handleRemoveField = (fieldId: string) => {
        setDraftFields(current => current.filter(field => field.id !== fieldId));
    };

    const handleMoveField = (fromIndex: number, toIndex: number) => {
        setDraftFields(current => {
            if (fromIndex < 0 || fromIndex >= current.length || toIndex < 0 || toIndex >= current.length) {
                return current;
            }
            const next = [...current];
            const [moved] = next.splice(fromIndex, 1);
            if (!moved) {
                return current;
            }
            next.splice(toIndex, 0, moved);
            return next;
        });
    };

    const handleSave = async () => {
        setIsSaving(true);
        try {
            await saveFields(draftFields);
            notify.success('Subscription form saved.');
            navigate(formListPath);
        } finally {
            setIsSaving(false);
        }
    };

    const handleExport = () => {
        const safeName = form.name.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-') || 'subscription-form';
        downloadBuilderConfig(draftFields, `${safeName}-builder.json`);
        notify.success('Builder configuration exported.');
    };

    const handleImportClick = () => importInputRef.current?.click();

    const handleImportFile = async (file: File | undefined) => {
        if (!file) {
            return;
        }
        try {
            const fields = await readBuilderConfigFile(file);
            setDraftFields(fields);
            notify.success('Builder configuration imported.');
        } catch (error) {
            notify.error(error, 'Failed to import configuration.');
        } finally {
            if (importInputRef.current) {
                importInputRef.current.value = '';
            }
        }
    };

    const handleMappedApisChange = (next: readonly MappedApi[]) => {
        void updateMappedApis(next).then(() => {
            notify.success('Mapped APIs updated.');
        });
    };

    return (
        <div className="mx-auto max-w-screen-2xl space-y-8 p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{portal.name}</p>
                    <h1 className="text-2xl font-bold tracking-tight">{form.name}</h1>
                    {form.description ? <p className="text-sm text-muted-foreground">{form.description}</p> : null}
                </div>

                <div className="flex flex-wrap items-center gap-2">
                    <input
                        ref={importInputRef}
                        type="file"
                        accept="application/json,.json"
                        className="hidden"
                        aria-label="Import builder configuration file"
                        onChange={event => void handleImportFile(event.target.files?.[0])}
                    />
                    <Button type="button" variant="outline" onClick={handleImportClick}>
                        Import
                    </Button>
                    <Button type="button" variant="outline" onClick={handleExport}>
                        Export
                    </Button>
                    <Button type="button" onClick={() => void handleSave()} disabled={isSaving}>
                        {isSaving ? 'Saving…' : 'Save'}
                    </Button>
                </div>
            </div>

            <ApiMappingPanel
                mappedApis={form.mappedApis}
                onChange={handleMappedApisChange}
                description="Choose which APIs use this subscription form."
            />

            <FormBuilderPanel
                fields={draftFields}
                onAddField={handleAddField}
                onUpdateField={handleUpdateField}
                onRemoveField={handleRemoveField}
                onMoveField={handleMoveField}
            />

            <div className="flex flex-wrap items-center gap-2">
                <Button type="button" onClick={() => void handleSave()} disabled={isSaving}>
                    {isSaving ? 'Saving…' : 'Save'}
                </Button>
                <Button variant="outline" asChild>
                    <Link to={formListPath}>Cancel</Link>
                </Button>
            </div>
        </div>
    );
}
