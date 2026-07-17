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
import { useCallback, useEffect, useState } from 'react';

import type { Application } from '../../editor/entities/application';
import { getApplicationById, listApplications } from '../../editor/services/applications.service';

import { ApplicationCreate } from './ApplicationCreate';
import { ApplicationDetails } from './ApplicationDetails';
import { ApplicationsList } from './ApplicationsList';
import styles from './ApplicationsBlock.module.scss';

const PAGE_SIZE = 6;

type ViewState =
    | { mode: 'list' }
    | { mode: 'create' }
    | { mode: 'details'; applicationId: string };

interface ApplicationsViewProps {
    readonly isEditable?: boolean;
}

export function ApplicationsView({ isEditable = false }: ApplicationsViewProps) {
    const [view, setView] = useState<ViewState>({ mode: 'list' });
    const [applications, setApplications] = useState<Application[]>([]);
    const [selectedApplication, setSelectedApplication] = useState<Application | null>(null);
    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [totalItems, setTotalItems] = useState(0);
    const [loading, setLoading] = useState(true);

    const loadApplications = useCallback(async () => {
        setLoading(true);
        try {
            const response = await listApplications({ page, size: PAGE_SIZE });
            setApplications(response.data);
            setTotalItems(response.metadata?.pagination?.total ?? response.data.length);
            setTotalPages(response.metadata?.pagination?.total_pages ?? 1);
        } finally {
            setLoading(false);
        }
    }, [page]);

    useEffect(() => {
        void loadApplications();
    }, [loadApplications]);

    useEffect(() => {
        if (view.mode !== 'details') {
            setSelectedApplication(null);
            return;
        }

        let cancelled = false;
        void (async () => {
            const app = await getApplicationById(view.applicationId);
            if (!cancelled) setSelectedApplication(app ?? null);
        })();

        return () => {
            cancelled = true;
        };
    }, [view]);

    const handleCreated = (application: Application) => {
        setView({ mode: 'details', applicationId: application.id });
        void loadApplications();
    };

    const handleUpdated = (application: Application) => {
        setSelectedApplication(application);
        void loadApplications();
    };

    const handleDeleted = () => {
        setView({ mode: 'list' });
        void loadApplications();
    };

    if (view.mode === 'create') {
        return (
            <div className={styles.wrapper}>
                <ApplicationCreate
                    onCancel={() => setView({ mode: 'list' })}
                    onCreated={handleCreated}
                />
            </div>
        );
    }

    if (view.mode === 'details') {
        if (!selectedApplication) {
            return (
                <div className={styles.wrapper}>
                    <p className={styles.loadingMessage}>Loading application…</p>
                </div>
            );
        }

        return (
            <div className={styles.wrapper}>
                <ApplicationDetails
                    application={selectedApplication}
                    onBack={() => setView({ mode: 'list' })}
                    onUpdated={handleUpdated}
                    onDeleted={handleDeleted}
                />
            </div>
        );
    }

    return (
        <div className={styles.wrapper}>
            <ApplicationsList
                applications={applications}
                currentPage={page}
                totalPages={totalPages}
                totalItems={totalItems}
                loading={loading}
                showCreateButton
                onSelectApplication={app => {
                    if (isEditable) return;
                    setView({ mode: 'details', applicationId: app.id });
                }}
                onCreate={() => {
                    if (isEditable) return;
                    setView({ mode: 'create' });
                }}
                onPageChange={setPage}
            />
        </div>
    );
}
