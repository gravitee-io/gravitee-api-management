/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { BlockNoteDocument, PageContent, PortalNavigationItem } from '../types';
import { DEFAULT_TILE_TEMPLATE, serializeTileTemplate } from '../../blocks/ApiCatalogBlock/tile-template';
import { saveNavItem } from './navigation-items.storage';
import { savePageContent } from './page-contents.storage';

// ---------------------------------------------------------------------------
// Block helpers — keep document definitions readable
// ---------------------------------------------------------------------------

type Block = Record<string, unknown>;

function text(t: string, styles: Record<string, boolean> = {}): Record<string, unknown> {
    return { type: 'text', text: t, styles };
}

function heading(level: number, t: string): Block {
    return { type: 'heading', props: { level }, content: [text(t)], children: [] };
}

function paragraph(...parts: string[]): Block {
    return { type: 'paragraph', content: parts.map(p => text(p)), children: [] };
}

function richParagraph(...parts: Array<{ t: string; bold?: boolean; italic?: boolean }>): Block {
    return {
        type: 'paragraph',
        content: parts.map(p => text(p.t, { ...(p.bold ? { bold: true } : {}), ...(p.italic ? { italic: true } : {}) })),
        children: [],
    };
}

function bulletItem(t: string): Block {
    return { type: 'bulletListItem', content: [text(t)], children: [] };
}

function numberedItem(t: string): Block {
    return { type: 'numberedListItem', content: [text(t)], children: [] };
}

function spacer(): Block {
    return { type: 'paragraph', content: [], children: [] };
}

interface BannerButton { label: string; link: string; variant: 'primary' | 'secondary' | 'outline' }

function banner(title: string, subtitle: string, variant: string, buttons: BannerButton[] = []): Block {
    return {
        type: 'graviteeBanner',
        props: { title, subtitle, variant, buttons: JSON.stringify(buttons), backgroundImage: '', height: '0' },
        content: [],
        children: [],
    };
}

interface SectionItem { icon: string; title: string; description: string; buttonLabel?: string; buttonLink?: string }

function section(title: string, subtitle: string, variant: string, columns: string, items: SectionItem[]): Block {
    return {
        type: 'graviteeSection',
        props: { title, subtitle, variant, columns, items: JSON.stringify(items), height: '0', contentWidth: 'auto' },
        content: [],
        children: [],
    };
}

function card(title: string, subtitle: string, icon = 'book', color = 'white'): Block {
    return { type: 'graviteeCard', props: { title, subtitle, icon, color }, content: [], children: [] };
}

function button(label: string, link: string, appearance: 'filled' | 'outlined' | 'text' = 'filled'): Block {
    return { type: 'graviteeButton', props: { label, link, appearance }, content: [], children: [] };
}

function apiList(title: string, viewMode = 'cards'): Block {
    return {
        type: 'graviteeApiCatalog',
        props: { title, viewMode, tileTemplate: serializeTileTemplate(DEFAULT_TILE_TEMPLATE) },
        content: [],
        children: [],
    };
}

function subscriptionFlow(): Block {
    return { type: 'graviteeSubscriptionFlow', props: { apiId: '' }, content: [], children: [] };
}

function subscriptionViewer(): Block {
    return { type: 'graviteeSubscriptionViewer', props: {}, content: [], children: [] };
}

function applications(): Block {
    return { type: 'graviteeApplications', props: {}, content: [], children: [] };
}

// ---------------------------------------------------------------------------
// OpenAPI specs
// ---------------------------------------------------------------------------

