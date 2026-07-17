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
import type { Application } from '../../../editor/entities/application';

import styles from '../SubscriptionFlowBlock.module.scss';

interface ChooseApplicationProps {
    readonly applications: Application[];
    readonly selectedApplicationId: string | null;
    readonly disabledApplicationIds: Set<string>;
    readonly currentPage: number;
    readonly totalPages: number;
    readonly totalApplications: number;
    readonly onSelectApplication: (application: Application) => void;
    readonly onPageChange: (page: number) => void;
}

export function ChooseApplication({
    applications,
    selectedApplicationId,
    disabledApplicationIds,
    currentPage,
    totalPages,
    totalApplications,
    onSelectApplication,
    onPageChange,
}: ChooseApplicationProps) {
    if (applications.length === 0) {
        return (
            <div className={styles.emptyState}>
                <p className={styles.emptyTitle}>No application found</p>
                <p className={styles.emptyMessage}>
                    Create an application and it will show up here.
                </p>
            </div>
        );
    }

    return (
        <div>
            <div className={styles.applicationList} role="radiogroup" aria-label="Choose an application">
                {applications.map(application => {
                    const selected = application.id === selectedApplicationId;
                    const disabled = disabledApplicationIds.has(application.id);

                    return (
                        <button
                            key={application.id}
                            type="button"
                            role="radio"
                            aria-checked={selected}
                            disabled={disabled}
                            className={`${styles.radioCard} ${selected ? styles.radioCardSelected : ''} ${
                                disabled ? styles.radioCardDisabled : ''
                            }`}
                            onClick={() => onSelectApplication(application)}
                        >
                            <div className={styles.radioCardHeader}>
                                <span className={styles.radioIndicator} aria-hidden="true" />
                                <span className={styles.radioCardTitle}>{application.name}</span>
                            </div>
                            {application.description && (
                                <p className={styles.radioCardDescription}>{application.description}</p>
                            )}
                            {application.applicationType && (
                                <span className={styles.badge}>{application.applicationType}</span>
                            )}
                            {disabled && (
                                <p className={styles.disabledMessage}>
                                    This application already has an active subscription to this plan.
                                </p>
                            )}
                        </button>
                    );
                })}
            </div>

            {totalPages > 1 && (
                <nav className={styles.pagination} aria-label="Applications pagination">
                    <button
                        type="button"
                        className={styles.paginationBtn}
                        disabled={currentPage <= 1}
                        onClick={() => onPageChange(currentPage - 1)}
                    >
                        Previous page
                    </button>
                    <span className={styles.paginationInfo}>
                        Page {currentPage} of {totalPages} ({totalApplications} applications)
                    </span>
                    <button
                        type="button"
                        className={styles.paginationBtn}
                        disabled={currentPage >= totalPages}
                        onClick={() => onPageChange(currentPage + 1)}
                    >
                        Next page
                    </button>
                </nav>
            )}
        </div>
    );
}
