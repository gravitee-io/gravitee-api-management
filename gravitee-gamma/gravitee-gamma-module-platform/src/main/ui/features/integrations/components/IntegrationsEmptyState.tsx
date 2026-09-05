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

import { DataTableEmptyState } from '@gravitee/graphene-core';
import { PlugIcon } from '@gravitee/graphene-core/icons';

export function IntegrationsEmptyState() {
    return (
        <div className="rounded-lg border">
            <DataTableEmptyState
                variant="first-use"
                icon={<PlugIcon className="size-8" aria-hidden />}
                title="No integrations yet"
                description="Create an integration to start importing APIs and event streams from a 3rd-party provider."
            />
        </div>
    );
}
