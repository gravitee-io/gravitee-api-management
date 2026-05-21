import { ServicePolicyPage } from '../ServicePolicyPage';
import { llmsServiceConfig } from '../service-defs/llms';

export function LlmsPage() {
    return <ServicePolicyPage config={llmsServiceConfig} />;
}
