import { Globe } from 'lucide-react';
import type { ServicePageConfig } from '../ServicePolicyPage';

const conditionSnippets: readonly { label: string; snippet: string }[] = [
    { label: 'Corporate IP range', snippet: 'context.source.ip.in_cidr("10.0.0.0/8")' },
    { label: 'Scope present', snippet: 'context.auth.scopes.contains("orders:read")' },
    { label: 'Rate < 100/min', snippet: 'context.rate.per_minute(principal) < 100' },
    { label: 'Tenant match', snippet: 'context.request.header.x_tenant == principal.tenant' },
];

export const apisServiceConfig: ServicePageConfig = {
    type: 'API',
    title: 'API Policies',
    description: 'Control which principals can reach each API and its endpoints, and what data fields they may see.',
    createButtonLabel: 'Create Policy for API',
    searchPlaceholder: 'Search API policies…',
    icon: Globe,
    hasTarget: true,
    targetPickerVariant: 'default',
    targetPickerTitle: 'Create policy for API',
    targetPickerDescription: 'Pick an API from the catalog. APIs that already have a policy are hidden.',
    targetPickerEmptyState: 'Every catalog API already has a policy. Edit an existing one from the list.',
    targetPickerSearchPlaceholder: 'Search APIs…',
    serviceLabel: 'API',
    resourceGroups: [
        { key: 'API', label: 'API' },
        { key: 'Endpoint', label: 'Endpoints' },
        { key: 'DataField', label: 'Data Fields' },
    ],
    conditionSnippets,
};
