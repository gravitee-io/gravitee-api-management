import { Badge } from '@gravitee/graphene-core';
import type { PolicyStatus } from '../lib/api/authz-api.types';

const MAP: Record<PolicyStatus, { label: string; variant: 'default' | 'outline' | 'destructive' | 'secondary' }> = {
    DRAFT: { label: 'Draft', variant: 'outline' },
    DEPLOYED: { label: 'Deployed', variant: 'default' },
    DISABLED: { label: 'Disabled', variant: 'secondary' },
};

export function StatusBadge({ status }: { status: PolicyStatus }) {
    const { label, variant } = MAP[status];
    return <Badge variant={variant}>{label}</Badge>;
}
