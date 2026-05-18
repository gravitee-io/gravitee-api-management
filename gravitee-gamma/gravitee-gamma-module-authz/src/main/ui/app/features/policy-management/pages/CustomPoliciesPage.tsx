import { ServicePolicyPage } from '../ServicePolicyPage';
import { customServiceConfig } from '../service-defs/custom';

export function CustomPoliciesPage() {
    return <ServicePolicyPage config={customServiceConfig} />;
}
