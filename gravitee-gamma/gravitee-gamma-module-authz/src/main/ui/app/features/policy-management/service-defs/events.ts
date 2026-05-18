import { Radio } from 'lucide-react';
import type { ServicePageConfig } from '../ServicePolicyPage';

const conditionSnippets: readonly { label: string; snippet: string }[] = [
    { label: 'Key prefix matches tenant', snippet: 'context.message.key.starts_with(principal.tenant)' },
    { label: 'Schema version >= 3', snippet: 'context.message.schema_version >= 3' },
    { label: 'Partition in range', snippet: 'context.message.partition in [0, 1, 2, 3]' },
    { label: 'Payload field filter', snippet: 'context.message.payload.amount < 10000' },
];

export const eventsServiceConfig: ServicePageConfig = {
    type: 'EVENT',
    title: 'Event Policies',
    description: 'Control who can publish, subscribe, consume or replay across event streams, individual topics and schema fields.',
    createButtonLabel: 'Create Policy for Event',
    searchPlaceholder: 'Search event policies…',
    icon: Radio,
    hasTarget: true,
    targetPickerVariant: 'default',
    targetPickerTitle: 'Create policy for Event stream',
    targetPickerDescription: 'Pick an event stream from the catalog. Streams that already have a policy are hidden.',
    targetPickerEmptyState: 'Every catalog event stream already has a policy. Edit an existing one from the list.',
    targetPickerSearchPlaceholder: 'Search event streams…',
    serviceLabel: 'Event',
    resourceGroups: [
        { key: 'EventStream', label: 'Event stream' },
        { key: 'Topic', label: 'Topic' },
        { key: 'SchemaField', label: 'Schema field' },
    ],
    conditionSnippets,
};
