import { Server } from 'lucide-react';
import type { ServicePageConfig } from '../ServicePolicyPage';

export const mcpServiceConfig: ServicePageConfig = {
    type: 'MCP',
    title: 'MCP Policies',
    description: 'Grant and restrict what principals can do on each MCP Server, its tools, prompts, and resources.',
    createButtonLabel: 'Create Policy for MCP',
    searchPlaceholder: 'Search MCP policies…',
    icon: Server,
    hasTarget: true,
    targetPickerVariant: 'default',
    targetPickerTitle: 'Create policy for MCP',
    targetPickerDescription: 'Pick an MCP Server from the catalog. Servers that already have a policy are hidden.',
    targetPickerEmptyState: 'Every catalog MCP Server already has a policy. Edit an existing one from the list.',
    targetPickerSearchPlaceholder: 'Search MCP servers…',
    serviceLabel: 'MCP',
    resourceGroups: [
        { key: 'MCPServer', label: 'MCP Server' },
        { key: 'MCPTool', label: 'Tools' },
        { key: 'MCPPrompt', label: 'Prompts' },
        { key: 'MCPResource', label: 'Resources' },
    ],
    conditionSnippets: [
        { label: 'Business hours', snippet: 'context.time.hour >= 9 && context.time.hour < 17' },
        { label: 'Trusted device', snippet: 'context.device.trusted == true' },
        { label: 'Corporate IP range', snippet: 'context.source.ip.in_cidr("10.0.0.0/8")' },
    ],
};