const PAYMENT_API_SPEC = JSON.stringify({
    openapi: '3.0.3',
    info: {
        title: 'Payment Processing API',
        version: '2.1.0',
        description: 'Create, authorize, capture, and manage payments across multiple payment methods and currencies.',
        contact: { name: 'Payments Team', email: 'payments-api@example.com' },
    },
    servers: [
        { url: 'https://api.payments.example.com/v2', description: 'Production' },
        { url: 'https://sandbox.payments.example.com/v2', description: 'Sandbox' },
    ],
    security: [{ apiKey: [] }],
    tags: [{ name: 'Payments', description: 'Payment lifecycle operations' }],
    paths: {
        '/payments': {
            post: {
                operationId: 'createPayment',
                summary: 'Create a payment',
                description: 'Initialize a new payment with the specified amount, currency, and payment method. Include an Idempotency-Key header to prevent duplicate charges.',
                tags: ['Payments'],
                parameters: [{ name: 'Idempotency-Key', in: 'header', required: true, schema: { type: 'string', format: 'uuid' }, description: 'Unique key to ensure idempotent processing' }],
                requestBody: { required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/CreatePaymentRequest' } } } },
                responses: {
                    '201': { description: 'Payment created', content: { 'application/json': { schema: { $ref: '#/components/schemas/Payment' } } } },
                    '400': { description: 'Invalid request', content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } } },
                    '401': { description: 'Unauthorized' },
                    '409': { description: 'Idempotency conflict — a different request body was sent with the same key' },
                    '429': { description: 'Rate limit exceeded' },
                },
            },
            get: {
                operationId: 'listPayments',
                summary: 'List payments',
                description: 'Retrieve a paginated list of payments, optionally filtered by status or date range.',
                tags: ['Payments'],
                parameters: [
                    { name: 'limit', in: 'query', schema: { type: 'integer', default: 20, maximum: 100 }, description: 'Number of results per page' },
                    { name: 'offset', in: 'query', schema: { type: 'integer', default: 0 } },
                    { name: 'status', in: 'query', schema: { type: 'string', enum: ['pending', 'authorized', 'captured', 'failed', 'cancelled'] } },
                    { name: 'created_after', in: 'query', schema: { type: 'string', format: 'date-time' } },
                    { name: 'created_before', in: 'query', schema: { type: 'string', format: 'date-time' } },
                ],
                responses: {
                    '200': {
                        description: 'Paginated list of payments',
                        content: { 'application/json': { schema: { type: 'object', properties: { data: { type: 'array', items: { $ref: '#/components/schemas/Payment' } }, total: { type: 'integer' }, limit: { type: 'integer' }, offset: { type: 'integer' } } } } },
                    },
                },
            },
        },
        '/payments/{id}': {
            get: {
                operationId: 'getPayment',
                summary: 'Get payment details',
                description: 'Retrieve the full details of a specific payment by ID.',
                tags: ['Payments'],
                parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' }, description: 'Payment ID' }],
                responses: {
                    '200': { description: 'Payment details', content: { 'application/json': { schema: { $ref: '#/components/schemas/Payment' } } } },
                    '404': { description: 'Payment not found' },
                },
            },
        },
        '/payments/{id}/capture': {
            post: {
                operationId: 'capturePayment',
                summary: 'Capture a payment',
                description: 'Capture an authorized payment. Supports partial captures by specifying an amount less than the authorized total.',
                tags: ['Payments'],
                parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' } }],
                requestBody: {
                    content: { 'application/json': { schema: { type: 'object', properties: { amount: { type: 'integer', description: 'Amount to capture in minor units. Omit for full capture.' } } } } },
                },
                responses: {
                    '200': { description: 'Payment captured', content: { 'application/json': { schema: { $ref: '#/components/schemas/Payment' } } } },
                    '400': { description: 'Payment cannot be captured (wrong status)' },
                    '404': { description: 'Payment not found' },
                },
            },
        },
        '/payments/{id}/cancel': {
            post: {
                operationId: 'cancelPayment',
                summary: 'Cancel a payment',
                description: 'Cancel a pending or authorized payment. Captured payments must be refunded instead.',
                tags: ['Payments'],
                parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' } }],
                responses: {
                    '200': { description: 'Payment cancelled', content: { 'application/json': { schema: { $ref: '#/components/schemas/Payment' } } } },
                    '400': { description: 'Payment cannot be cancelled' },
                },
            },
        },
    },
    components: {
        schemas: {
            CreatePaymentRequest: {
                type: 'object',
                required: ['amount', 'currency', 'payment_method'],
                properties: {
                    amount: { type: 'integer', description: 'Payment amount in minor units (e.g. 5000 = $50.00)', example: 5000 },
                    currency: { type: 'string', description: 'ISO 4217 currency code', example: 'USD', minLength: 3, maxLength: 3 },
                    payment_method: {
                        type: 'object',
                        required: ['type', 'token'],
                        properties: {
                            type: { type: 'string', enum: ['card', 'bank_transfer', 'wallet'], description: 'Payment method type' },
                            token: { type: 'string', description: 'Tokenized payment method identifier' },
                        },
                    },
                    description: { type: 'string', description: 'Optional payment description', example: 'Order #12345' },
                    customer_email: { type: 'string', format: 'email', description: 'Customer email for receipt' },
                    metadata: { type: 'object', additionalProperties: { type: 'string' }, description: 'Custom key-value metadata' },
                    capture: { type: 'boolean', default: true, description: 'Whether to capture immediately or authorize only' },
                },
            },
            Payment: {
                type: 'object',
                properties: {
                    id: { type: 'string', example: 'pay_2wB9xKqM7nP4' },
                    amount: { type: 'integer', example: 5000 },
                    amount_captured: { type: 'integer', example: 5000 },
                    currency: { type: 'string', example: 'USD' },
                    status: { type: 'string', enum: ['pending', 'authorized', 'captured', 'failed', 'cancelled'] },
                    payment_method: { type: 'object', properties: { type: { type: 'string' }, last_four: { type: 'string' }, brand: { type: 'string' } } },
                    description: { type: 'string' },
                    customer_email: { type: 'string' },
                    metadata: { type: 'object', additionalProperties: { type: 'string' } },
                    failure_reason: { type: 'string', nullable: true },
                    created_at: { type: 'string', format: 'date-time' },
                    updated_at: { type: 'string', format: 'date-time' },
                },
            },
            Error: {
                type: 'object',
                properties: {
                    code: { type: 'string', example: 'invalid_request' },
                    message: { type: 'string', example: 'The amount field is required' },
                    details: { type: 'array', items: { type: 'object', properties: { field: { type: 'string' }, message: { type: 'string' } } } },
                },
            },
        },
        securitySchemes: {
            apiKey: { type: 'apiKey', name: 'X-API-Key', in: 'header', description: 'Your API key from the Dashboard' },
        },
    },
});

