/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Subscription form, composed client-side from the generic portal navigation item content
 * endpoint (gmdContent) and the API-scoped subscription-form endpoint (resolvedOptions). See
 * `PortalService.getSubscriptionForm`. Portal does not expose id or enabled flag to consumers.
 */
export interface SubscriptionForm {
  gmdContent: string;
  resolvedOptions?: Record<string, string[]>;
}

/**
 * Response of `GET /apis/{apiId}/subscription-form`: the subscription form's resolved dynamic
 * options for a specific API, and the visibility check for that API. Content is fetched
 * separately via the generic portal navigation item content endpoint.
 * Returned only when the form exists, is enabled, and the API is visible (404 otherwise).
 */
export interface ResolvedSubscriptionFormOptions {
  resolvedOptions?: Record<string, string[]>;
}
