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
import { Badge, Button, Sheet, SheetContent, SheetTitle, Tabs, TabsContent, TabsList, TabsTrigger } from '@gravitee/graphene-core';
import { CheckIcon, CopyIcon, PencilIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';
import type { PolicyResponse } from '../../../shared/api/authz-api.types';
import { formatEntityUid } from '../../../shared/entity-adapter';
import { policiesFor, referencedBy } from '../../../shared/entity-relationships';
import type { EntityInstance } from '../../../shared/entity.types';
import { EntityGaplShapeTab } from './EntityGaplShapeTab';
import { EntityOverviewTab } from './EntityOverviewTab';
import { EntityPoliciesTab } from './EntityPoliciesTab';
import { EntityRelationshipsTab } from './EntityRelationshipsTab';

type DetailTab = 'overview' | 'relationships' | 'policies' | 'gapl';

export interface EntityDetailSheetProps {
    readonly entity: EntityInstance | null;
    readonly allEntities: readonly EntityInstance[];
    readonly policies: readonly PolicyResponse[];
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onEdit: (entity: EntityInstance) => void;
}

function sourceLabel(source: EntityInstance['source']): string {
    if (source === 'apim') return 'APIM';
    if (source === 'gravitee-catalog') return 'Gravitee Catalog';
    return 'Local';
}

export function EntityDetailSheet({ entity, allEntities, policies, open, onOpenChange, onEdit }: EntityDetailSheetProps) {
    const [tab, setTab] = useState<DetailTab>('overview');
    const [copied, setCopied] = useState(false);

    // Land on Overview each time a different entity is opened.
    const entityUid = entity ? formatEntityUid(entity.uid) : '';
    useEffect(() => {
        setTab('overview');
        setCopied(false);
    }, [entityUid]);

    useEffect(() => {
        if (!copied) return;
        const timer = setTimeout(() => setCopied(false), 1500);
        return () => clearTimeout(timer);
    }, [copied]);

    const counts = useMemo(() => {
        if (!entity) return { attrs: 0, parents: 0, refs: 0, policies: 0 };
        return {
            attrs: Object.keys(entity.attrs).length,
            parents: entity.parents.length,
            refs: referencedBy(entity, allEntities).length,
            policies: policiesFor(entity, policies).length,
        };
    }, [entity, allEntities, policies]);

    async function copyUid() {
        try {
            await navigator.clipboard?.writeText(entityUid);
            setCopied(true);
        } catch {
            // clipboard unavailable
        }
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent
                side="right"
                showCloseButton
                aria-label="Entity details"
                style={{ width: 'min(640px, 100vw)', maxWidth: 'min(640px, 100vw)' }}
                className="flex h-full flex-col gap-0 p-0"
            >
                {entity && (
                    <>
                        <div className="flex flex-col gap-2 border-b px-6 py-4">
                            <div className="flex items-center gap-2">
                                <Badge variant="outline" className="font-mono text-xs">
                                    {entity.uid.type}
                                </Badge>
                                <Badge variant="secondary">{sourceLabel(entity.source)}</Badge>
                                {entity.source === 'local' && (
                                    <Button type="button" variant="outline" size="sm" className="ml-auto" onClick={() => onEdit(entity)}>
                                        <PencilIcon className="mr-2 size-4" aria-hidden />
                                        Edit
                                    </Button>
                                )}
                            </div>
                            <SheetTitle className="text-lg font-semibold">{entity.displayName ?? entity.uid.id}</SheetTitle>
                            <div className="flex items-center gap-1.5">
                                <span className="font-mono text-sm text-muted-foreground">{entityUid}</span>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    className="size-6 p-0"
                                    aria-label={`Copy ${entityUid}`}
                                    onClick={copyUid}
                                >
                                    {copied ? (
                                        <CheckIcon className="size-3.5 text-muted-foreground" aria-hidden />
                                    ) : (
                                        <CopyIcon className="size-3.5 text-muted-foreground" aria-hidden />
                                    )}
                                </Button>
                            </div>
                            <div className="flex flex-wrap gap-1.5">
                                <Badge variant="secondary">{counts.attrs} attrs</Badge>
                                <Badge variant="secondary">{counts.parents} parents</Badge>
                                <Badge variant="secondary">{counts.refs} referenced by</Badge>
                                <Badge variant="secondary">{counts.policies} policies</Badge>
                            </div>
                        </div>
                        <Tabs
                            value={tab}
                            onValueChange={value => {
                                if (value === 'overview' || value === 'relationships' || value === 'policies' || value === 'gapl') {
                                    setTab(value);
                                }
                            }}
                            className="flex min-h-0 flex-1 flex-col"
                        >
                            <TabsList variant="line" className="px-6">
                                <TabsTrigger value="overview">Overview</TabsTrigger>
                                <TabsTrigger value="relationships">Relationships</TabsTrigger>
                                <TabsTrigger value="policies">Policies</TabsTrigger>
                                <TabsTrigger value="gapl">GAPL shape</TabsTrigger>
                            </TabsList>
                            <div className="min-h-0 flex-1 overflow-y-auto px-6 py-4">
                                <TabsContent value="overview">
                                    <EntityOverviewTab entity={entity} />
                                </TabsContent>
                                <TabsContent value="relationships">
                                    <EntityRelationshipsTab entity={entity} allEntities={allEntities} />
                                </TabsContent>
                                <TabsContent value="policies">
                                    <EntityPoliciesTab entity={entity} policies={policies} />
                                </TabsContent>
                                <TabsContent value="gapl">
                                    <EntityGaplShapeTab entity={entity} />
                                </TabsContent>
                            </div>
                        </Tabs>
                    </>
                )}
            </SheetContent>
        </Sheet>
    );
}
