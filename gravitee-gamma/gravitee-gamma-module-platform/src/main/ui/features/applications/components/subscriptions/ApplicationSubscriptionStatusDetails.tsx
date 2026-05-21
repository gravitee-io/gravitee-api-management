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
import { Button, Collapsible, CollapsibleContent, CollapsibleTrigger, cn } from '@gravitee/graphene-core';
import { ChevronDownIcon, InfoIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

export function ApplicationSubscriptionStatusDetails() {
    const [open, setOpen] = useState(false);

    return (
        <Collapsible open={open} onOpenChange={setOpen}>
            <CollapsibleTrigger asChild>
                <Button variant="ghost" size="sm" className="-ml-2 h-8 px-2 text-muted-foreground">
                    <InfoIcon className="mr-1.5 size-4" aria-hidden />
                    Subscription status details
                    <ChevronDownIcon className={cn('ml-1 size-4 transition-transform', open && 'rotate-180')} aria-hidden />
                </Button>
            </CollapsibleTrigger>
            <CollapsibleContent>
                <ul className="mt-2 list-inside list-disc space-y-2 pl-1 text-sm text-muted-foreground">
                    <li>
                        <strong className="text-foreground">Accepted</strong> — Subscription is valid and the application can use its
                        credentials to perform operations on the API.
                    </li>
                    <li>
                        <strong className="text-foreground">Pending</strong> — The application has requested access and is waiting for API
                        owner approval.
                    </li>
                    <li>
                        <strong className="text-foreground">Rejected</strong> — Subscription is rejected and the application cannot use the
                        API.
                    </li>
                    <li>
                        <strong className="text-foreground">Closed</strong> — Subscription is closed and the application cannot use the API.
                    </li>
                </ul>
            </CollapsibleContent>
        </Collapsible>
    );
}
