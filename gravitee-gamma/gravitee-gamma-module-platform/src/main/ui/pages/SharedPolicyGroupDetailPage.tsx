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

import { Card, CardContent, CardHeader, CardTitle, DateCell } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';
import { useOutletContext } from 'react-router-dom';

import { type SharedPolicyGroup, toReadableApiType, toReadableFlowPhase } from '../features/shared-policy-groups/types/sharedPolicyGroup';

function DetailField({ label, value }: Readonly<{ label: string; value: ReactNode }>) {
    return (
        <div className="space-y-1">
            <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
            <dd className="text-sm">{value}</dd>
        </div>
    );
}

/** Overview tab — metadata details (header/tabs live on SharedPolicyGroupDetailLayout). */
export function SharedPolicyGroupDetailPage() {
    const sharedPolicyGroup = useOutletContext<SharedPolicyGroup>();

    return (
        <div className="space-y-6" data-testid="shared-policy-group-overview">
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Details</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                        <DetailField label="API type" value={toReadableApiType(sharedPolicyGroup.apiType)} />
                        <DetailField label="Phase" value={toReadableFlowPhase(sharedPolicyGroup.phase)} />
                        <DetailField
                            label="Last updated"
                            value={
                                sharedPolicyGroup.updatedAt ? (
                                    <DateCell value={new Date(sharedPolicyGroup.updatedAt)} format="absolute" />
                                ) : (
                                    '—'
                                )
                            }
                        />
                        <DetailField
                            label="Last deployed"
                            value={
                                sharedPolicyGroup.deployedAt ? (
                                    <DateCell value={new Date(sharedPolicyGroup.deployedAt)} format="absolute" />
                                ) : (
                                    '—'
                                )
                            }
                        />
                    </dl>
                    {sharedPolicyGroup.prerequisiteMessage && (
                        <DetailField label="Prerequisite message" value={sharedPolicyGroup.prerequisiteMessage} />
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
