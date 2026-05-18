import { ServicePolicyPage } from '../ServicePolicyPage';
import { eventsServiceConfig } from '../service-defs/events';

export function EventsPage() {
    return <ServicePolicyPage config={eventsServiceConfig} />;
}
