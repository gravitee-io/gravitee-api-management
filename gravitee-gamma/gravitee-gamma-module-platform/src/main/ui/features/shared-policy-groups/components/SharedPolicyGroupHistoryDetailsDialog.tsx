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
    Button,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    ScrollArea,
    Skeleton,
    Textarea,
} from '@gravitee/graphene-core';
import { getProtocolType, type Policy } from '@gravitee/graphene-policy-studio';
import { useQuery } from '@tanstack/react-query';
import { useCallback, useId } from 'react';

import { SharedPolicyGroupPolicyStudio } from './SharedPolicyGroupPolicyStudio';
import { getPolicyDocumentation, getPolicySchema, listPolicies } from '../services/sharedPolicyGroupPolicies';
import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';
import { sharedPolicyGroupKeys } from '../utils/queryKeys';

interface SharedPolicyGroupHistoryDetailsDialogProps {
    readonly sharedPolicyGroup?: SharedPolicyGroup;
    readonly canRestore: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onRestore: (sharedPolicyGroup: SharedPolicyGroup) => void;
}

export function SharedPolicyGroupHistoryDetailsDialog({
    sharedPolicyGroup,
    canRestore,
    onOpenChange,
    onRestore,
}: SharedPolicyGroupHistoryDetailsDialogProps) {
    if (!sharedPolicyGroup) {
        return null;
    }

    return (
        <SharedPolicyGroupHistoryDetailsDialogContent
            key={`${sharedPolicyGroup.id}-${sharedPolicyGroup.version ?? 'unknown'}`}
            sharedPolicyGroup={sharedPolicyGroup}
            canRestore={canRestore}
            onOpenChange={onOpenChange}
            onRestore={onRestore}
        />
    );
}

function SharedPolicyGroupHistoryDetailsDialogContent({
    sharedPolicyGroup,
    canRestore,
    onOpenChange,
    onRestore,
}: Omit<SharedPolicyGroupHistoryDetailsDialogProps, 'sharedPolicyGroup'> & { readonly sharedPolicyGroup: SharedPolicyGroup }) {
    const nameId = useId();
    const descriptionId = useId();
    const prerequisiteId = useId();
    const protocolType = getProtocolType(sharedPolicyGroup.apiType);
    const policiesQuery = useQuery({
        queryKey: sharedPolicyGroupKeys.policies(),
        queryFn: listPolicies,
        staleTime: 5 * 60 * 1000,
    });
    const onFetchPolicySchema = useCallback((policy: Policy) => getPolicySchema(policy.id, protocolType), [protocolType]);
    const onFetchPolicyDocumentation = useCallback((policy: Policy) => getPolicyDocumentation(policy.id, protocolType), [protocolType]);

    return (
        <Dialog open onOpenChange={onOpenChange}>
            {/* DialogContent caps itself at `sm:max-w-sm`. An unprefixed `max-w-none` never wins against a
                breakpoint-prefixed class, so the cap has to be cleared at that breakpoint too — without
                `sm:max-w-none` the policy canvas renders in a 24rem column. */}
            <DialogContent className="w-[min(72rem,calc(100vw-2rem))] max-w-none sm:max-w-none">
                <DialogHeader>
                    <DialogTitle>Version {sharedPolicyGroup.version ?? '—'} details</DialogTitle>
                    <DialogDescription>Review this historical version and its configured policies.</DialogDescription>
                </DialogHeader>
                <ScrollArea className="h-[min(72vh,48rem)] pr-4">
                    <div className="space-y-6">
                        <section className="space-y-4" aria-labelledby={`${nameId}-heading`}>
                            <h2 id={`${nameId}-heading`} className="text-sm font-semibold">
                                Read-only version state
                            </h2>
                            <div className="space-y-2">
                                <Label htmlFor={nameId}>Name</Label>
                                <Input id={nameId} value={sharedPolicyGroup.name} readOnly />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor={descriptionId}>Description</Label>
                                <Textarea id={descriptionId} value={sharedPolicyGroup.description ?? ''} readOnly />
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor={prerequisiteId}>Prerequisite message</Label>
                                <Textarea id={prerequisiteId} value={sharedPolicyGroup.prerequisiteMessage ?? ''} readOnly />
                            </div>
                        </section>
                        {policiesQuery.isLoading ? <Skeleton className="h-[32rem] w-full rounded-lg" /> : null}
                        {policiesQuery.isError ? (
                            <p className="text-sm text-destructive">Failed to load the policy catalog. Please refresh and try again.</p>
                        ) : null}
                        {policiesQuery.data ? (
                            <SharedPolicyGroupPolicyStudio
                                sharedPolicyGroup={sharedPolicyGroup}
                                policies={policiesQuery.data}
                                readOnly
                                onFetchPolicySchema={onFetchPolicySchema}
                                onFetchPolicyDocumentation={onFetchPolicyDocumentation}
                            />
                        ) : null}
                    </div>
                </ScrollArea>
                <DialogFooter>
                    {canRestore ? (
                        <Button type="button" onClick={() => onRestore(sharedPolicyGroup)}>
                            Restore version
                        </Button>
                    ) : null}
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Close
                        </Button>
                    </DialogClose>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
