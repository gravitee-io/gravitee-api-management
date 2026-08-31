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
    Alert,
    AlertDescription,
    Badge,
    buildLinearBreadcrumbs,
    Spinner,
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
    useLayoutConfig,
} from '@gravitee/graphene-core';
import { useMemo } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import { AgentDocs } from './AgentDocs';
import { AgentOverview } from './AgentOverview';
import { AgentChatTab, AgentSubscribeTab } from './AgentPlaceholders';
import { agentDetailBreadcrumbs, agentTabLabel, isAgentTab, pathForAgentTab, type AgentTab } from './tabs';
import { useAgent } from './useAgent';
import { protocolLabel } from '../catalog/protocol';

export function AgentDetailPage() {
    const { apiId, tab: tabParam } = useParams<{ apiId: string; tab?: string }>();
    const navigate = useNavigate();
    const { agent, loading, error } = useAgent(apiId);

    if (tabParam && !isAgentTab(tabParam)) {
        return <Navigate to={pathForAgentTab(apiId ?? '', 'overview')} replace />;
    }

    const activeTab: AgentTab = tabParam && isAgentTab(tabParam) ? tabParam : 'overview';

    return (
        <AgentDetailBody
            apiId={apiId}
            activeTab={activeTab}
            agent={agent}
            loading={loading}
            error={error}
            onTabChange={tab => {
                if (apiId) {
                    navigate(pathForAgentTab(apiId, tab));
                }
            }}
        />
    );
}

function AgentDetailBody({
    apiId,
    activeTab,
    agent,
    loading,
    error,
    onTabChange,
}: {
    apiId: string | undefined;
    activeTab: AgentTab;
    agent: ReturnType<typeof useAgent>['agent'];
    loading: boolean;
    error: string | null;
    onTabChange: (tab: AgentTab) => void;
}) {
    const navigate = useNavigate();
    const breadcrumbs = useMemo(() => {
        if (!agent || !apiId) {
            return [];
        }
        return buildLinearBreadcrumbs(navigate, agentDetailBreadcrumbs(apiId, agent.name, activeTab));
    }, [activeTab, agent, apiId, navigate]);

    useLayoutConfig({ breadcrumbs }, [breadcrumbs]);

    if (loading) {
        return (
            <div className="flex justify-center py-12">
                <Spinner className="size-8" aria-label="Loading agent" />
            </div>
        );
    }

    if (error || !agent || !apiId) {
        return (
            <Alert variant="destructive" role="alert">
                <AlertDescription>{error ?? 'This agent could not be found.'}</AlertDescription>
            </Alert>
        );
    }

    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                    <h1 className="text-lg font-semibold">{agent.name}</h1>
                    <Badge variant="secondary">{protocolLabel(agent.type)}</Badge>
                    <span className="text-sm text-muted-foreground">v{agent.version}</span>
                    {agent.categories?.map(category => (
                        <Badge key={category} variant="outline">
                            {category}
                        </Badge>
                    ))}
                </div>
                <p className="text-sm text-muted-foreground">{agent.description}</p>
                <p className="text-sm text-muted-foreground">
                    Publisher: {agent.owner.display_name ?? agent.owner.email ?? '—'}
                </p>
            </div>

            <Tabs
                value={activeTab}
                onValueChange={value => {
                    if (isAgentTab(value)) {
                        onTabChange(value);
                    }
                }}
            >
                <TabsList variant="line" aria-label="Agent sections">
                    <TabsTrigger value="overview">{agentTabLabel('overview')}</TabsTrigger>
                    <TabsTrigger value="docs">{agentTabLabel('docs')}</TabsTrigger>
                    <TabsTrigger value="subscribe">{agentTabLabel('subscribe')}</TabsTrigger>
                    <TabsTrigger value="chat">{agentTabLabel('chat')}</TabsTrigger>
                </TabsList>
                <TabsContent value="overview">
                    <AgentOverview api={agent} />
                </TabsContent>
                <TabsContent value="docs">
                    <AgentDocs apiId={apiId} />
                </TabsContent>
                <TabsContent value="subscribe">
                    <AgentSubscribeTab apiName={agent.name} />
                </TabsContent>
                <TabsContent value="chat">
                    <AgentChatTab apiId={apiId} />
                </TabsContent>
            </Tabs>
        </div>
    );
}
