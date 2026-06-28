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

import type { Application } from '../../features/editor/entities/application';
import { updateApplication } from '../../features/editor/services/applications.service';
import { getApplicationTypeByEnum } from '../../features/editor/services/applications.mock';

import { ApplicationForm } from './ApplicationForm';
import { applicationToFormState, buildApplicationFromForm, isFormValid } from './utils';
import styles from './ApplicationsBlock.module.scss';

interface ApplicationEditProps {
    readonly application: Application;
    readonly onCancel: () => void;
    readonly onSaved: (application: Application) => void;
}

export function ApplicationEdit({ application, onCancel, onSaved }: ApplicationEditProps) {
    const typeConfig = application.applicationType
        ? getApplicationTypeByEnum(application.applicationType)
        : undefined;

    const [form, setForm] = useState(() =>
        typeConfig ? applicationToFormState(application, typeConfig) : applicationToFormState(application, {
            id: 'simple',
            name: 'Simple',
            description: '',
            applicationType: 'SIMPLE',
            requires_redirect_uris: false,
            allowed_grant_types: [],
            default_grant_types: [],
            mandatory_grant_types: [],
        }),
    );
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async () => {
        if (!typeConfig || !isFormValid(form, typeConfig)) return;

        setSubmitting(true);
        setError(null);
        try {
            const updated = buildApplicationFromForm(form, typeConfig, application);
            const saved = await updateApplication(updated);
            onSaved(saved);
        } catch {
            setError('Failed to save application.');
        } finally {
            setSubmitting(false);
        }
    };

    if (!typeConfig) {
        return <p className={styles.errorMessage}>Unknown application type.</p>;
    }

    return (
        <div className={styles.editLayout}>
            <h2 className={styles.viewTitle}>Edit application</h2>
            <ApplicationForm typeConfig={typeConfig} form={form} mode="edit" onChange={setForm} />

            {error && <p className={styles.errorMessage} role="alert">{error}</p>}

            <div className={styles.actions}>
                <button type="button" className={styles.secondaryBtn} onClick={onCancel}>
                    Cancel
                </button>
                <button
                    type="button"
                    className={styles.primaryBtn}
                    disabled={!isFormValid(form, typeConfig) || submitting}
                    onClick={() => void handleSubmit()}
                >
                    {submitting ? 'Saving…' : 'Save'}
                </button>
            </div>
        </div>
    );
}
