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
import { Button, Table, TableBody, TableCell, TableRow } from '@gravitee/graphene-core';
import { CopyIcon } from '@gravitee/graphene-core/icons';

import type { ApplicationSubscriptionDetail } from '../../types/applicationSubscription';
import { formatApplicationDateTime } from '../../utils/applicationFormatters';

function MetadataDetailRow({ metadata }: Readonly<{ metadata?: Record<string, string> }>) {
    const entries = metadata ? Object.entries(metadata) : [];
    if (entries.length === 0) {
        return <DetailRow label="Metadata" value="" />;
    }

    return (
        <TableRow className="border-0 hover:bg-transparent">
            <TableCell className="w-40 border-0 bg-transparent py-3 align-top text-sm text-muted-foreground">Metadata</TableCell>
            <TableCell className="border-0 bg-transparent py-3 align-top text-sm text-foreground">
                <div className="space-y-1">
                    {entries.map(([key, value]) => (
                        <p key={key} className="text-xs break-all">
                            <span className="font-mono text-muted-foreground">{key}:</span> {value}
                        </p>
                    ))}
                </div>
            </TableCell>
        </TableRow>
    );
}

function DetailRow({
    label,
    value,
    copyable = false,
}: Readonly<{
    label: string;
    value: string;
    copyable?: boolean;
}>) {
    const display = value || '—';
    const canCopy = copyable && display !== '—';

    return (
        <TableRow className="border-0 hover:bg-transparent">
            <TableCell className="w-40 border-0 bg-transparent py-3 align-top text-sm text-muted-foreground">{label}</TableCell>
            <TableCell className="border-0 bg-transparent py-3 align-top text-sm text-foreground">
                <div className="flex items-start gap-2">
                    <span className="break-all">{display}</span>
                    {canCopy ? (
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="size-7 shrink-0"
                            aria-label={`Copy ${label}`}
                            onClick={() => void navigator.clipboard?.writeText(display)}
                        >
                            <CopyIcon className="size-3.5" aria-hidden />
                        </Button>
                    ) : null}
                </div>
            </TableCell>
        </TableRow>
    );
}

export function ApplicationSubscriptionDetailsTable({ detail }: Readonly<{ detail: ApplicationSubscriptionDetail }>) {
    return (
        <Table className="border-0 [&_td]:border-0 [&_tr]:border-0">
            <TableBody>
                <DetailRow label="ID" value={detail.id} copyable />
                <DetailRow label={detail.referenceTypeLabel} value={detail.apiDisplay} copyable />
                <DetailRow label="Plan" value={detail.planName} copyable />
                <DetailRow label="Security type" value={detail.securityType} />
                <DetailRow label="Status" value={detail.status} />
                <DetailRow label="Subscribed by" value={detail.subscribedBy} />
                {detail.request ? <DetailRow label="Publisher message to subscriber" value={detail.request} /> : null}
                {detail.reason ? <DetailRow label="Subscriber message to publisher" value={detail.reason} /> : null}
                <DetailRow label="Created at" value={formatApplicationDateTime(detail.createdAt)} />
                <DetailRow label="Processed at" value={formatApplicationDateTime(detail.processedAt)} />
                {detail.status !== 'REJECTED' ? (
                    <>
                        <DetailRow label="Starting at" value={formatApplicationDateTime(detail.startingAt)} />
                        <DetailRow label="Paused at" value={formatApplicationDateTime(detail.pausedAt)} />
                        <DetailRow label="Ending at" value={formatApplicationDateTime(detail.endingAt)} />
                    </>
                ) : null}
                <DetailRow label="Closed at" value={formatApplicationDateTime(detail.closedAt)} />
                <MetadataDetailRow metadata={detail.metadata} />
            </TableBody>
        </Table>
    );
}
