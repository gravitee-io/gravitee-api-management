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
import { formatEntityUid } from '../../../shared/entity-adapter';
import { childrenByType, referencedBy } from '../../../shared/entity-relationships';
import type { EntityInstance } from '../../../shared/entity.types';

export function EntityRelationshipsTab({ entity, allEntities }: { entity: EntityInstance; allEntities: readonly EntityInstance[] }) {
    const parents = entity.parents;
    const children = useMemo(() => referencedBy(entity, allEntities), [entity, allEntities]);
    const grouped = useMemo(() => childrenByType(entity, allEntities), [entity, allEntities]);

    if (parents.length === 0 && children.length === 0) {
        return <p className="text-xs text-muted-foreground">No relationships.</p>;
    }

    return (
        <div className="flex flex-col gap-5">
            {grouped.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                    {grouped.map(group => (
                        <Badge key={group.type} variant="secondary" className="text-xs">
                            contains {group.count} {group.type}
                        </Badge>
                    ))}
                </div>
            )}
            {parents.length > 0 && (
                <section className="flex flex-col gap-2">
                    <h3 className="text-sm font-semibold">Parents</h3>
                    {parents.map(parent => (
                        <span key={formatEntityUid(parent)} className="font-mono text-sm">
                            {formatEntityUid(parent)}
                        </span>
                    ))}
                </section>
            )}
            {children.length > 0 && (
                <section className="flex flex-col gap-2">
                    <h3 className="text-sm font-semibold">Referenced by</h3>
                    {children.map(child => (
                        <span key={formatEntityUid(child.uid)} className="font-mono text-sm">
                            {formatEntityUid(child.uid)}
                        </span>
                    ))}
                </section>
            )}
        </div>
    );
}
