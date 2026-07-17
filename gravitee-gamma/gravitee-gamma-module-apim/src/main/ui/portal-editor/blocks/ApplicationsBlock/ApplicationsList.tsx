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
import type { Application } from '../../editor/entities/application';
import { formatApplicationType, formatDate } from './utils';
import styles from './ApplicationsBlock.module.scss';

interface ApplicationsListProps {
    readonly applications: Application[];
    readonly currentPage: number;
    readonly totalPages: number;
    readonly totalItems: number;
    readonly loading: boolean;
    readonly showCreateButton?: boolean;
    readonly onSelectApplication: (application: Application) => void;
    readonly onCreate: () => void;
    readonly onPageChange: (page: number) => void;
}

export function ApplicationsList({
    applications,
    currentPage,
    totalPages,
    totalItems,
    loading,
    showCreateButton = true,
    onSelectApplication,
    onCreate,
    onPageChange,
}: ApplicationsListProps) {
    return (
        <div className={styles.listView}>
            <div className={styles.listHeader}>
                <div>
                    <h2 className={styles.viewTitle}>Applications</h2>
                    <p className={styles.viewDescription}>Manage your applications and their settings.</p>
                </div>
                {showCreateButton ? (
                    <button type="button" className={styles.primaryBtn} onClick={onCreate}>
                        Create
                    </button>
                ) : null}
            </div>

            {loading && applications.length === 0 ? (
                <p className={styles.loadingMessage}>Loading applications…</p>
            ) : applications.length === 0 ? (
                <div className={styles.emptyState}>
                    <p className={styles.emptyTitle}>No applications available yet</p>
                    <p className={styles.emptyMessage}>Create your first application to get started.</p>
                </div>
            ) : (
                <>
                    <div className={styles.cardGrid}>
                        {applications.map(application => (
                            <button
                                key={application.id}
                                type="button"
                                className={styles.appCard}
                                onClick={() => onSelectApplication(application)}
                            >
                                <div className={styles.appCardHeader}>
                                    <h3 className={styles.appCardTitle}>{application.name}</h3>
                                    {application.applicationType && (
                                        <span className={styles.badge}>
                                            {formatApplicationType(application.applicationType)}
                                        </span>
                                    )}
                                </div>
                                <p className={styles.appCardDescription}>
                                    {application.description || 'Description for this application is missing.'}
                                </p>
                                <span className={styles.appCardMeta}>
                                    Created {formatDate(application.created_at)}
                                </span>
                            </button>
                        ))}
                    </div>

                    {totalPages > 1 && (
                        <div className={styles.pagination}>
                            <button
                                type="button"
                                className={styles.secondaryBtn}
                                disabled={currentPage <= 1}
                                onClick={() => onPageChange(currentPage - 1)}
                            >
                                Previous
                            </button>
                            <span className={styles.paginationInfo}>
                                Page {currentPage} of {totalPages} ({totalItems} applications)
                            </span>
                            <button
                                type="button"
                                className={styles.secondaryBtn}
                                disabled={currentPage >= totalPages}
                                onClick={() => onPageChange(currentPage + 1)}
                            >
                                Next
                            </button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
