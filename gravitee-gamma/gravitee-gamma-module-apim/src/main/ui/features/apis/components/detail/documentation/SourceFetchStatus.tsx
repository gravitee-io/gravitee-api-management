/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Alert, AlertDescription, Button } from '@gravitee/graphene-core';
import { AlertCircleIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';

import type { ApiDocumentationItem, PortalNavigationItemSource } from '../../../types/apiDocumentation';
import { describeCronSchedule } from '../../../utils/cronSchedule';
import { fetcherLabel } from '../../../utils/fetcherSchema';
import { formatDateTime } from '../../../utils/formatDate';

/** Where a synced page comes from, when it was last fetched and why its last fetch failed, if it did. */
export function SourceFetchStatus({
    source,
    syncedFolder,
    canFetchSyncedFolder = false,
    onFetch,
    isFetching = false,
}: Readonly<{
    /** The page's own source. */
    source?: PortalNavigationItemSource;
    /** The folder synced from a source that the page is in, which fetches it. */
    syncedFolder?: ApiDocumentationItem;
    /** Whether the user may fetch that folder, which only some synced folders accept. */
    canFetchSyncedFolder?: boolean;
    /** Absent without the permission to fetch. */
    onFetch?: () => void;
    isFetching?: boolean;
}>) {
    if (syncedFolder) {
        return (
            <div
                role="status"
                aria-label="External source"
                className="rounded-lg border bg-muted/40 px-3 py-2 text-sm text-muted-foreground"
            >
                {`Managed by the folder ${syncedFolder.title}, read only.`}
                {canFetchSyncedFolder ? ' Fetch it from that folder.' : null}
            </div>
        );
    }
    if (!source) return null;

    const schedule = source.useAutoFetch && source.fetchCron ? describeCronSchedule(source.fetchCron) : null;
    const details = [
        source.lastFetchedAt ? `Last fetched ${formatDateTime(source.lastFetchedAt)}` : 'Not fetched yet',
        ...(schedule ? [schedule] : []),
    ];

    return (
        <div className="flex flex-col gap-3">
            <div
                role="status"
                aria-label="External source"
                className="flex flex-wrap items-center gap-x-3 gap-y-2 rounded-lg border bg-muted/40 px-3 py-2 text-sm"
            >
                <span className="font-medium">Content from {fetcherLabel(source.type)}, read only</span>
                <span className="flex-1 text-muted-foreground">{details.join(' · ')}</span>
                {onFetch ? (
                    <Button size="sm" variant="outline" onClick={onFetch} disabled={isFetching}>
                        <RefreshCwIcon className="size-4" aria-hidden />
                        Fetch now
                    </Button>
                ) : null}
            </div>
            {source.lastFetchError ? (
                <Alert variant="destructive">
                    <AlertCircleIcon />
                    <AlertDescription>
                        {`Last fetch failed on ${formatDateTime(source.lastFetchAttemptAt)}. ${withPeriod(source.lastFetchError)} `}
                        {source.lastFetchedAt
                            ? 'The content below is from the last successful fetch.'
                            : 'No content has been fetched from it yet. Until then, the page holds the placeholder it was created with.'}
                    </AlertDescription>
                </Alert>
            ) : null}
        </div>
    );
}

function withPeriod(message: string): string {
    return /[.!?]$/.test(message) ? message : `${message}.`;
}
