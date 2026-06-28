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
import { deleteApplication } from '../../features/editor/services/applications.service';
import { getApplicationTypeByEnum } from '../../features/editor/services/applications.mock';

import { ApplicationEdit } from './ApplicationEdit';
import { ApplicationMembers } from './ApplicationMembers';
import { formatApplicationType, formatDate, formatGrantType, isOAuthApplication } from './utils';
import styles from './ApplicationsBlock.module.scss';

type DetailsTab = 'settings' | 'members';

interface ApplicationDetailsProps {
    readonly application: Application;
    readonly onBack: () => void;
    readonly onUpdated: (application: Application) => void;
    readonly onDeleted: () => void;
}

export function ApplicationDetails({ application, onBack, onUpdated, onDeleted }: ApplicationDetailsProps) {
    const [activeTab, setActiveTab] = useState<DetailsTab>('settings');
    const [isEditing, setIsEditing] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState<string | null>(null);

    const typeConfig = application.applicationType
        ? getApplicationTypeByEnum(application.applicationType)
        : undefined;

    if (isEditing) {
        return (
            <div className={styles.detailsView}>
                <button type="button" className={styles.backBtn} onClick={() => setIsEditing(false)}>
                    ← Back to details
                </button>
                <ApplicationEdit
                    application={application}
                    onCancel={() => setIsEditing(false)}
                    onSaved={saved => {
                        onUpdated(saved);
                        setIsEditing(false);
                    }}
                />
            </div>
        );
    }

    const oauth = application.settings.oauth;
    const simple = application.settings.app;

    const handleDelete = async () => {
        setDeleting(true);
        setDeleteError(null);
        try {
            await deleteApplication(application.id);
            onDeleted();
        } catch {
            setDeleteError('Failed to delete application.');
            setDeleting(false);
        }
    };

    return (
        <div className={styles.detailsView}>
            <button type="button" className={styles.backBtn} onClick={onBack}>
                ← Back to applications
            </button>

            <div className={styles.detailsHeader}>
                <div>
                    <h2 className={styles.viewTitle}>{application.name}</h2>
                    {application.description && (
                        <p className={styles.viewDescription}>{application.description}</p>
                    )}
                </div>
            </div>

            <div className={styles.tabs} role="tablist" aria-label="Application details">
                <button
                    type="button"
                    role="tab"
                    aria-selected={activeTab === 'settings'}
                    className={`${styles.tab} ${activeTab === 'settings' ? styles.tabActive : ''}`}
                    onClick={() => setActiveTab('settings')}
                >
                    Settings
                </button>
                <button
                    type="button"
                    role="tab"
                    aria-selected={activeTab === 'members'}
                    className={`${styles.tab} ${activeTab === 'members' ? styles.tabActive : ''}`}
                    onClick={() => setActiveTab('members')}
                >
                    Members
                </button>
            </div>

            {activeTab === 'settings' && (
                <div className={styles.settingsPanel} role="tabpanel">
                    <div className={styles.settingsHeader}>
                        <h3 className={styles.formSectionTitle}>Settings & Security</h3>
                        <div className={styles.settingsActions}>
                            <button type="button" className={styles.secondaryBtn} onClick={() => setIsEditing(true)}>
                                Edit
                            </button>
                            <button
                                type="button"
                                className={styles.dangerBtn}
                                onClick={() => setShowDeleteDialog(true)}
                            >
                                Delete
                            </button>
                        </div>
                    </div>

                    <dl className={styles.detailList}>
                        <DetailRow label="Type" value={formatApplicationType(application.applicationType)} />
                        <DetailRow label="Created" value={formatDate(application.created_at)} />
                        <DetailRow label="Updated" value={formatDate(application.updated_at)} />
                        <DetailRow label="Domain" value={application.domain || 'Not set'} />
                        {application.owner && (
                            <DetailRow label="Owner" value={application.owner.display_name} />
                        )}

                        {simple && (
                            <>
                                <DetailRow label="Application type" value={simple.type || '—'} />
                                <DetailRow label="Client ID" value={simple.client_id || '—'} />
                            </>
                        )}

                        {oauth && typeConfig && (
                            <>
                                {oauth.grant_types && oauth.grant_types.length > 0 && (
                                    <DetailRow
                                        label="Grant types"
                                        value={oauth.grant_types
                                            .map(type => formatGrantType(type, typeConfig))
                                            .join(', ')}
                                    />
                                )}
                                {isOAuthApplication(application) &&
                                    !typeConfig.requires_redirect_uris && (
                                        <>
                                            <DetailRow label="Client ID" value={oauth.client_id || '—'} />
                                            <DetailRow label="Client secret" value={oauth.client_secret ? '••••••••' : '—'} />
                                        </>
                                    )}
                                {typeConfig.requires_redirect_uris && oauth.redirect_uris && (
                                    <DetailRow label="Redirect URIs" value={oauth.redirect_uris.join(', ')} />
                                )}
                            </>
                        )}
                    </dl>
                </div>
            )}

            {activeTab === 'members' && (
                <div role="tabpanel">
                    <ApplicationMembers applicationId={application.id} />
                </div>
            )}

            {showDeleteDialog && (
                <div
                    className={styles.dialogOverlay}
                    role="presentation"
                    onClick={() => !deleting && setShowDeleteDialog(false)}
                >
                    <div
                        className={styles.dialog}
                        role="alertdialog"
                        aria-labelledby="delete-app-title"
                        aria-describedby="delete-app-description"
                        onClick={event => event.stopPropagation()}
                    >
                        <h3 id="delete-app-title" className={styles.dialogTitle}>
                            Delete application
                        </h3>
                        <p id="delete-app-description" className={styles.dialogMessage}>
                            Are you sure you want to delete <strong>{application.name}</strong>? This action cannot
                            be undone.
                        </p>
                        {deleteError && (
                            <p className={styles.errorMessage} role="alert">
                                {deleteError}
                            </p>
                        )}
                        <div className={styles.dialogActions}>
                            <button
                                type="button"
                                className={styles.secondaryBtn}
                                disabled={deleting}
                                onClick={() => setShowDeleteDialog(false)}
                            >
                                Cancel
                            </button>
                            <button
                                type="button"
                                className={styles.dangerBtn}
                                disabled={deleting}
                                onClick={() => void handleDelete()}
                            >
                                {deleting ? 'Deleting…' : 'Delete'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

interface DetailRowProps {
    readonly label: string;
    readonly value: string;
}

function DetailRow({ label, value }: DetailRowProps) {
    return (
        <div className={styles.detailRow}>
            <dt className={styles.detailLabel}>{label}</dt>
            <dd className={styles.detailValue}>{value}</dd>
        </div>
    );
}
