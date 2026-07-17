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
import type { SubscriptionTableRow } from './utils';
import { formatDate, formatStatus } from './utils';

import styles from './SubscriptionViewerBlock.module.scss';

interface SubscriptionsTableProps {
    readonly rows: SubscriptionTableRow[];
    readonly selectedId: string | null;
    readonly currentPage: number;
    readonly totalPages: number;
    readonly totalItems: number;
    readonly pageSize: number;
    readonly onSelectRow: (id: string) => void;
    readonly onPageChange: (page: number) => void;
}

export function SubscriptionsTable({
    rows,
    selectedId,
    currentPage,
    totalPages,
    totalItems,
    pageSize,
    onSelectRow,
    onPageChange,
}: SubscriptionsTableProps) {
    return (
        <div className={styles.tableSection}>
            <div className={styles.tableWrapper}>
                <table className={styles.table}>
                    <thead>
                        <tr>
                            <th scope="col">API</th>
                            <th scope="col">Plan</th>
                            <th scope="col">Application</th>
                            <th scope="col">Created</th>
                            <th scope="col">Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map(row => (
                            <tr
                                key={row.id}
                                className={`${styles.tableRow} ${selectedId === row.id ? styles.tableRowSelected : ''}`}
                                onClick={() => onSelectRow(row.id)}
                                onKeyDown={event => {
                                    if (event.key === 'Enter' || event.key === ' ') {
                                        event.preventDefault();
                                        onSelectRow(row.id);
                                    }
                                }}
                                tabIndex={0}
                                role="button"
                                aria-pressed={selectedId === row.id}
                            >
                                <td>{row.api}</td>
                                <td>{row.plan}</td>
                                <td>{row.application}</td>
                                <td>{formatDate(row.createdAt)}</td>
                                <td>
                                    <span className={`${styles.statusBadge} ${styles[`status${row.status}`]}`}>
                                        {formatStatus(row.status)}
                                    </span>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {totalPages > 1 && (
                <nav className={styles.pagination} aria-label="Subscriptions pagination">
                    <button
                        type="button"
                        className={styles.paginationBtn}
                        disabled={currentPage <= 1}
                        onClick={() => onPageChange(currentPage - 1)}
                    >
                        Previous page
                    </button>
                    <span className={styles.paginationInfo}>
                        Page {currentPage} of {totalPages} ({totalItems} subscriptions, {pageSize} per page)
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
