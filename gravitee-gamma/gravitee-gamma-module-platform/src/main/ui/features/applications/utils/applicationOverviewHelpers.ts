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
import type { ApplicationListItem } from '../types/application';

export function hasReviewedGeneralSettings(application: ApplicationListItem | null): boolean {
    if (!application) {
        return false;
    }

    return Boolean(
        application.description?.trim() ||
            application.domain?.trim() ||
            application.settings?.app?.client_id?.trim() ||
            application.settings?.oauth?.client_id?.trim() ||
            application.settings?.oauth?.redirect_uris?.length ||
            application.updated_at > application.created_at,
    );
}

export function getSubscriptionCountFromPage(response: { page?: { total_elements?: number } } | undefined): number {
    return response?.page?.total_elements ?? 0;
}
