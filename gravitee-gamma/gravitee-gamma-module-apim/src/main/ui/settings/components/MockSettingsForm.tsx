/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Checkbox,
    Field,
    FieldDescription,
    FieldLabel,
    Input,
    Separator,
    Switch,
} from '@gravitee/graphene-core';

import type { SettingsNavItem } from '../mock/navigation-items';

interface MockSettingsFormProps {
    readonly item: SettingsNavItem;
    readonly disabled: boolean;
}

const FORM_CONFIGS: Record<string, FormConfig> = {
    dashboard: {
        title: 'Dashboard Configuration',
        description: 'Configure the overview dashboard widgets and layout.',
        fields: [
            { type: 'text', label: 'Dashboard Title', placeholder: 'My Organization', description: 'Displayed at the top of the dashboard.' },
            { type: 'switch', label: 'Show Activity Feed', description: 'Display recent activity on the dashboard.' },
            { type: 'text', label: 'Refresh Interval (seconds)', placeholder: '30' },
        ],
    },
    applications: {
        title: 'Application Settings',
        description: 'Control default behavior for applications in this environment.',
        fields: [
            { type: 'switch', label: 'Auto-validate Applications', description: 'Automatically approve new application registrations.' },
            { type: 'text', label: 'Default Application Type', placeholder: 'Web' },
            { type: 'checkbox', label: 'Require Application Description' },
        ],
    },
    users: {
        title: 'User Management',
        description: 'Configure user account settings for the organization.',
        fields: [
            { type: 'text', label: 'Max Login Attempts', placeholder: '5' },
            { type: 'text', label: 'Account Lockout Duration (min)', placeholder: '30' },
            { type: 'switch', label: 'Enable Self-Registration', description: 'Allow users to create their own accounts.' },
        ],
    },
    roles: {
        title: 'Role Configuration',
        description: 'Manage role definitions and default assignments.',
        fields: [
            { type: 'text', label: 'Default Role', placeholder: 'USER' },
            { type: 'switch', label: 'Allow Custom Roles', description: 'Let administrators create custom roles.' },
            { type: 'checkbox', label: 'Enforce Role Hierarchy' },
        ],
    },
    'user-groups': {
        title: 'User Group Settings',
        description: 'Configure user group behavior and defaults.',
        fields: [
            { type: 'text', label: 'Max Groups per User', placeholder: '10' },
            { type: 'switch', label: 'Auto-assign Default Group', description: 'New users are added to the default group.' },
            { type: 'text', label: 'Default Group Name', placeholder: 'Members' },
        ],
    },
    'identity-providers': {
        title: 'Identity Provider Configuration',
        description: 'Manage external identity providers for SSO.',
        fields: [
            { type: 'switch', label: 'Enable OIDC', description: 'Allow OpenID Connect authentication.' },
            { type: 'text', label: 'OIDC Issuer URL', placeholder: 'https://idp.example.com' },
            { type: 'switch', label: 'Enable SAML', description: 'Allow SAML 2.0 authentication.' },
        ],
    },
    authentication: {
        title: 'Authentication Settings',
        description: 'Configure organization-wide authentication policies.',
        fields: [
            { type: 'switch', label: 'Enforce MFA', description: 'Require multi-factor authentication for all users.' },
            { type: 'text', label: 'Session Timeout (min)', placeholder: '60' },
            { type: 'switch', label: 'Remember Me', description: 'Allow persistent sessions.' },
        ],
    },
    'user-fields': {
        title: 'Custom User Fields',
        description: 'Define additional fields displayed on user profiles.',
        fields: [
            { type: 'text', label: 'Field Name', placeholder: 'Department' },
            { type: 'switch', label: 'Required Field', description: 'Users must fill in this field.' },
            { type: 'checkbox', label: 'Visible on Registration Form' },
        ],
    },
    'primary-owner-mode': {
        title: 'Primary Owner Mode',
        description: 'Configure how primary ownership is assigned to APIs.',
        fields: [
            { type: 'text', label: 'Default Owner Mode', placeholder: 'USER' },
            { type: 'switch', label: 'Allow Transfer', description: 'Permit ownership transfer between users.' },
            { type: 'checkbox', label: 'Require Approval for Transfer' },
        ],
    },
    'api-behavior': {
        title: 'API Behavior Settings',
        description: 'Default runtime behavior for APIs in this environment.',
        fields: [
            { type: 'text', label: 'Default Timeout (ms)', placeholder: '30000' },
            { type: 'switch', label: 'Enable Rate Limiting', description: 'Apply default rate limits to new APIs.' },
            { type: 'text', label: 'Max Payload Size (KB)', placeholder: '1024' },
            { type: 'checkbox', label: 'Validate Request Headers' },
        ],
    },
    'api-logging': {
        title: 'API Logging Configuration',
        description: 'Configure logging levels and retention for API traffic.',
        fields: [
            { type: 'switch', label: 'Enable Request Logging', description: 'Log all incoming API requests.' },
            { type: 'text', label: 'Log Retention (days)', placeholder: '90' },
            { type: 'switch', label: 'Include Response Body', description: 'Capture response payloads in logs.' },
            { type: 'text', label: 'Max Log Entry Size (KB)', placeholder: '256' },
        ],
    },
    'dictionaries-metadata': {
        title: 'Dictionaries & Metadata',
        description: 'Manage shared dictionaries and metadata properties.',
        fields: [
            { type: 'text', label: 'Default Dictionary', placeholder: 'global-config' },
            { type: 'switch', label: 'Enable Dynamic Properties', description: 'Allow runtime property resolution.' },
            { type: 'text', label: 'Metadata Prefix', placeholder: 'gio_' },
        ],
    },
    'policy-groups': {
        title: 'Policy Group Settings',
        description: 'Configure shared policy groups applied across APIs.',
        fields: [
            { type: 'switch', label: 'Enable Global Policies', description: 'Apply organization-wide policies to all APIs.' },
            { type: 'text', label: 'Default Policy Timeout (ms)', placeholder: '5000' },
            { type: 'checkbox', label: 'Allow Policy Override per API' },
        ],
    },
    resources: {
        title: 'Shared Resources',
        description: 'Manage shared resources available to APIs.',
        fields: [
            { type: 'text', label: 'Resource Cache TTL (s)', placeholder: '300' },
            { type: 'switch', label: 'Enable Resource Pooling', description: 'Pool shared resource connections.' },
            { type: 'text', label: 'Max Pool Size', placeholder: '50' },
        ],
    },
    'api-scores': {
        title: 'API Scores (Labs)',
        description: 'Configure API quality scoring rules and thresholds.',
        fields: [
            { type: 'switch', label: 'Enable API Scoring', description: 'Assign quality scores to APIs.' },
            { type: 'text', label: 'Minimum Score Threshold', placeholder: '70' },
            { type: 'checkbox', label: 'Block Deployment Below Threshold' },
        ],
    },
    'entrypoints-sharding-tags': {
        title: 'Entrypoints & Sharding Tags',
        description: 'Configure gateway entrypoints and sharding tag assignments.',
        fields: [
            { type: 'text', label: 'Default Entrypoint', placeholder: 'https://api.example.com' },
            { type: 'text', label: 'Sharding Tag', placeholder: 'internal' },
            { type: 'switch', label: 'Enable Tag-based Routing', description: 'Route traffic based on sharding tags.' },
        ],
    },
    tenants: {
        title: 'Tenant Configuration',
        description: 'Manage tenant isolation and gateway assignment.',
        fields: [
            { type: 'text', label: 'Default Tenant', placeholder: 'default' },
            { type: 'switch', label: 'Enable Tenant Isolation', description: 'Strictly isolate API traffic by tenant.' },
            { type: 'text', label: 'Tenant Header Name', placeholder: 'X-Gravitee-Tenant' },
        ],
    },
    'gateway-policies': {
        title: 'Gateway Policy Configuration',
        description: 'Manage environment-level gateway policies.',
        fields: [
            { type: 'switch', label: 'Enable Request Transformation', description: 'Apply transformation policies at gateway level.' },
            { type: 'text', label: 'Policy Execution Order', placeholder: 'platform, api' },
            { type: 'checkbox', label: 'Log Policy Execution' },
        ],
    },
    'security-plan-types': {
        title: 'Security Plan Types',
        description: 'Configure available security plan types for APIs.',
        fields: [
            { type: 'switch', label: 'Enable API Key Plans', description: 'Allow API key-based security plans.' },
            { type: 'switch', label: 'Enable OAuth2 Plans', description: 'Allow OAuth2-based security plans.' },
            { type: 'switch', label: 'Enable JWT Plans', description: 'Allow JWT-based security plans.' },
        ],
    },
    management: {
        title: 'Management Settings',
        description: 'Organization-level management configuration.',
        fields: [
            { type: 'text', label: 'Organization Name', placeholder: 'Acme Corp' },
            { type: 'text', label: 'Support Email', placeholder: 'support@acme.com' },
            { type: 'switch', label: 'Maintenance Mode', description: 'Put the platform in read-only mode.' },
        ],
    },
    'client-registration': {
        title: 'Client Registration',
        description: 'Configure dynamic client registration settings.',
        fields: [
            { type: 'switch', label: 'Enable DCR', description: 'Allow dynamic client registration.' },
            { type: 'text', label: 'Registration Endpoint', placeholder: '/oidc/register' },
            { type: 'checkbox', label: 'Require Initial Access Token' },
        ],
    },
    'cors-settings': {
        title: 'CORS Settings',
        description: 'Configure Cross-Origin Resource Sharing defaults.',
        fields: [
            { type: 'text', label: 'Allowed Origins', placeholder: '*' },
            { type: 'text', label: 'Allowed Methods', placeholder: 'GET, POST, PUT, DELETE' },
            { type: 'text', label: 'Max Age (seconds)', placeholder: '3600' },
            { type: 'checkbox', label: 'Allow Credentials' },
        ],
    },
    'smtp-schedulers': {
        title: 'SMTP & Schedulers',
        description: 'Configure email sending and background scheduler settings.',
        fields: [
            { type: 'text', label: 'SMTP Host', placeholder: 'smtp.example.com' },
            { type: 'text', label: 'SMTP Port', placeholder: '587' },
            { type: 'switch', label: 'Enable TLS', description: 'Use TLS for SMTP connections.' },
            { type: 'text', label: 'Scheduler Interval (s)', placeholder: '60' },
        ],
    },
    notifications: {
        title: 'Notification Settings',
        description: 'Configure notification channels and triggers.',
        fields: [
            { type: 'switch', label: 'Email Notifications', description: 'Send notifications via email.' },
            { type: 'switch', label: 'Webhook Notifications', description: 'Send notifications via webhooks.' },
            { type: 'text', label: 'Webhook URL', placeholder: 'https://hooks.example.com/notify' },
        ],
    },
    'audit-logs': {
        title: 'Audit Log Settings',
        description: 'Configure audit logging for organizational events.',
        fields: [
            { type: 'switch', label: 'Enable Audit Logging', description: 'Record all administrative actions.' },
            { type: 'text', label: 'Retention Period (days)', placeholder: '365' },
            { type: 'checkbox', label: 'Include User Agent' },
        ],
    },
    'environment-audit': {
        title: 'Environment Audit (Labs)',
        description: 'Configure environment-level audit trail settings.',
        fields: [
            { type: 'switch', label: 'Enable Environment Audit', description: 'Track changes within this environment.' },
            { type: 'text', label: 'Audit Retention (days)', placeholder: '180' },
            { type: 'checkbox', label: 'Audit API Deployments' },
        ],
    },
    alerts: {
        title: 'Alerts (Labs)',
        description: 'Configure alerting rules and notification thresholds.',
        fields: [
            { type: 'switch', label: 'Enable Alerting', description: 'Activate the alerting engine.' },
            { type: 'text', label: 'Alert Cooldown (min)', placeholder: '15' },
            { type: 'text', label: 'Max Alerts per Hour', placeholder: '100' },
        ],
    },
};

