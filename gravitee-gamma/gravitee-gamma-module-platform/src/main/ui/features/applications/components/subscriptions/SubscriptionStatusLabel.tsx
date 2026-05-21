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
import { cn } from '@gravitee/graphene-core';

import type { SubscriptionStatus } from '../../types/applicationSubscription';

export function SubscriptionStatusLabel({ status }: Readonly<{ status: SubscriptionStatus }>) {
    return (
        <span
            className={cn(
                'inline-flex h-6 items-center rounded-md px-2.5 text-xs font-medium uppercase leading-none',
                status === 'ACCEPTED' || status === 'PENDING'
                    ? 'bg-primary text-primary-foreground'
                    : status === 'CLOSED' || status === 'REJECTED'
                      ? 'bg-destructive text-destructive-foreground'
                      : 'bg-muted text-muted-foreground',
            )}
        >
            {status}
        </span>
    );
}
