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
import { Alert, AlertDescription, Button, Card, DataTableEmptyState, Skeleton } from '@gravitee/graphene-core';
import { GlobeIcon } from '@gravitee/graphene-core/icons';
import { Link } from 'react-router-dom';

import { FEATURED_AGENT_COUNT } from './useFeaturedAgents';
import type { Api } from '../../api/types';
import { pathForAgentTab } from '../agent-detail/tabs';
import { AgentCard } from '../catalog/AgentCard';

export function FeaturedAgents({
    agents,
    loading,
    error,
}: {
    agents: readonly Api[];
    loading: boolean;
    error: string | null;
}) {
    return (
        <section className="space-y-3">
            <h2 className="text-lg font-semibold">Featured</h2>
            {error ? (
                <Alert variant="destructive" role="alert">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            ) : null}
            {loading ? (
                <div role="status" aria-label="Loading featured agents" className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {Array.from({ length: FEATURED_AGENT_COUNT }, (_, index) => (
                        <Card key={index} className="p-4">
                            <Skeleton className="mb-3 h-5 w-40" />
                            <Skeleton className="mb-2 h-4 w-16" />
                            <Skeleton className="h-12 w-full" />
                        </Card>
                    ))}
                </div>
            ) : null}
            {!loading && !error && agents.length === 0 ? (
                <div className="rounded-lg border">
                    <DataTableEmptyState
                        variant="first-use"
                        icon={<GlobeIcon />}
                        title="No agents yet"
                        description="Published agents will appear here."
                        primaryAction={
                            <Button asChild>
                                <Link to="/catalog">Browse catalog</Link>
                            </Button>
                        }
                    />
                </div>
            ) : null}
            {!loading && !error && agents.length > 0 ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {agents.map(agent => (
                        <AgentCard
                            key={agent.id}
                            api={agent}
                            footer={
                                <div className="px-4 pb-4">
                                    <Button asChild size="sm" className="w-full">
                                        <Link
                                            to={pathForAgentTab(agent.id, 'subscribe')}
                                            aria-label={`Subscribe to ${agent.name}`}
                                        >
                                            Subscribe
                                        </Link>
                                    </Button>
                                </div>
                            }
                        />
                    ))}
                </div>
            ) : null}
        </section>
    );
}
