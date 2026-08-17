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
import { LayersIcon } from '@gravitee/graphene-core/icons';

export function SharedPolicyGroupStudioEmptyState() {
    return (
        <div className="rounded-lg border" data-testid="shared-policy-group-studio-empty">
            <DataTableEmptyState
                variant="first-use"
                icon={<LayersIcon className="size-8" aria-hidden />}
                title="No policies yet"
                description="Combine policies into this reusable group, then apply it across API flows. Policy Studio configuration lands in a follow-up."
            />
        </div>
    );
}