interface FormField {
    readonly type: 'text' | 'switch' | 'checkbox';
    readonly label: string;
    readonly placeholder?: string;
    readonly description?: string;
}

interface FormConfig {
    readonly title: string;
    readonly description: string;
    readonly fields: readonly FormField[];
}

const FALLBACK_CONFIG: FormConfig = {
    title: 'Settings',
    description: 'Configure this resource.',
    fields: [
        { type: 'text', label: 'Name', placeholder: 'Enter a value' },
        { type: 'switch', label: 'Enabled', description: 'Toggle this setting on or off.' },
    ],
};

export function MockSettingsForm({ item, disabled }: MockSettingsFormProps) {
    const config = FORM_CONFIGS[item.key] ?? FALLBACK_CONFIG;

    return (
        <Card>
            <CardHeader>
                <CardTitle>{config.title}</CardTitle>
                <CardDescription>{config.description}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                {config.fields.map((field, idx) => (
                    <Field key={field.label} orientation="vertical">
                        {field.type === 'text' && (
                            <>
                                <FieldLabel>{field.label}</FieldLabel>
                                {field.description && <FieldDescription>{field.description}</FieldDescription>}
                                <Input placeholder={field.placeholder} disabled={disabled} />
                            </>
                        )}
                        {field.type === 'switch' && (
                            <div className="flex items-center justify-between">
                                <div>
                                    <FieldLabel>{field.label}</FieldLabel>
                                    {field.description && (
                                        <FieldDescription>{field.description}</FieldDescription>
                                    )}
                                </div>
                                <Switch disabled={disabled} defaultChecked={idx % 2 === 0} />
                            </div>
                        )}
                        {field.type === 'checkbox' && (
                            <div className="flex items-center gap-2">
                                <Checkbox disabled={disabled} defaultChecked={idx % 2 === 0} />
                                <FieldLabel className="mb-0">{field.label}</FieldLabel>
                            </div>
                        )}
                    </Field>
                ))}
                <Separator />
                <div className="flex justify-end">
                    <Button type="button" disabled={disabled}>
                        Save Changes
                    </Button>
                </div>
            </CardContent>
        </Card>
    );
}
