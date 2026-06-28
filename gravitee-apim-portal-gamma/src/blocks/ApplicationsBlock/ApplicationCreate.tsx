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
import type { ApplicationTypeConfig } from '../../features/editor/entities/application';
import { createApplication } from '../../features/editor/services/applications.service';
import { getApplicationTypes } from '../../features/editor/services/applications.mock';

import { ApplicationForm, initializeGrantTypesForType } from './ApplicationForm';
import {
    buildApplicationFromForm,
    createEmptyFormState,
    isFormValid,
    type ApplicationFormState,
} from './utils';
import styles from './ApplicationsBlock.module.scss';

interface ApplicationCreateProps {
    readonly onCancel: () => void;
    readonly onCreated: (application: Application) => void;
}

export function ApplicationCreate({ onCancel, onCreated }: ApplicationCreateProps) {
    const types = getApplicationTypes();
    const [selectedTypeId, setSelectedTypeId] = useState<string | null>(null);
    const [form, setForm] = useState<ApplicationFormState>(createEmptyFormState());
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const typeConfig = types.find(type => type.id === selectedTypeId);

    const selectType = (type: ApplicationTypeConfig) => {
        setSelectedTypeId(type.id);
        setForm({
            ...createEmptyFormState(),
            grantTypes: initializeGrantTypesForType(type),
        });
    };

    const handleSubmit = async () => {
        if (!typeConfig || !isFormValid(form, typeConfig)) return;

        setSubmitting(true);
        setError(null);
        try {
            const application = buildApplicationFromForm(form, typeConfig);
            const created = await createApplication(application);
            onCreated(created);
        } catch {
            setError('Failed to create application.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className={styles.createLayout}>
            <div className={styles.createHeader}>
                <h2 className={styles.viewTitle}>Create application</h2>
                <p className={styles.viewDescription}>Choose an application type and configure its settings.</p>
            </div>

            <div className={styles.createColumns}>
                <section className={styles.typePicker}>
                    <h3 className={styles.formSectionTitle}>Application type</h3>
                    <div className={styles.typeList} role="radiogroup" aria-label="Application type">
                        {types.map(type => (
                            <button
                                key={type.id}
                                type="button"
                                role="radio"
                                aria-checked={selectedTypeId === type.id}
                                className={`${styles.typeCard} ${selectedTypeId === type.id ? styles.typeCardSelected : ''}`}
                                onClick={() => selectType(type)}
                            >
                                <span className={styles.typeCardName}>{type.name}</span>
                                <span className={styles.typeCardDescription}>{type.description}</span>
                            </button>
                        ))}
                    </div>
                </section>

                <section className={styles.createFormPanel}>
                    {typeConfig ? (
                        <ApplicationForm
                            typeConfig={typeConfig}
                            form={form}
                            mode="create"
                            onChange={setForm}
                        />
                    ) : (
                        <p className={styles.placeholderText}>Select an application type to configure details.</p>
                    )}
                </section>
            </div>

            {error && <p className={styles.errorMessage} role="alert">{error}</p>}

            <div className={styles.actions}>
                <button type="button" className={styles.secondaryBtn} onClick={onCancel}>
                    Cancel
                </button>
                <button
                    type="button"
                    className={styles.primaryBtn}
                    disabled={!typeConfig || !isFormValid(form, typeConfig) || submitting}
                    onClick={() => void handleSubmit()}
                >
                    {submitting ? 'Creating…' : 'Create'}
                </button>
            </div>
        </div>
    );
}
