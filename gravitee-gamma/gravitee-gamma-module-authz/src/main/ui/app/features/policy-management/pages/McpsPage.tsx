import { ServicePolicyPage } from '../ServicePolicyPage';
import { mcpServiceConfig } from '../service-defs/mcp';

export function McpsPage() {
    return <ServicePolicyPage config={mcpServiceConfig} />;
}
