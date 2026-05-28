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
import { DEFAULT_SUBSCRIPTION_PAGE_SIZE, TABLE_PAGE_SIZE_OPTIONS } from './paginationConstants';
import type { SubscriptionStatus } from '../types/applicationSubscription';

export { DEFAULT_SUBSCRIPTION_PAGE_SIZE };

export const SUBSCRIPTION_STATUS_OPTIONS: { id: SubscriptionStatus; name: string }[] = [
    { id: 'ACCEPTED', name: 'Accepted' },
    { id: 'CLOSED', name: 'Closed' },
    { id: 'PAUSED', name: 'Paused' },
    { id: 'PENDING', name: 'Pending' },
    { id: 'REJECTED', name: 'Rejected' },
    { id: 'RESUMED', name: 'Resumed' },
];

export const DEFAULT_SUBSCRIPTION_FILTER_STATUSES: SubscriptionStatus[] = ['ACCEPTED', 'PAUSED', 'PENDING', 'RESUMED'];

/** Every status the Management API supports when listing application subscriptions. */
export const ALL_SUBSCRIPTION_STATUSES: SubscriptionStatus[] = SUBSCRIPTION_STATUS_OPTIONS.map(option => option.id);

export const SUBSCRIPTION_PAGE_SIZE_OPTIONS: number[] = [...TABLE_PAGE_SIZE_OPTIONS];
