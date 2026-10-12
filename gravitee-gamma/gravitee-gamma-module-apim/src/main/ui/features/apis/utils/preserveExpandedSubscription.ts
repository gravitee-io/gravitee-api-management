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
import type { Subscription } from '../types/subscription';

/** Action responses omit the expands the detail page loaded (plan security, API key mode, subscriber). */
export function preserveExpandedSubscription(previous: Subscription | undefined, next: Subscription): Subscription {
    if (!previous) return next;
    return {
        ...next,
        plan: next.plan?.security?.type ? next.plan : previous.plan,
        application: {
            ...previous.application,
            ...next.application,
            apiKeyMode: next.application?.apiKeyMode ?? previous.application?.apiKeyMode,
        },
        subscribedBy: next.subscribedBy ?? previous.subscribedBy,
    };
}
