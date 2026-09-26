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
import type { PlanStatus } from '../../../types/plan';

const PLAN_STATUSES: readonly PlanStatus[] = ['STAGING', 'PUBLISHED', 'DEPRECATED', 'CLOSED'];

export function isPlanStatus(value: string | null | undefined): value is PlanStatus {
    return value !== null && value !== undefined && (PLAN_STATUSES as readonly string[]).includes(value);
}

/** Matches classic Console: open on Published unless the URL names another bucket. */
export function planStatusFromSearchParam(value: string | null | undefined): PlanStatus {
    return isPlanStatus(value) ? value : 'PUBLISHED';
}

export function planListSearchForStatus(status: PlanStatus): string {
    return `?status=${status}`;
}
