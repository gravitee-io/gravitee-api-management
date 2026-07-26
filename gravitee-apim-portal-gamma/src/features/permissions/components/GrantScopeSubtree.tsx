/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import {
    Badge,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { ExternalLinkIcon, FileTextIcon, FolderOpenIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import type { PortalNavigationItem } from '../../portals/types/navigation-item.types';
import type { OverrideSelection } from '../hooks/useGroupGrants';
import type { PortalAccessGrant } from '../types/permissions.types';
import { flattenGrantScopeTree } from '../utils/resolve-nav-item-access';

const OVERRIDE_OPTIONS: readonly { value: OverrideSelection; label: string }[] = [
    { value: 'INHERIT', label: 'Inherited' },
    { value: 'VIEW', label: 'View' },
    { value: 'CONSUME', label: 'Consume' },
    { value: 'NONE', label: 'No access' },
];

function NavItemIcon({ item }: { readonly item: PortalNavigationItem }) {
    if (item.type === 'FOLDER') {
        return <FolderOpenIcon className="size-4 text-muted-foreground" aria-hidden />;
    }
    if (item.type === 'LINK') {
        return <ExternalLinkIcon className="size-4 text-muted-foreground" aria-hidden />;
    }
    return <FileTextIcon className="size-4 text-muted-foreground" aria-hidden />;
}

interface GrantScopeSubtreeProps {
    readonly grant: PortalAccessGrant;
    readonly grants: readonly PortalAccessGrant[];
    readonly navigationItems: readonly PortalNavigationItem[];
    readonly portalNameById: ReadonlyMap<string, string>;
    readonly readOnly: boolean;
    readonly onOverrideChange: (
        navigationItemId: string,
        portalId: string,
        access: OverrideSelection,
    ) => Promise<void>;
}

export function GrantScopeSubtree({
    grant,
    grants,
    navigationItems,
    portalNameById,
    readOnly,
    onOverrideChange,
}: GrantScopeSubtreeProps) {
    const rows = useMemo(
        () => flattenGrantScopeTree(grant, navigationItems, grants),
        [grant, grants, navigationItems],
    );

    const showsMultiplePortals = useMemo(
        () => new Set(rows.map(row => row.item.portalId)).size > 1,
        [rows],
    );

    if (rows.length === 0) {
        return (
            <p className="px-4 py-3 text-sm text-muted-foreground">
                This scope has no navigation items yet. Pages added later inherit{' '}
                {grant.access === 'CONSUME' ? 'Consume' : 'View'} automatically.
            </p>
        );
    }

    return (
        <div className="divide-y">
            {rows.map(({ item, depth, resolved }) => (
                <div key={item.id} className="flex items-center gap-3 px-4 py-2">
                    <div
                        className="flex min-w-0 flex-1 items-center gap-2"
                        style={{ paddingLeft: `${depth * 1.25}rem` }}
                    >
                        <NavItemIcon item={item} />
                        <span className="min-w-0 truncate text-sm">{item.title}</span>
                        {showsMultiplePortals && (
                            <span className="shrink-0 text-xs text-muted-foreground">
                                {portalNameById.get(item.portalId) ?? item.portalId}
                            </span>
                        )}
                        {resolved.inherited ? (
                            <Badge variant="outline" className="shrink-0">
                                Inherited
                            </Badge>
                        ) : (
                            <Badge variant="highlight" className="shrink-0">
                                Override
                            </Badge>
                        )}
                    </div>

                    <Select
                        value={resolved.inherited ? 'INHERIT' : resolved.access}
                        disabled={readOnly}
                        onValueChange={value => {
                            void onOverrideChange(item.id, item.portalId, value as OverrideSelection);
                        }}
                    >
                        <SelectTrigger className="h-8 w-36" aria-label={`Access for ${item.title}`}>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {OVERRIDE_OPTIONS.filter(
                                option => option.value !== 'CONSUME' || grant.access === 'CONSUME',
                            ).map(option => (
                                <SelectItem key={option.value} value={option.value}>
                                    {option.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            ))}
        </div>
    );
}
