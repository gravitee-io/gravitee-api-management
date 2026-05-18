import { ServicePolicyPage } from '../ServicePolicyPage';
import { agentsServiceConfig } from '../service-defs/agents';

export function AgentsPage() {
    return <ServicePolicyPage config={agentsServiceConfig} />;
}
