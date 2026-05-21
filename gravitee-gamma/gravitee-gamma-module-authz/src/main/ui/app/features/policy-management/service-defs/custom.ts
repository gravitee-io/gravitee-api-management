import { SlidersHorizontal } from 'lucide-react';
import type { ServicePageConfig } from '../ServicePolicyPage';

const conditionSnippets: readonly { label: string; snippet: string }[] = [
    { label: 'Corporate IP range', snippet: 'context.source.ip.in_cidr("10.0.0.0/8")' },
    { label: 'MFA required', snippet: 'context.auth.mfa == true' },
    { label: 'Business hours', snippet: 'context.time.hour >= 9 && context.time.hour < 17' },
    { label: 'Owner only', snippet: 'resource.owner == principal' },
];

// `resourceGroups` and `resourceOptions` are intentionally omitted here: for
// Custom policies they are derived at runtime in ServicePolicyPage from the
// live entity catalog (everything that is not a principal or action). The
// previous static seed list was prototype scaffolding — it has been removed
// so the picker reflects only entities the user actually created or that
// were synced from APIM.
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
    conditionSnippets,
};
