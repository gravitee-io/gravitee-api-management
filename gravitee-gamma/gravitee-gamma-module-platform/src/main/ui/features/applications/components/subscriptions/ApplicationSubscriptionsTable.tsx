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
import {
    Badge,
    Button,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { CircleXIcon, EyeIcon } from '@gravitee/graphene-core/icons';

import type { ApplicationSubscriptionTableRow } from '../../types/applicationSubscription';
import { formatApplicationDateTime } from '../../utils/applicationFormatters';
import { canCloseSubscription } from '../../utils/applicationSubscriptionMapper';

function SubscriptionStatusBadge({ status }: { status: ApplicationSubscriptionTableRow['status'] }) {
    const variant =
        status === 'ACCEPTED'
            ? 'default'
            : status === 'PENDING' || status === 'REJECTED'
              ? 'secondary'
              : status === 'CLOSED'
                ? 'destructive'
                : 'outline';

    return <Badge variant={variant}>{status}</Badge>;
}

function SkeletonRow() {
    return (
        <TableRow>
            {Array.from({ length: 9 }).map((_, index) => (
                <TableCell key={index}>
                    <Skeleton className="h-4 w-24 rounded" />
                </TableCell>
            ))}
        </TableRow>
    );
}

export function ApplicationSubscriptionsTable({
    rows,
    isLoading,
    skeletonRowCount,
    readOnly,
    canViewDetail,
    canClose,
    onView,
    onClose,
}: Readonly<{
    rows: ApplicationSubscriptionTableRow[];
    isLoading: boolean;
    skeletonRowCount: number;
    readOnly: boolean;
    canViewDetail: boolean;
    canClose: boolean;
    onView: (row: ApplicationSubscriptionTableRow) => void;
    onClose: (row: ApplicationSubscriptionTableRow) => void;
}>) {
    return (
        <div className="overflow-hidden overflow-x-auto rounded-xl border bg-card">
            <Table aria-label="Subscriptions table">
                <TableHeader>
                    <TableRow>
                        <TableHead>Security type</TableHead>
                        <TableHead>Plan</TableHead>
                        <TableHead>API</TableHead>
                        <TableHead>Created at</TableHead>
                        <TableHead>Processed at</TableHead>
                        <TableHead>Started at</TableHead>
                        <TableHead>Ended at</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="w-[88px]" />
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {isLoading ? Array.from({ length: skeletonRowCount }).map((_, index) => <SkeletonRow key={index} />) : null}
                    {!isLoading && rows.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={9} className="h-24 text-center text-muted-foreground">
                                There is no subscription (yet).
                            </TableCell>
                        </TableRow>
                    ) : null}
                    {!isLoading
                        ? rows.map(row => (
                              <TableRow key={row.id}>
                                  <TableCell>
                                      <span className="text-sm">{row.securityType}</span>
                                      {row.isSharedApiKey ? (
                                          <Badge variant="outline" className="ml-2 text-[10px]">
                                              Shared
                                          </Badge>
                                      ) : null}
                                  </TableCell>
                                  <TableCell className="text-sm">{row.planName}</TableCell>
                                  <TableCell>
                                      <p className="text-sm font-medium">{row.apiName}</p>
                                      <p className="text-[11px] text-muted-foreground">
                                          {row.referenceTypeLabel}
                                          {row.apiVersion ? ` · v${row.apiVersion}` : ''}
                                      </p>
                                  </TableCell>
                                  <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                                      {formatApplicationDateTime(row.createdAt)}
                                  </TableCell>
                                  <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                                      {formatApplicationDateTime(row.processedAt)}
                                  </TableCell>
                                  <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                                      {formatApplicationDateTime(row.startingAt)}
                                  </TableCell>
                                  <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                                      {formatApplicationDateTime(row.endAt)}
                                  </TableCell>
                                  <TableCell>
                                      <SubscriptionStatusBadge status={row.status} />
                                  </TableCell>
                                  <TableCell>
                                      <div className="flex items-center justify-end gap-0.5">
                                          {canViewDetail ? (
                                              <Button
                                                  type="button"
                                                  variant="ghost"
                                                  size="icon"
                                                  className="size-8"
                                                  aria-label="Subscription details"
                                                  onClick={() => onView(row)}
                                              >
                                                  <EyeIcon className="size-4" aria-hidden />
                                              </Button>
                                          ) : null}
                                          {!readOnly && canClose && canCloseSubscription(row.status) && row.origin !== 'KUBERNETES' ? (
                                              <TooltipProvider>
                                                  <Tooltip>
                                                      <TooltipTrigger asChild>
                                                          <Button
                                                              type="button"
                                                              variant="ghost"
                                                              size="icon"
                                                              className="size-8 text-destructive hover:text-destructive"
                                                              aria-label="Close subscription"
                                                              onClick={() => onClose(row)}
                                                          >
                                                              <CircleXIcon className="size-4" aria-hidden />
                                                          </Button>
                                                      </TooltipTrigger>
                                                      <TooltipContent>Close subscription</TooltipContent>
                                                  </Tooltip>
                                              </TooltipProvider>
                                          ) : null}
                                      </div>
                                  </TableCell>
                              </TableRow>
                          ))
                        : null}
                </TableBody>
            </Table>
        </div>
    );
}
