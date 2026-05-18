import { SlidersHorizontal } from 'lucide-react';
import type { ChipOption } from '../PolicyStatementCard';
import type { ServicePageConfig } from '../ServicePolicyPage';

const seedResources: readonly ChipOption[] = [
    { id: 'app.billing-ui', group: 'Application', label: 'billing-ui' },
    { id: 'app.reporting-dashboard', group: 'Application', label: 'reporting-dashboard' },
    { id: 'app.ops-console', group: 'Application', label: 'ops-console' },
    { id: 'asset.customer-db', group: 'Asset', label: 'customer-db' },
    { id: 'asset.pii-bucket', group: 'Asset', label: 'pii-bucket' },
    { id: 'asset.warehouse', group: 'Asset', label: 'warehouse' },
    { id: 'resource.feature-flags', group: 'Resource', label: 'feature-flags' },
    { id: 'resource.kms-keys', group: 'Resource', label: 'kms-keys' },
];

const conditionSnippets: readonly { label: string; snippet: string }[] = [
    { label: 'Corporate IP range', snippet: 'context.source.ip.in_cidr("10.0.0.0/8")' },
    { label: 'MFA required', snippet: 'context.auth.mfa == true' },
    { label: 'Business hours', snippet: 'context.time.hour >= 9 && context.time.hour < 17' },
    { label: 'Owner only', snippet: 'resource.owner == principal' },
];

export const customServiceConfig: ServicePageConfig = {
    type: 'CUSTOM',
    title: 'Custom Policies',
    description:
        'Write policies against anything that is not already routed as an MCP, API, Agent, LLM or Event — internal applications, data assets, and bespoke resources.',
    createButtonLabel: 'Create Custom Policy',
    searchPlaceholder: 'Search custom policies…',
    icon: SlidersHorizontal,
    hasTarget: false,
    serviceLabel: 'Custom',
    resourceGroups: [
        { key: 'Application', label: 'Application' },
        { key: 'Asset', label: 'Asset' },
        { key: 'Resource', label: 'Resource' },
    ],
    resourceOptions: seedResources,
    conditionSnippets,
};
