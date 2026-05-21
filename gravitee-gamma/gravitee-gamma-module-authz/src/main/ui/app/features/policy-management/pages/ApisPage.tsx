import { ServicePolicyPage } from '../ServicePolicyPage';
import { apisServiceConfig } from '../service-defs/apis';

export function ApisPage() {
    return <ServicePolicyPage config={apisServiceConfig} />;
}