const REFUNDS_API_SPEC = JSON.stringify({
    openapi: '3.0.3',
    info: {
        title: 'Refunds API',
        version: '1.3.0',
        description: 'Create and manage full or partial refunds for captured payments. Supports multiple refund reasons and automatic customer notifications.',
        contact: { name: 'Payments Team', email: 'payments-api@example.com' },
    },
    servers: [
        { url: 'https://api.payments.example.com/v2', description: 'Production' },
        { url: 'https://sandbox.payments.example.com/v2', description: 'Sandbox' },
    ],
    security: [{ apiKey: [] }],
    tags: [{ name: 'Refunds', description: 'Refund management operations' }],
    paths: {
        '/refunds': {
            post: {
                operationId: 'createRefund',
                summary: 'Create a refund',
                description: 'Issue a full or partial refund for a previously captured payment. Refunds are processed asynchronously; use webhooks or polling to track status.',
                tags: ['Refunds'],
                parameters: [{ name: 'Idempotency-Key', in: 'header', required: true, schema: { type: 'string', format: 'uuid' } }],
                requestBody: { required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/CreateRefundRequest' } } } },
                responses: {
                    '201': { description: 'Refund created', content: { 'application/json': { schema: { $ref: '#/components/schemas/Refund' } } } },
                    '400': { description: 'Invalid request or payment not refundable' },
                    '404': { description: 'Payment not found' },
                    '422': { description: 'Refund amount exceeds remaining capturable amount' },
                },
            },
            get: {
                operationId: 'listRefunds',
                summary: 'List refunds',
                description: 'Retrieve a paginated list of refunds, optionally filtered by payment or status.',
                tags: ['Refunds'],
                parameters: [
                    { name: 'payment_id', in: 'query', schema: { type: 'string' }, description: 'Filter by payment ID' },
                    { name: 'status', in: 'query', schema: { type: 'string', enum: ['pending', 'processed', 'failed'] } },
                    { name: 'limit', in: 'query', schema: { type: 'integer', default: 20, maximum: 100 } },
                    { name: 'offset', in: 'query', schema: { type: 'integer', default: 0 } },
                ],
                responses: {
                    '200': {
                        description: 'Paginated list of refunds',
                        content: { 'application/json': { schema: { type: 'object', properties: { data: { type: 'array', items: { $ref: '#/components/schemas/Refund' } }, total: { type: 'integer' } } } } },
                    },
                },
            },
        },
        '/refunds/{id}': {
            get: {
                operationId: 'getRefund',
                summary: 'Get refund details',
                description: 'Retrieve the full details of a specific refund.',
                tags: ['Refunds'],
                parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' } }],
                responses: {
                    '200': { description: 'Refund details', content: { 'application/json': { schema: { $ref: '#/components/schemas/Refund' } } } },
                    '404': { description: 'Refund not found' },
                },
            },
        },
    },
    components: {
        schemas: {
            CreateRefundRequest: {
                type: 'object',
                required: ['payment_id'],
                properties: {
                    payment_id: { type: 'string', description: 'ID of the captured payment to refund', example: 'pay_2wB9xKqM7nP4' },
                    amount: { type: 'integer', description: 'Refund amount in minor units. Omit for full refund.', example: 2500 },
                    reason: { type: 'string', enum: ['duplicate', 'fraudulent', 'requested_by_customer', 'other'], description: 'Reason for the refund' },
                    notify_customer: { type: 'boolean', default: true, description: 'Send email notification to the customer' },
                    metadata: { type: 'object', additionalProperties: { type: 'string' } },
                },
            },
            Refund: {
                type: 'object',
                properties: {
                    id: { type: 'string', example: 'ref_8kT3vNqR5mX2' },
                    payment_id: { type: 'string', example: 'pay_2wB9xKqM7nP4' },
                    amount: { type: 'integer', example: 2500 },
                    currency: { type: 'string', example: 'USD' },
                    status: { type: 'string', enum: ['pending', 'processed', 'failed'] },
                    reason: { type: 'string' },
                    failure_reason: { type: 'string', nullable: true },
                    created_at: { type: 'string', format: 'date-time' },
                    processed_at: { type: 'string', format: 'date-time', nullable: true },
                },
            },
        },
        securitySchemes: {
            apiKey: { type: 'apiKey', name: 'X-API-Key', in: 'header' },
        },
    },
});

// ---------------------------------------------------------------------------
// Page documents
// ---------------------------------------------------------------------------

function gettingStartedDocument(): BlockNoteDocument {
    return [
        banner(
            'Payments Developer Portal',
            'Process payments, manage refunds, and build financial applications with our secure, scalable API platform.',
            'gradient',
            [
                { label: 'Browse APIs', link: '/catalog', variant: 'primary' },
                { label: 'Quick Start', link: '/guides/quick-start-nav004', variant: 'secondary' },
            ],
        ),
        spacer(),
        section(
            'Build with Confidence',
            'Enterprise-grade payment infrastructure trusted by thousands of businesses worldwide.',
            'none',
            '3',
            [
                { icon: 'shield', title: 'PCI DSS Level 1', description: 'Fully certified payment processing. Card data is protected with end-to-end encryption and tokenization.' },
                { icon: 'globe', title: 'Global Coverage', description: 'Accept payments in 135+ currencies across 45 countries with automatic conversion and local payment methods.' },
                { icon: 'rocket', title: '99.99% Uptime', description: 'Enterprise-grade infrastructure with automatic failover, real-time monitoring, and instant horizontal scaling.' },
            ],
        ),
        spacer(),
        section(
            'Platform Capabilities',
            'Everything you need to manage the complete payment lifecycle.',
            'dark',
            '4',
            [
                { icon: 'code', title: 'RESTful APIs', description: 'Clean, intuitive endpoints with SDKs for Python, Node.js, Go, Java, and Ruby.', buttonLabel: 'API Catalog', buttonLink: '/api-reference-nav002' },
                { icon: 'key', title: 'Flexible Auth', description: 'API keys, OAuth 2.0, and mutual TLS. Granular scopes and IP allowlisting.', buttonLabel: 'Auth Guide', buttonLink: '/guides/authentication-nav005' },
                { icon: 'server', title: 'Webhooks', description: 'Real-time event notifications for payments, refunds, disputes, and account changes.', buttonLabel: 'Webhook Guide', buttonLink: '/guides/webhooks-nav008' },
                { icon: 'monitor', title: 'Dashboard', description: 'Monitor transactions, track revenue, analyze trends, and manage your integration.' },
            ],
        ),
        spacer(),
        apiList('Popular APIs', 'cards'),
        spacer(),
        button('View Complete API Catalog', '/api-reference-nav002', 'filled'),
    ];
}

function apiReferenceDocument(): BlockNoteDocument {
    return [
        heading(1, 'API Catalog'),
        paragraph('Browse our complete collection of payment APIs. Each API includes interactive documentation, code examples, and sandbox environments for testing.'),
        spacer(),
        apiList('Available APIs', 'cards'),
        spacer(),
        section(
            'Quick Links',
            'Not sure where to start? These guides will help you get up and running.',
            'light',
            '3',
            [
                { icon: 'book', title: 'Quick Start', description: 'New to our platform? Make your first API call in under 5 minutes with our step-by-step guide.', buttonLabel: 'Get Started', buttonLink: '/guides/quick-start-nav004' },
                { icon: 'key', title: 'Authentication', description: 'Learn about API keys, OAuth 2.0, and other methods for securing your API requests.', buttonLabel: 'Auth Guide', buttonLink: '/guides/authentication-nav005' },
                { icon: 'shield', title: 'Error Handling', description: 'Understand error codes, retry strategies, and best practices for resilient integrations.', buttonLabel: 'Error Guide', buttonLink: '/guides/error-handling-nav009' },
            ],
        ),
    ];
}

function quickStartDocument(): BlockNoteDocument {
    return [
        heading(1, 'Quick Start Guide'),
        paragraph('Get up and running with the Payments API in under 5 minutes. This guide walks you through creating your first payment from scratch.'),
        spacer(),
        heading(2, 'Prerequisites'),
        bulletItem('An active developer account with API access enabled'),
        bulletItem('Your API key pair (available in the Dashboard under Settings \u2192 API Keys)'),
        bulletItem('A tool for making HTTP requests (cURL, Postman, or any HTTP client)'),
        spacer(),
        heading(2, 'Step 1 \u2014 Create an Application'),
        paragraph('Applications represent your integration and hold your API credentials. Each application can subscribe to multiple APIs with different plans.'),
        numberedItem('Navigate to the Applications page and click "Create Application"'),
        numberedItem('Enter a name and description for your application'),
        numberedItem('Select the APIs you want to subscribe to'),
        numberedItem('Choose a subscription plan that fits your needs'),
        spacer(),
        heading(2, 'Step 2 \u2014 Subscribe to the Payment API'),
        paragraph('Once your application is created, subscribe to the Payment Processing API to get access. Choose a plan below to get started:'),
        spacer(),
        subscriptionFlow(),
        spacer(),
        heading(2, 'Step 3 \u2014 Make Your First Payment'),
        paragraph('With your subscription active, send a POST request to the /payments endpoint. Include your API key in the X-API-Key header and set Content-Type to application/json.'),
        richParagraph(
            { t: 'The request body should include the ' },
            { t: 'amount', bold: true },
            { t: ' (in minor units, e.g. 5000 = $50.00), ' },
            { t: 'currency', bold: true },
            { t: ' (ISO 4217 code), ' },
            { t: 'payment_method', bold: true },
            { t: ', and an ' },
            { t: 'Idempotency-Key', bold: true },
            { t: ' header to prevent duplicate charges.' },
        ),
        spacer(),
        heading(2, 'Step 4 \u2014 Verify the Payment'),
        paragraph('After creating a payment, use the GET /payments/{id} endpoint to check its status at any time. Possible statuses:'),
        bulletItem('pending \u2014 Payment is being processed by the payment network'),
        bulletItem('authorized \u2014 Funds are reserved and ready for capture'),
        bulletItem('captured \u2014 Funds have been captured successfully'),
        bulletItem('failed \u2014 Payment was declined or encountered an error'),
        spacer(),
        banner(
            'Ready for Production?',
            'Switch from sandbox to production API keys in your Dashboard settings. All endpoints and behaviors are identical between environments.',
            'light',
            [
                { label: 'Go to Dashboard', link: '/dashboard', variant: 'primary' },
                { label: 'View API Reference', link: '/api-reference-nav002', variant: 'secondary' },
            ],
        ),
    ];
}

function authenticationDocument(): BlockNoteDocument {
    return [
        heading(1, 'Authentication'),
        paragraph('All API requests must be authenticated. We support multiple authentication methods to fit different use cases and security requirements.'),
        spacer(),
        section(
            '',
            '',
            'none',
            '3',
            [
                { icon: 'shield', title: 'API Keys', description: 'Simple key-based authentication for server-to-server integrations. Include your key in the X-API-Key header.' },
                { icon: 'key', title: 'OAuth 2.0', description: 'Token-based authentication with granular scopes. Ideal for applications acting on behalf of users.' },
                { icon: 'globe', title: 'Mutual TLS', description: 'Certificate-based authentication for the highest level of security. Required for some regulated industries.' },
            ],
        ),
        spacer(),
        heading(2, 'API Key Authentication'),
        paragraph('API keys are the simplest way to authenticate. Generate a key pair in your Dashboard, then include it in every request using the X-API-Key header.'),
        bulletItem('Keep your secret key confidential \u2014 never expose it in client-side code or commit it to version control'),
        bulletItem('Use environment variables to store keys in your application'),
        bulletItem('Rotate keys regularly through the Dashboard without downtime'),
        bulletItem('Each key can be scoped to specific APIs and operations'),
        spacer(),
        heading(2, 'OAuth 2.0'),
        paragraph('OAuth 2.0 provides token-based authentication with fine-grained access control. Use the Client Credentials flow for machine-to-machine communication, or the Authorization Code flow for user-facing applications.'),
        numberedItem('Register your application to obtain a client_id and client_secret'),
        numberedItem('Request an access token from the /oauth/token endpoint'),
        numberedItem('Include the Bearer token in the Authorization header of your API requests'),
        numberedItem('Refresh tokens before expiry to maintain uninterrupted access'),
        spacer(),
        heading(2, 'Rate Limits'),
        paragraph('API requests are rate-limited based on your subscription plan. Rate limit headers are included in every response:'),
        bulletItem('X-RateLimit-Limit \u2014 Maximum requests allowed per window'),
        bulletItem('X-RateLimit-Remaining \u2014 Requests remaining in the current window'),
        bulletItem('X-RateLimit-Reset \u2014 Unix timestamp when the window resets'),
        spacer(),
        banner(
            'Security Best Practices',
            'Never log or expose API keys. Use HTTPS for all requests. Implement IP allowlisting for production environments. Enable webhook signature verification.',
            'indigo',
            [{ label: 'View Security Guide', link: '/guides/authentication-nav005', variant: 'primary' }],
        ),
    ];
}

function processingPaymentsDocument(): BlockNoteDocument {
    return [
        heading(1, 'Processing Payments'),
        paragraph('Learn how to create, authorize, capture, and manage payments through our API. This guide covers the complete payment lifecycle and all supported payment methods.'),
        spacer(),
        section(
            'Supported Payment Methods',
            'Accept payments however your customers prefer to pay.',
            'none',
            '3',
            [
                { icon: 'code', title: 'Card Payments', description: 'Visa, Mastercard, Amex, Discover, and more. Full support for 3D Secure, recurring payments, and saved cards.' },
                { icon: 'database', title: 'Bank Transfers', description: 'ACH, SEPA, and wire transfers. Lower fees and higher limits, ideal for B2B and large transactions.' },
                { icon: 'globe', title: 'Digital Wallets', description: 'Apple Pay, Google Pay, and PayPal. One-tap checkout for higher conversion rates.' },
            ],
        ),
        spacer(),
        heading(2, 'Payment Lifecycle'),
        paragraph('Every payment goes through a series of states from creation to settlement. Understanding this lifecycle helps you build robust payment flows.'),
        numberedItem('Create \u2014 Initialize a payment with amount, currency, and payment method details'),
        numberedItem('Authorize \u2014 The payment processor verifies the method and reserves funds on the customer\'s account'),
        numberedItem('Capture \u2014 Capture the authorized funds (supports partial captures). This initiates the actual fund transfer'),
        numberedItem('Settle \u2014 Funds are transferred to your merchant account, typically within 1\u20132 business days'),
        spacer(),
        heading(2, 'Idempotency'),
        paragraph('Payment operations are critical financial transactions. Always include an Idempotency-Key header with a unique UUID for every payment creation request. This prevents duplicate charges if a request is retried due to network issues or timeouts.'),
        spacer(),
        banner(
            'Idempotency is Critical',
            'Always include an Idempotency-Key header when creating payments. Without it, retried requests may result in duplicate charges to your customers.',
            'dark',
            [{ label: 'View API Reference', link: '/payment-api-nav010', variant: 'primary' }],
        ),
        spacer(),
        heading(2, 'Multi-Currency Support'),
        paragraph('Process payments in 135+ currencies with automatic conversion. Specify the currency using ISO 4217 codes (e.g., USD, EUR, GBP). Exchange rates are updated in real-time and applied at the time of capture.'),
        spacer(),
        heading(2, 'Testing in Sandbox'),
        paragraph('Use the sandbox environment to test your integration without processing real transactions. Sandbox API keys are available in your Dashboard under Settings. Use test card numbers to simulate different outcomes:'),
        bulletItem('4242 4242 4242 4242 \u2014 Successful payment'),
        bulletItem('4000 0000 0000 0002 \u2014 Card declined'),
        bulletItem('4000 0000 0000 9995 \u2014 Insufficient funds'),
        bulletItem('4000 0000 0000 0069 \u2014 Expired card'),
        spacer(),
        button('View Payment API Reference', '/payment-api-nav010', 'filled'),
    ];
}

function webhooksDocument(): BlockNoteDocument {
    return [
        heading(1, 'Handling Webhooks'),
        paragraph('Webhooks deliver real-time notifications about events in your payment flow. Instead of polling our API, receive instant updates when payments are completed, refunds are processed, or disputes are opened.'),
        spacer(),
        heading(2, 'Setting Up Webhooks'),
        numberedItem('Configure a publicly accessible HTTPS endpoint in your application to receive webhook events'),
        numberedItem('Register the endpoint URL in your Dashboard under Settings \u2192 Webhooks'),
        numberedItem('Select the event types you want to receive notifications for'),
        numberedItem('Test the integration using the Dashboard\'s webhook testing tool'),
        numberedItem('Deploy your endpoint and monitor incoming events'),
        spacer(),
        section(
            'Event Types',
            'Subscribe to the events that matter to your integration.',
            'gray',
            '3',
            [
                { icon: 'code', title: 'Payment Events', description: 'payment.created, payment.authorized, payment.captured, payment.failed, payment.cancelled' },
                { icon: 'database', title: 'Refund Events', description: 'refund.created, refund.processed, refund.failed' },
                { icon: 'shield', title: 'Dispute Events', description: 'dispute.opened, dispute.evidence_required, dispute.resolved, dispute.lost' },
            ],
        ),
        spacer(),
        heading(2, 'Verifying Webhook Signatures'),
        paragraph('Every webhook request includes an X-Webhook-Signature header containing an HMAC-SHA256 signature. Verify this signature using your webhook secret to ensure the request is authentic and has not been tampered with.'),
        numberedItem('Extract the X-Webhook-Signature header from the incoming request'),
        numberedItem('Compute the HMAC-SHA256 hash of the raw request body using your webhook secret key'),
        numberedItem('Compare the computed hash with the signature from the header using a constant-time comparison'),
        numberedItem('Reject the request and return a 401 if the signatures do not match'),
        spacer(),
        banner(
            'Always Verify Signatures',
            'Never process webhook events without verifying the HMAC signature. Unverified webhooks could be forged by malicious actors to trigger unauthorized actions.',
            'dark',
            [{ label: 'Security Best Practices', link: '/guides/authentication-nav005', variant: 'primary' }],
        ),
        spacer(),
        heading(2, 'Retry Policy'),
        paragraph('If your endpoint returns a non-2xx status code or times out (30-second limit), we retry with exponential backoff. Webhooks are retried up to 5 times over a 24-hour period. After all retries are exhausted, the event is marked as failed in your Dashboard.'),
        spacer(),
        heading(2, 'Best Practices'),
        bulletItem('Return a 200 status code immediately upon receiving the webhook, then process the event asynchronously'),
        bulletItem('Implement idempotent event processing \u2014 the same event may be delivered more than once'),
        bulletItem('Log all incoming webhook events for debugging and audit purposes'),
        bulletItem('Set up monitoring and alerts for webhook delivery failures in your Dashboard'),
        bulletItem('Use a message queue to buffer events during high-traffic periods'),
    ];
}

function errorHandlingDocument(): BlockNoteDocument {
    return [
        heading(1, 'Error Handling'),
        paragraph('Our APIs use standard HTTP status codes and return structured error responses. This guide covers error categories, common codes, and strategies for building resilient integrations.'),
        spacer(),
        section(
            'Error Categories',
            '',
            'light',
            '3',
            [
                { icon: 'code', title: 'Client Errors (4xx)', description: 'Invalid requests, authentication failures, or resource not found. Fix the request before retrying.' },
                { icon: 'server', title: 'Server Errors (5xx)', description: 'Internal errors and service unavailability. These are temporary \u2014 retry with exponential backoff.' },
                { icon: 'shield', title: 'Payment Errors', description: 'Declined transactions, insufficient funds, and fraud detection. Handle based on the specific decline reason.' },
            ],
        ),
        spacer(),
        heading(2, 'Common Error Codes'),
        paragraph('Each error response includes a code, message, and details field. Here are the most common error codes:'),
        bulletItem('invalid_request \u2014 The request body is malformed or missing required fields'),
        bulletItem('authentication_failed \u2014 Invalid or expired API key or access token'),
        bulletItem('insufficient_funds \u2014 The payment method does not have enough funds'),
        bulletItem('card_declined \u2014 The card issuer declined the transaction'),
        bulletItem('rate_limit_exceeded \u2014 Too many requests; wait and retry after the reset window'),
        bulletItem('idempotency_conflict \u2014 A different request body was sent with the same idempotency key'),
        bulletItem('payment_method_expired \u2014 The card or payment method has expired'),
        bulletItem('fraud_detected \u2014 The transaction was flagged by the fraud detection system'),
        spacer(),
        heading(2, 'Retry Strategy'),
        paragraph('Not all errors should be retried. Follow these guidelines:'),
        numberedItem('4xx errors \u2014 Do not retry. Fix the request based on the error message and code'),
        numberedItem('429 (Rate Limited) \u2014 Retry after the duration specified in the Retry-After header'),
        numberedItem('5xx errors \u2014 Retry with exponential backoff: 1s, 2s, 4s, 8s, up to 5 retries maximum'),
        numberedItem('Network errors \u2014 Retry with backoff, always using idempotency keys to prevent duplicate operations'),
        spacer(),
        banner(
            'Need Help?',
            'If you encounter persistent errors or unexpected behavior, our support team is available 24/7 to help diagnose and resolve issues.',
            'indigo',
            [
                { label: 'Contact Support', link: 'https://support.example.com', variant: 'primary' },
                { label: 'Status Page', link: 'https://status.example.com', variant: 'secondary' },
            ],
        ),
    ];
}

function subscribeDocument(): BlockNoteDocument {
    return [
        heading(1, 'Subscribe to an API'),
        paragraph('Select an API and plan to get started. Once subscribed, you will receive API keys specific to your subscription that you can use to make authenticated requests.'),
        spacer(),
        subscriptionFlow(),
        spacer(),
        section(
            'Subscription Plans',
            'Choose the plan that fits your needs. Upgrade or downgrade at any time.',
            'light',
            '3',
            [
                { icon: 'rocket', title: 'Free Tier', description: 'Perfect for development and testing. 1,000 requests per month with full sandbox access. No credit card required.' },
                { icon: 'code', title: 'Growth', description: 'For production applications. 100,000 requests per month with priority support and 99.9% SLA guarantee.' },
                { icon: 'shield', title: 'Enterprise', description: 'Unlimited requests, dedicated support, custom SLAs, advanced security, and a dedicated account manager.' },
            ],
        ),
    ];
}

function subscriptionsDocument(): BlockNoteDocument {
    return [
        heading(1, 'My Subscriptions'),
        paragraph('View and manage your active API subscriptions. Monitor usage, rotate API keys, and upgrade or cancel plans from this dashboard.'),
        spacer(),
        subscriptionViewer(),
    ];
}

function applicationsDocument(): BlockNoteDocument {
    return [
        heading(1, 'My Applications'),
        paragraph('Manage your applications and their API subscriptions. Each application has its own set of credentials and can be configured independently for different environments or services.'),
        spacer(),
        applications(),
    ];
}

// ---------------------------------------------------------------------------
// Navigation items & page contents
// ---------------------------------------------------------------------------

function navId(portalId: string, key: string): string {
    return `${portalId}-${key}`;
}

function createRichNavItems(portalId: string): PortalNavigationItem[] {
    const guidesId = navId(portalId, 'nav-guides');

    return [
        { id: navId(portalId, 'nav-processing-payments'), portalId, title: 'Processing Payments', type: 'PAGE', parentId: guidesId, order: 2, slug: 'processing-payments-nav007' },
        { id: navId(portalId, 'nav-webhooks'), portalId, title: 'Handling Webhooks', type: 'PAGE', parentId: guidesId, order: 3, slug: 'webhooks-nav008' },
        { id: navId(portalId, 'nav-error-handling'), portalId, title: 'Error Handling', type: 'PAGE', parentId: guidesId, order: 4, slug: 'error-handling-nav009' },
        {
            id: navId(portalId, 'nav-payment-api'), portalId, title: 'Payment Processing API', type: 'PAGE',
            contentType: 'OPENAPI', renderer: 'swagger', specSource: { type: 'INLINE', content: PAYMENT_API_SPEC },
            parentId: null, order: 4, slug: 'payment-api-nav010',
        },
        {
            id: navId(portalId, 'nav-refunds-api'), portalId, title: 'Refunds API', type: 'PAGE',
            contentType: 'OPENAPI', renderer: 'swagger', specSource: { type: 'INLINE', content: REFUNDS_API_SPEC },
            parentId: null, order: 5, slug: 'refunds-api-nav011',
        },
        { id: navId(portalId, 'nav-subscribe'), portalId, title: 'Subscribe', type: 'PAGE', parentId: null, order: 6, slug: 'subscribe-nav012' },
        { id: navId(portalId, 'nav-subscriptions'), portalId, title: 'My Subscriptions', type: 'PAGE', parentId: null, order: 7, slug: 'subscriptions-nav013' },
        { id: navId(portalId, 'nav-applications'), portalId, title: 'My Applications', type: 'PAGE', parentId: null, order: 8, slug: 'applications-nav014' },
    ];
}

function createRichPageContents(portalId: string, richNavItems: readonly PortalNavigationItem[]): PageContent[] {
    const documentMap: Record<string, () => BlockNoteDocument> = {
        'nav-getting-started': gettingStartedDocument,
        'nav-api-reference': apiReferenceDocument,
        'nav-quick-start': quickStartDocument,
        'nav-authentication': authenticationDocument,
        'nav-processing-payments': processingPaymentsDocument,
        'nav-webhooks': webhooksDocument,
        'nav-error-handling': errorHandlingDocument,
        'nav-subscribe': subscribeDocument,
        'nav-subscriptions': subscriptionsDocument,
        'nav-applications': applicationsDocument,
    };

    const blockPages: PageContent[] = Object.entries(documentMap).map(([key, docFn]) => ({
        id: `page-content-${navId(portalId, key)}`,
        portalId,
        navigationItemId: navId(portalId, key),
        contentType: 'BLOCK' as const,
        document: docFn(),
    }));

    const openApiPages: PageContent[] = richNavItems
        .filter((item): item is PortalNavigationItem & { contentType: 'OPENAPI'; renderer: 'swagger'; specSource: { type: 'INLINE'; content: string } } =>
            item.type === 'PAGE' && 'contentType' in item && item.contentType === 'OPENAPI',
        )
        .map(item => ({
            id: `page-content-${item.id}`,
            portalId,
            navigationItemId: item.id,
            contentType: 'OPENAPI' as const,
            renderer: item.renderer,
            specContent: item.specSource.content,
        }));

    return [...blockPages, ...openApiPages];
}

// ---------------------------------------------------------------------------
// Public seed function
// ---------------------------------------------------------------------------

export async function seedRichPaymentPages(portalId: string): Promise<void> {
    const richNavItems = createRichNavItems(portalId);
    const richPageContents = createRichPageContents(portalId, richNavItems);

    await Promise.all(richNavItems.map(item => saveNavItem(item)));
    await Promise.all(richPageContents.map(content => savePageContent(content)));
}
