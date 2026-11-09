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
import { Badge, Button, Card, CardContent, CardFooter, CardHeader } from '@gravitee/graphene-core';
import { useId } from 'react';
import { Link } from 'react-router-dom';

import { PortalLogo } from './PortalLogo';
import { ViewPortalLink } from './ViewPortalLink';
import type { Portal } from '../types/portal';

interface PortalTileProps {
    readonly portal: Portal;
    readonly canConfigure: boolean;
    readonly overviewPath: string;
}

export function PortalTile({ portal, canConfigure, overviewPath }: PortalTileProps) {
    const titleId = useId();

    return (
        <Card role="article" aria-labelledby={titleId} className="min-w-0 gap-4">
            <CardHeader>
                <div className="flex items-center gap-3 min-w-0">
                    <PortalLogo name={portal.name} src={portal.logoUrl} />
                    <div className="min-w-0 space-y-1">
                        <h3 id={titleId} className="text-sm font-medium break-words">
                            {portal.name}
                        </h3>
                        {portal.default ? <Badge variant="secondary">Default</Badge> : null}
                        <p className="text-sm text-muted-foreground break-all">{portal.url || 'No public URL yet'}</p>
                    </div>
                </div>
            </CardHeader>
            <CardContent>
                <dl>
                    <dt className="text-sm text-muted-foreground">Published APIs</dt>
                    <dd className="text-base font-semibold">{portal.publishedApisCount}</dd>
                </dl>
            </CardContent>
            <CardFooter className="mt-auto flex flex-wrap justify-end gap-2">
                <ViewPortalLink url={portal.url} portalName={portal.name} />
                {canConfigure ? (
                    <Button variant="ghost" size="sm" asChild>
                        <Link to={overviewPath} aria-label={`Configure ${portal.name}`}>
                            Configure
                        </Link>
                    </Button>
                ) : null}
            </CardFooter>
        </Card>
    );
}
