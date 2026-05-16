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
import { Badge, Card, CardContent, CardHeader, CardTitle, Skeleton } from '@gravitee/graphene-core';

import type { Subscription } from '../../../features/apis/types/subscription';
import { formatDateTime } from '../../../features/apis/utils/formatDate';
import { SubscriptionStatusBadge } from '../SubscriptionStatusBadge';

function Row({ label, children }: { label: string; children: React.ReactNode }) {
    return (
        <div style={{ display: 'grid', gridTemplateColumns: '180px 1fr', gap: '1rem' }} className="py-2.5 border-b last:border-0">
            <span className="text-sm text-muted-foreground shrink-0">{label}</span>
            <span className="text-sm min-w-0 break-words">{children}</span>
        </div>
    );
}

function DateCell({ value }: { value?: string }) {
    return <span className={value ? undefined : 'text-muted-foreground'}>{formatDateTime(value)}</span>;
}

interface SubscriptionInfoCardProps {
    subscription: Subscription;
    isLoading: boolean;
}

export function SubscriptionInfoCard({ subscription: sub, isLoading }: Readonly<SubscriptionInfoCardProps>) {
    if (isLoading) {
        return (
            <Card>
                <CardHeader>
                    <Skeleton className="h-5 w-40 rounded" />
                </CardHeader>
                <CardContent className="space-y-3">
                    {Array.from({ length: 8 }).map((_, i) => (
                        <Skeleton key={i} className="h-4 w-full rounded" />
                    ))}
                </CardContent>
            </Card>
        );
    }

    const isSharedKey = sub.application.apiKeyMode === 'SHARED' && sub.plan.security?.type === 'API_KEY';

    return (
        <Card>
            <CardHeader className="pb-0">
                <CardTitle className="text-base">Subscription details</CardTitle>
            </CardHeader>
            <CardContent className="pt-2">
                <Row label="ID">
                    <span className="font-mono text-xs break-all">{sub.id}</span>
                </Row>
                <Row label="Plan">
                    <div className="flex items-center gap-2">
                        <span>{sub.plan.name}</span>
                        {sub.plan.security?.type && (
                            <Badge variant="secondary" className="text-xs">
                                {sub.plan.security.type}
                            </Badge>
                        )}
                        {isSharedKey && (
                            <Badge variant="outline" className="text-xs">
                                Shared
                            </Badge>
                        )}
                    </div>
                </Row>
                <Row label="Status">
                    <SubscriptionStatusBadge status={sub.status} />
                </Row>
                {sub.consumerStatus && (
                    <Row label="Consumer status">
                        {sub.consumerStatus === 'STARTED' ? (
                            <Badge className="bg-success/10 text-success border-transparent">Started</Badge>
                        ) : sub.consumerStatus === 'FAILURE' ? (
                            <Badge variant="destructive">Failure</Badge>
                        ) : (
                            <Badge variant="secondary">Stopped</Badge>
                        )}
                    </Row>
                )}
                {sub.subscribedBy?.displayName && <Row label="Subscribed by">{sub.subscribedBy.displayName}</Row>}
                <Row label="Application">
                    <div>
                        <p>{sub.application.name}</p>
                        {sub.application.primaryOwner?.displayName && (
                            <p className="text-xs text-muted-foreground">{sub.application.primaryOwner.displayName}</p>
                        )}
                    </div>
                </Row>
                {sub.publisherMessage && <Row label="Publisher message">{sub.publisherMessage}</Row>}
                {sub.consumerMessage && <Row label="Subscriber message">{sub.consumerMessage}</Row>}
                <Row label="Created at">
                    <DateCell value={sub.createdAt} />
                </Row>
                <Row label="Updated at">
                    <DateCell value={sub.updatedAt} />
                </Row>
                <Row label="Processed at">
                    <DateCell value={sub.processedAt} />
                </Row>
                <Row label="Starting at">
                    <DateCell value={sub.startingAt} />
                </Row>
                {sub.pausedAt && (
                    <Row label="Paused at">
                        <DateCell value={sub.pausedAt} />
                    </Row>
                )}
                <Row label="Ending at">
                    <DateCell value={sub.endingAt} />
                </Row>
                {sub.closedAt && (
                    <Row label="Closed at">
                        <DateCell value={sub.closedAt} />
                    </Row>
                )}
                {sub.metadata && Object.keys(sub.metadata).length > 0 && (
                    <Row label="Metadata">
                        <div className="space-y-1">
                            {Object.entries(sub.metadata).map(([k, v]) => (
                                <p key={k} className="text-xs">
                                    <span className="font-mono text-muted-foreground">{k}:</span> {v}
                                </p>
                            ))}
                        </div>
                    </Row>
                )}
            </CardContent>
        </Card>
    );
}
