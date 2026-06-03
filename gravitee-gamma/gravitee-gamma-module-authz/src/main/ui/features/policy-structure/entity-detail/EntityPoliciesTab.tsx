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
import { Badge } from '@gravitee/graphene-core';
import { useMemo } from 'react';
import type { PolicyResponse } from '../../../shared/api/authz-api.types';
import { policiesFor } from '../../../shared/entity-relationships';
import type { EntityInstance } from '../../../shared/entity.types';

export function EntityPoliciesTab({ entity, policies }: { entity: EntityInstance; policies: readonly PolicyResponse[] }) {
    const matched = useMemo(() => policiesFor(entity, policies), [entity, policies]);

    if (matched.length === 0) {
        return <p className="text-xs text-muted-foreground">No policies reference this entity.</p>;
    }

    return (
        <div className="flex flex-col">
            {matched.map(policy => (
                <div key={policy.id} className="flex items-center justify-between border-t py-2 first:border-t-0">
                    <span className="font-medium">{policy.name}</span>
                    <Badge variant="secondary">{policy.status}</Badge>
                </div>
            ))}
        </div>
    );
}
