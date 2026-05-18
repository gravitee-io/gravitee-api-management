import { Bot } from 'lucide-react';
import type { ServicePageConfig } from '../ServicePolicyPage';

export const agentsServiceConfig: ServicePageConfig = {
    type: 'AGENT',
    title: 'Agent Policies',
    description:
        'Define who may chat, delegate to, or invoke capabilities on each agent — and which of its skills, tools, memory or knowledge sources.',
    createButtonLabel: 'Create Policy for Agent',
    searchPlaceholder: 'Search agent policies…',
    icon: Bot,
    hasTarget: true,
    targetPickerVariant: 'default',
    targetPickerTitle: 'Create policy for Agent',
    targetPickerDescription: 'Pick an agent from the catalog. Agents that already have a policy are hidden.',
    targetPickerEmptyState: 'Every catalog agent already has a policy. Edit an existing one from the list.',
    targetPickerSearchPlaceholder: 'Search agents…',
    serviceLabel: 'Agent',
    resourceGroups: [
        { key: 'Agent', label: 'Agent' },
        { key: 'AgentSkill', label: 'Skills' },
        { key: 'AgentTool', label: 'Tools' },
        { key: 'AgentMemory', label: 'Memory' },
        { key: 'AgentKnowledge', label: 'Knowledge' },
    ],
    conditionSnippets: [
        { label: 'Same tenant', snippet: 'resource.tenant == principal.tenant' },
        { label: 'Premium plan', snippet: 'context.user.plan in ["premium", "enterprise"]' },
        { label: 'Within agent budget', snippet: 'context.agent.tokens_used < context.agent.budget' },
        { label: 'Human-in-the-loop on', snippet: 'context.session.hitl == true' },
    ],
};
