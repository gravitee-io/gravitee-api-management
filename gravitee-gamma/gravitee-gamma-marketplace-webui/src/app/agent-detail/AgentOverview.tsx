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
import { Badge, Button, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@gravitee/graphene-core';
import { useNavigate } from 'react-router-dom';

import { gatewayEndpoint, mcpServerUrl, mcpTools } from './connection';
import { pathForAgentTab } from './tabs';
import type { Api } from '../../api/types';

export function AgentOverview({ api }: { api: Api }) {
    const navigate = useNavigate();
    const tools = mcpTools(api);
    const a2a = gatewayEndpoint(api);
    const mcp = mcpServerUrl(api);

    return (
        <div className="space-y-6">
            <section className="space-y-2">
                <h2 className="text-sm font-semibold">Skills / labels</h2>
                <div className="flex flex-wrap gap-1">
                    {api.labels && api.labels.length > 0 ? (
                        api.labels.map(label => (
                            <Badge key={label} variant="outline">
                                {label}
                            </Badge>
                        ))
                    ) : (
                        <p className="text-sm text-muted-foreground">No labels published for this agent.</p>
                    )}
                </div>
            </section>

            <section className="space-y-2">
                <h2 className="text-sm font-semibold">Connection</h2>
                <dl className="space-y-1 text-sm">
                    <div className="flex flex-wrap gap-2">
                        <dt className="w-24 text-muted-foreground">A2A endpoint</dt>
                        <dd className="font-mono break-all">{a2a ?? '—'}</dd>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        <dt className="w-24 text-muted-foreground">MCP server</dt>
                        <dd className="font-mono break-all">{mcp ?? '—'}</dd>
                    </div>
                </dl>
            </section>

            <section className="space-y-2">
                <h2 className="text-sm font-semibold">MCP tools</h2>
                {tools.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No tools published for this agent.</p>
                ) : (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Tool</TableHead>
                                <TableHead>Description</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {tools.map(tool => (
                                <TableRow key={tool.name}>
                                    <TableCell className="font-mono text-sm">{tool.name}</TableCell>
                                    <TableCell className="text-sm">{tool.description || '—'}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                )}
            </section>

            <div className="flex justify-end">
                <Button type="button" onClick={() => navigate(pathForAgentTab(api.id, 'subscribe'))}>
                    Subscribe to this agent
                </Button>
            </div>
        </div>
    );
}
