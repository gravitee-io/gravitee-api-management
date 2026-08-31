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
import { Badge, Card, CardContent, CardDescription, CardHeader, CardTitle } from '@gravitee/graphene-core';
import { Link } from 'react-router-dom';

import { isMcpAgent, protocolLabel } from './protocol';
import type { Api } from '../../api/types';

export function AgentCard({ api }: { api: Api }) {
    return (
        <Link
            to={`/catalog/${api.id}`}
            className="block h-full rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
            <Card className="h-full transition-colors hover:bg-accent/40">
                <CardHeader className="space-y-2">
                    <div className="flex items-start justify-between gap-2">
                        <CardTitle className="text-base">{api.name}</CardTitle>
                        <div className="flex shrink-0 flex-wrap justify-end gap-1">
                            <Badge variant="secondary">{protocolLabel(api.type)}</Badge>
                            {isMcpAgent(api) ? <Badge variant="outline">MCP</Badge> : null}
                        </div>
                    </div>
                    <p className="text-xs text-muted-foreground">v{api.version}</p>
                </CardHeader>
                <CardContent className="space-y-3">
                    <CardDescription className="line-clamp-2">{api.description}</CardDescription>
                    <div className="flex flex-wrap gap-1">
                        {api.categories?.map(category => (
                            <Badge key={`category-${category}`} variant="outline">
                                {category}
                            </Badge>
                        ))}
                        {api.labels?.map(label => (
                            <Badge key={`label-${label}`} variant="outline">
                                {label}
                            </Badge>
                        ))}
                    </div>
                </CardContent>
            </Card>
        </Link>
    );
}
