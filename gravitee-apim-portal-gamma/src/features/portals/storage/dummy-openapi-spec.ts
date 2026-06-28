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

/**
 * Rich OpenAPI 3.0.3 document used for portal demos, Swagger/Redoc rendering,
 * and editor validation fixtures. Intentionally verbose to exercise tags,
 * security schemes, polymorphic schemas, callbacks, links, and error models.
 */
export const DETAILED_DUMMY_OPENAPI_SPEC = JSON.stringify(
    {
        openapi: '3.0.3',
        info: {
            title: 'Gravitee Commerce Platform API',
            version: '3.2.1',
            description: [
                'The **Gravitee Commerce Platform API** lets you manage products, inventory, customers,',
                'orders, and fulfillment workflows programmatically.',
                '',
                '## Getting started',
                '',
                '1. Create an application in the Developer Portal.',
                '2. Subscribe to the Commerce API plan to receive credentials.',
                '3. Use the sandbox base URL while integrating; switch to production when ready.',
                '',
                '## Idempotency',
                '',
                'All `POST` operations that create resources accept an `Idempotency-Key` header.',
                'Reuse the same key with an identical body to safely retry after network failures.',
                '',
                '## Rate limits',
                '',
                '| Tier       | Requests / minute | Burst |',
                '|------------|-------------------|-------|',
                '| Sandbox    | 120               | 20    |',
                '| Standard   | 600               | 60    |',
                '| Enterprise | 3,000             | 200   |',
            ].join('\n'),
            termsOfService: 'https://example.com/terms',
            contact: {
                name: 'Commerce API Support',
                url: 'https://developer.example.com/support',
                email: 'commerce-api@example.com',
            },
            license: {
                name: 'Apache 2.0',
                url: 'https://www.apache.org/licenses/LICENSE-2.0',
            },
            'x-logo': {
                url: 'https://www.gravitee.io/images/gravitee-logo-cyan.svg',
                altText: 'Gravitee',
            },
        },
        externalDocs: {
            description: 'Full integration guides and SDK reference',
            url: 'https://developer.example.com/docs/commerce',
        },
        servers: [
            { url: 'https://api.commerce.example.com/v3', description: 'Production (global)' },
            { url: 'https://api.eu.commerce.example.com/v3', description: 'Production (EU)' },
            { url: 'https://sandbox.commerce.example.com/v3', description: 'Sandbox' },
        ],
        security: [{ oauth2: ['commerce.read'] }],
        tags: [
            { name: 'Products', description: 'Catalog and merchandising' },
            { name: 'Inventory', description: 'Stock levels and reservations' },
            { name: 'Customers', description: 'Customer profiles and addresses' },
            { name: 'Orders', description: 'Order lifecycle and fulfillment' },
            { name: 'Webhooks', description: 'Event subscriptions and delivery logs' },
            { name: 'Analytics', description: 'Aggregated commerce metrics' },
        ],
        paths: {
            '/products': {
                get: {
                    operationId: 'listProducts',
                    summary: 'List products',
                    description: 'Returns a cursor-paginated list of products. Supports full-text search, category filters, and sorting.',
                    tags: ['Products'],
                    parameters: [
                        { $ref: '#/components/parameters/PageSize' },
                        { $ref: '#/components/parameters/PageCursor' },
                        {
                            name: 'q',
                            in: 'query',
                            description: 'Full-text search across name, SKU, and description',
                            schema: { type: 'string', minLength: 2, maxLength: 120 },
                            example: 'wireless headset',
                        },
                        {
                            name: 'category',
                            in: 'query',
                            schema: { type: 'string', enum: ['electronics', 'apparel', 'home', 'services'] },
                        },
                        {
                            name: 'status',
                            in: 'query',
                            schema: { type: 'string', enum: ['draft', 'active', 'archived'], default: 'active' },
                        },
                        {
                            name: 'sort',
                            in: 'query',
                            schema: { type: 'string', enum: ['created_at', '-created_at', 'name', '-name', 'price'], default: '-created_at' },
                        },
                        { $ref: '#/components/parameters/AcceptLanguage' },
                    ],
                    responses: {
                        '200': {
                            description: 'Paginated product list',
                            headers: {
                                'X-Request-Id': { $ref: '#/components/headers/XRequestId' },
                                'X-RateLimit-Remaining': { schema: { type: 'integer' } },
                            },
                            content: {
                                'application/json': {
                                    schema: { $ref: '#/components/schemas/ProductList' },
                                    examples: {
                                        default: {
                                            summary: 'Two active products',
                                            value: {
                                                data: [
                                                    {
                                                        id: 'prod_7hK2mNqR9vX1',
                                                        sku: 'WH-1000XM5',
                                                        name: 'Aurora Wireless Headset',
                                                        status: 'active',
                                                        base_price: { amount: 34999, currency: 'USD' },
                                                        categories: ['electronics'],
                                                        created_at: '2025-11-04T14:22:11Z',
                                                    },
                                                ],
                                                pagination: { next_cursor: 'eyJpZCI6InByb2RfN2hLMm1OcVI5dlgxIn0', has_more: true },
                                            },
                                        },
                                    },
                                },
                            },
                        },
                        '400': { $ref: '#/components/responses/BadRequest' },
                        '401': { $ref: '#/components/responses/Unauthorized' },
                        '429': { $ref: '#/components/responses/RateLimited' },
                    },
                },
                post: {
                    operationId: 'createProduct',
                    summary: 'Create a product',
                    description: 'Creates a new catalog product. Physical and digital products use different required fields via a discriminated schema.',
                    tags: ['Products'],
                    security: [{ oauth2: ['commerce.write'] }],
                    parameters: [
                        { $ref: '#/components/parameters/IdempotencyKey' },
                        { $ref: '#/components/parameters/AcceptLanguage' },
                    ],
                    requestBody: {
                        required: true,
                        content: {
                            'application/json': {
                                schema: { $ref: '#/components/schemas/CreateProductRequest' },
                                examples: {
                                    physical: {
                                        summary: 'Physical product',
                                        value: {
                                            type: 'physical',
                                            sku: 'BK-LEATHER-001',
                                            name: 'Heritage Leather Notebook',
                                            description: 'Hand-stitched A5 notebook with 240 gsm paper.',
                                            base_price: { amount: 4200, currency: 'USD' },
                                            categories: ['home'],
                                            dimensions: { weight_grams: 480, length_mm: 210, width_mm: 148, height_mm: 18 },
                                            shipping_class: 'standard',
                                        },
                                    },
                                    digital: {
                                        summary: 'Digital product',
                                        value: {
                                            type: 'digital',
                                            sku: 'COURSE-REACT-101',
                                            name: 'React Foundations Course',
                                            description: '12-hour self-paced video course with exercises.',
                                            base_price: { amount: 9900, currency: 'USD' },
                                            categories: ['services'],
                                            download_url_ttl_seconds: 86400,
                                            license_type: 'single_user',
                                        },
                                    },
                                },
                            },
                        },
                    },
                    callbacks: {
                        onProductPublished: {
                            '{$request.body#/webhook_url}': {
                                post: {
                                    summary: 'Product published notification',
                                    requestBody: {
                                        content: {
                                            'application/json': {
                                                schema: { $ref: '#/components/schemas/WebhookEvent' },
                                            },
                                        },
                                    },
                                    responses: { '200': { description: 'Webhook accepted' } },
                                },
                            },
                        },
                    },
                    responses: {
                        '201': {
                            description: 'Product created',
                            headers: { Location: { schema: { type: 'string', format: 'uri' } } },
                            content: {
                                'application/json': {
                                    schema: { $ref: '#/components/schemas/Product' },
                                },
                            },
                            links: {
                                GetProduct: {
                                    operationId: 'getProduct',
                                    parameters: { id: '$response.body#/id' },
                                },
                                ListInventory: {
                                    operationId: 'listInventory',
                                    parameters: { product_id: '$response.body#/id' },
                                },
                            },
                        },
                        '400': { $ref: '#/components/responses/BadRequest' },
                        '401': { $ref: '#/components/responses/Unauthorized' },
                        '403': { $ref: '#/components/responses/Forbidden' },
                        '409': { $ref: '#/components/responses/Conflict' },
                        '422': { $ref: '#/components/responses/UnprocessableEntity' },
                    },
                },
            },
            '/products/{id}': {
                parameters: [{ $ref: '#/components/parameters/ProductId' }],
                get: {
                    operationId: 'getProduct',
                    summary: 'Retrieve a product',
                    tags: ['Products'],
                    responses: {
                        '200': {
                            description: 'Product details',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/Product' } },
                            },
                        },
                        '404': { $ref: '#/components/responses/NotFound' },
                    },
                },
                patch: {
                    operationId: 'updateProduct',
                    summary: 'Update a product',
                    description: 'Partial update using JSON Merge Patch semantics. Omitted fields are left unchanged.',
                    tags: ['Products'],
                    security: [{ oauth2: ['commerce.write'] }],
                    requestBody: {
                        required: true,
                        content: {
                            'application/merge-patch+json': {
                                schema: { $ref: '#/components/schemas/UpdateProductRequest' },
                            },
                        },
                    },
                    responses: {
                        '200': {
                            description: 'Updated product',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/Product' } },
                            },
                        },
                        '404': { $ref: '#/components/responses/NotFound' },
                        '422': { $ref: '#/components/responses/UnprocessableEntity' },
                    },
                },
                delete: {
                    operationId: 'archiveProduct',
                    summary: 'Archive a product',
                    description: 'Soft-deletes a product by setting status to `archived`. Archived products are hidden from catalog listings.',
                    tags: ['Products'],
                    security: [{ oauth2: ['commerce.write'] }],
                    deprecated: false,
                    responses: {
                        '204': { description: 'Product archived' },
                        '404': { $ref: '#/components/responses/NotFound' },
                    },
                },
            },
            '/products/{id}/variants': {
                get: {
                    operationId: 'listProductVariants',
                    summary: 'List product variants',
                    tags: ['Products'],
                    parameters: [
                        { $ref: '#/components/parameters/ProductId' },
                        { $ref: '#/components/parameters/PageSize' },
                    ],
                    responses: {
                        '200': {
                            description: 'Variant list',
                            content: {
                                'application/json': {
                                    schema: {
                                        type: 'object',
                                        properties: {
                                            data: { type: 'array', items: { $ref: '#/components/schemas/ProductVariant' } },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
                post: {
                    operationId: 'createProductVariant',
                    summary: 'Create a variant',
                    tags: ['Products'],
                    security: [{ oauth2: ['commerce.write'] }],
                    parameters: [{ $ref: '#/components/parameters/ProductId' }],
                    requestBody: {
                        required: true,
                        content: {
                            'application/json': { schema: { $ref: '#/components/schemas/CreateVariantRequest' } },
                        },
                    },
                    responses: {
                        '201': {
                            description: 'Variant created',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/ProductVariant' } },
                            },
                        },
                    },
                },
            },
            '/inventory': {
                get: {
                    operationId: 'listInventory',
                    summary: 'List inventory records',
                    tags: ['Inventory'],
                    parameters: [
                        {
                            name: 'product_id',
                            in: 'query',
                            schema: { type: 'string' },
                            description: 'Filter by product ID',
                        },
                        {
                            name: 'warehouse_id',
                            in: 'query',
                            schema: { type: 'string' },
                        },
                        {
                            name: 'low_stock_only',
                            in: 'query',
                            schema: { type: 'boolean', default: false },
                        },
                    ],
                    responses: {
                        '200': {
                            description: 'Inventory records',
                            content: {
                                'application/json': {
                                    schema: {
                                        type: 'object',
                                        properties: {
                                            data: { type: 'array', items: { $ref: '#/components/schemas/InventoryRecord' } },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            },
            '/inventory/reservations': {
                post: {
                    operationId: 'reserveInventory',
                    summary: 'Reserve stock',
                    description: 'Temporarily holds inventory for a checkout session. Reservations expire automatically after `ttl_seconds`.',
                    tags: ['Inventory'],
                    security: [{ oauth2: ['commerce.write'] }],
                    parameters: [{ $ref: '#/components/parameters/IdempotencyKey' }],
                    requestBody: {
                        required: true,
                        content: {
                            'application/json': { schema: { $ref: '#/components/schemas/ReserveInventoryRequest' } },
                        },
                    },
                    responses: {
                        '201': {
                            description: 'Reservation created',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/InventoryReservation' } },
                            },
                        },
                        '409': {
                            description: 'Insufficient stock',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/Error' } },
                            },
                        },
                    },
                },
            },
            '/customers': {
                get: {
                    operationId: 'listCustomers',
                    summary: 'List customers',
                    tags: ['Customers'],
                    parameters: [
                        { $ref: '#/components/parameters/PageSize' },
                        { $ref: '#/components/parameters/PageCursor' },
                        { name: 'email', in: 'query', schema: { type: 'string', format: 'email' } },
                    ],
                    responses: {
                        '200': {
                            description: 'Customer list',
                            content: {
                                'application/json': {
                                    schema: { $ref: '#/components/schemas/CustomerList' },
                                },
                            },
                        },
                    },
                },
                post: {
                    operationId: 'createCustomer',
                    summary: 'Create a customer',
                    tags: ['Customers'],
                    security: [{ oauth2: ['commerce.write'] }],
                    requestBody: {
                        required: true,
                        content: {
                            'application/json': { schema: { $ref: '#/components/schemas/CreateCustomerRequest' } },
                        },
                    },
                    responses: {
                        '201': {
                            description: 'Customer created',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/Customer' } },
                            },
                        },
                    },
                },
            },
            '/customers/{id}': {
                parameters: [{ $ref: '#/components/parameters/CustomerId' }],
                get: {
                    operationId: 'getCustomer',
                    summary: 'Get customer',
                    tags: ['Customers'],
                    responses: {
                        '200': {
                            description: 'Customer profile',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/Customer' } },
                            },
                        },
                        '404': { $ref: '#/components/responses/NotFound' },
                    },
                },
            },
            '/orders': {
                get: {
                    operationId: 'listOrders',
                    summary: 'List orders',
                    tags: ['Orders'],
                    parameters: [
                        { $ref: '#/components/parameters/PageSize' },
                        { name: 'status', in: 'query', schema: { $ref: '#/components/schemas/OrderStatus' } },
                        { name: 'customer_id', in: 'query', schema: { type: 'string' } },
                        { name: 'placed_after', in: 'query', schema: { type: 'string', format: 'date-time' } },
                        { name: 'placed_before', in: 'query', schema: { type: 'string', format: 'date-time' } },
                    ],
                    responses: {
                        '200': {
                            description: 'Order list',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/OrderList' } },
                            },
                        },
                    },
                },
                post: {
                    operationId: 'createOrder',
                    summary: 'Place an order',
                    description: 'Creates an order from line items. Use a prior inventory reservation when stock is limited.',
                    tags: ['Orders'],
                    security: [{ oauth2: ['commerce.write'] }],
                    parameters: [{ $ref: '#/components/parameters/IdempotencyKey' }],
                    requestBody: {
                        required: true,
                        content: {
                            'application/json': { schema: { $ref: '#/components/schemas/CreateOrderRequest' } },
                        },
                    },
                    responses: {
                        '201': {
                            description: 'Order placed',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/Order' } },
                            },
                        },
                        '402': {
                            description: 'Payment required',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/PaymentRequiredError' } },
                            },
                        },
                    },
                },
            },
            '/orders/{id}': {
                parameters: [{ $ref: '#/components/parameters/OrderId' }],
                get: {
                    operationId: 'getOrder',
                    summary: 'Get order details',
                    tags: ['Orders'],
                    responses: {
                        '200': {
                            description: 'Order',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/Order' } },
                            },
                        },
                        '404': { $ref: '#/components/responses/NotFound' },
                    },
                },
            },
            '/orders/{id}/cancel': {
                post: {
                    operationId: 'cancelOrder',
                    summary: 'Cancel an order',
                    description: 'Cancels an order while it is still `pending` or `confirmed`. Shipped orders must be returned instead.',
                    tags: ['Orders'],
                    security: [{ oauth2: ['commerce.write'] }],
                    parameters: [{ $ref: '#/components/parameters/OrderId' }],
                    requestBody: {
                        content: {
                            'application/json': {
                                schema: {
                                    type: 'object',
                                    properties: {
                                        reason: { type: 'string', enum: ['customer_request', 'fraud', 'out_of_stock', 'other'] },
                                        note: { type: 'string', maxLength: 500 },
                                    },
                                },
                            },
                        },
                    },
                    responses: {
                        '200': {
                            description: 'Cancelled order',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/Order' } },
                            },
                        },
                        '409': { $ref: '#/components/responses/Conflict' },
                    },
                },
            },
            '/orders/{id}/fulfillments': {
                get: {
                    operationId: 'listFulfillments',
                    summary: 'List fulfillments for an order',
                    tags: ['Orders'],
                    parameters: [{ $ref: '#/components/parameters/OrderId' }],
                    responses: {
                        '200': {
                            description: 'Fulfillment shipments',
                            content: {
                                'application/json': {
                                    schema: {
                                        type: 'object',
                                        properties: {
                                            data: { type: 'array', items: { $ref: '#/components/schemas/Fulfillment' } },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            },
            '/webhooks': {
                get: {
                    operationId: 'listWebhooks',
                    summary: 'List webhook endpoints',
                    tags: ['Webhooks'],
                    security: [{ apiKey: [] }],
                    responses: {
                        '200': {
                            description: 'Registered endpoints',
                            content: {
                                'application/json': {
                                    schema: {
                                        type: 'object',
                                        properties: {
                                            data: { type: 'array', items: { $ref: '#/components/schemas/WebhookEndpoint' } },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
                post: {
                    operationId: 'createWebhook',
                    summary: 'Register a webhook endpoint',
                    tags: ['Webhooks'],
                    security: [{ apiKey: [] }],
                    requestBody: {
                        required: true,
                        content: {
                            'application/json': { schema: { $ref: '#/components/schemas/CreateWebhookRequest' } },
                        },
                    },
                    responses: {
                        '201': {
                            description: 'Webhook registered',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/WebhookEndpoint' } },
                            },
                        },
                    },
                },
            },
            '/webhooks/{id}/deliveries': {
                get: {
                    operationId: 'listWebhookDeliveries',
                    summary: 'List delivery attempts',
                    description: 'Inspect recent delivery attempts, response codes, and retry schedules for debugging.',
                    tags: ['Webhooks'],
                    security: [{ apiKey: [] }],
                    parameters: [
                        { name: 'id', in: 'path', required: true, schema: { type: 'string' } },
                        { name: 'status', in: 'query', schema: { type: 'string', enum: ['pending', 'delivered', 'failed'] } },
                    ],
                    responses: {
                        '200': {
                            description: 'Delivery log',
                            content: {
                                'application/json': {
                                    schema: {
                                        type: 'object',
                                        properties: {
                                            data: { type: 'array', items: { $ref: '#/components/schemas/WebhookDelivery' } },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            },
            '/analytics/summary': {
                get: {
                    operationId: 'getAnalyticsSummary',
                    summary: 'Commerce KPI summary',
                    description: 'Returns aggregated revenue, order count, and conversion metrics for a date range.',
                    tags: ['Analytics'],
                    security: [{ bearerAuth: [] }],
                    parameters: [
                        { name: 'from', in: 'query', required: true, schema: { type: 'string', format: 'date' } },
                        { name: 'to', in: 'query', required: true, schema: { type: 'string', format: 'date' } },
                        { name: 'granularity', in: 'query', schema: { type: 'string', enum: ['day', 'week', 'month'], default: 'day' } },
                    ],
                    responses: {
                        '200': {
                            description: 'Analytics summary',
                            content: {
                                'application/json': { schema: { $ref: '#/components/schemas/AnalyticsSummary' } },
                                'text/csv': {
                                    schema: { type: 'string', format: 'binary' },
                                    example: 'date,revenue,orders\n2026-01-01,125000,42\n',
                                },
                            },
                        },
                    },
                },
            },
            '/health': {
                get: {
                    operationId: 'healthCheck',
                    summary: 'Health check',
                    description: 'Unauthenticated liveness probe for load balancers.',
                    tags: ['Analytics'],
                    security: [],
                    responses: {
                        '200': {
                            description: 'Service is healthy',
                            content: {
                                'application/json': {
                                    schema: {
                                        type: 'object',
                                        required: ['status'],
                                        properties: {
                                            status: { type: 'string', enum: ['ok'] },
                                            version: { type: 'string' },
                                            uptime_seconds: { type: 'integer' },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            },
        },
        components: {
            securitySchemes: {
                oauth2: {
                    type: 'oauth2',
                    description: 'OAuth 2.0 authorization code flow with PKCE',
                    flows: {
                        authorizationCode: {
                            authorizationUrl: 'https://auth.commerce.example.com/oauth/authorize',
                            tokenUrl: 'https://auth.commerce.example.com/oauth/token',
                            refreshUrl: 'https://auth.commerce.example.com/oauth/token',
                            scopes: {
                                'commerce.read': 'Read catalog, orders, and customers',
                                'commerce.write': 'Create and update commerce resources',
                                'analytics.read': 'Read aggregated analytics',
                            },
                        },
                    },
                },
                apiKey: {
                    type: 'apiKey',
                    in: 'header',
                    name: 'X-API-Key',
                    description: 'Portal subscription API key',
                },
                bearerAuth: {
                    type: 'http',
                    scheme: 'bearer',
                    bearerFormat: 'JWT',
                    description: 'Short-lived JWT from the token endpoint',
                },
            },
            parameters: {
                ProductId: {
                    name: 'id',
                    in: 'path',
                    required: true,
                    schema: { type: 'string', pattern: '^prod_[A-Za-z0-9]{12}$' },
                    example: 'prod_7hK2mNqR9vX1',
                },
                CustomerId: {
                    name: 'id',
                    in: 'path',
                    required: true,
                    schema: { type: 'string', pattern: '^cus_[A-Za-z0-9]{12}$' },
                },
                OrderId: {
                    name: 'id',
                    in: 'path',
                    required: true,
                    schema: { type: 'string', pattern: '^ord_[A-Za-z0-9]{12}$' },
                },
                PageSize: {
                    name: 'limit',
                    in: 'query',
                    schema: { type: 'integer', minimum: 1, maximum: 100, default: 25 },
                },
                PageCursor: {
                    name: 'cursor',
                    in: 'query',
                    schema: { type: 'string' },
                    description: 'Opaque cursor from a previous response `pagination.next_cursor`',
                },
                IdempotencyKey: {
                    name: 'Idempotency-Key',
                    in: 'header',
                    required: true,
                    schema: { type: 'string', format: 'uuid' },
                },
                AcceptLanguage: {
                    name: 'Accept-Language',
                    in: 'header',
                    schema: { type: 'string', example: 'en-US' },
                },
            },
            headers: {
                XRequestId: {
                    description: 'Unique request identifier for support correlation',
                    schema: { type: 'string', format: 'uuid' },
                },
                RetryAfter: {
                    description: 'Seconds to wait before retrying when rate limited',
                    schema: { type: 'integer', minimum: 1 },
                },
            },
            responses: {
                BadRequest: {
                    description: 'Malformed or invalid request',
                    content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
                },
                Unauthorized: {
                    description: 'Missing or invalid credentials',
                    content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
                },
                Forbidden: {
                    description: 'Insufficient scope or plan',
                    content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
                },
                NotFound: {
                    description: 'Resource not found',
                    content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
                },
                Conflict: {
                    description: 'State conflict or idempotency mismatch',
                    content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
                },
                UnprocessableEntity: {
                    description: 'Semantic validation failed',
                    content: { 'application/json': { schema: { $ref: '#/components/schemas/ValidationError' } } },
                },
                RateLimited: {
                    description: 'Too many requests',
                    headers: { 'Retry-After': { $ref: '#/components/headers/RetryAfter' } },
                    content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
                },
            },
            schemas: {
                Money: {
                    type: 'object',
                    required: ['amount', 'currency'],
                    properties: {
                        amount: { type: 'integer', description: 'Minor units (e.g. cents)', minimum: 0, example: 4999 },
                        currency: { type: 'string', minLength: 3, maxLength: 3, example: 'USD' },
                    },
                },
                Address: {
                    type: 'object',
                    required: ['line1', 'city', 'country'],
                    properties: {
                        line1: { type: 'string' },
                        line2: { type: 'string', nullable: true },
                        city: { type: 'string' },
                        region: { type: 'string', nullable: true },
                        postal_code: { type: 'string' },
                        country: { type: 'string', minLength: 2, maxLength: 2, example: 'US' },
                    },
                },
                Pagination: {
                    type: 'object',
                    properties: {
                        next_cursor: { type: 'string', nullable: true },
                        has_more: { type: 'boolean' },
                    },
                },
                ProductBase: {
                    type: 'object',
                    required: ['sku', 'name', 'base_price'],
                    properties: {
                        sku: { type: 'string', pattern: '^[A-Z0-9-]{3,32}$' },
                        name: { type: 'string', minLength: 2, maxLength: 160 },
                        description: { type: 'string', maxLength: 4000 },
                        base_price: { $ref: '#/components/schemas/Money' },
                        categories: { type: 'array', items: { type: 'string' }, maxItems: 8 },
                        metadata: { type: 'object', additionalProperties: { type: 'string' } },
                        webhook_url: { type: 'string', format: 'uri', writeOnly: true },
                    },
                },
                PhysicalProductDetails: {
                    type: 'object',
                    required: ['dimensions', 'shipping_class'],
                    properties: {
                        type: { type: 'string', enum: ['physical'] },
                        dimensions: {
                            type: 'object',
                            required: ['weight_grams'],
                            properties: {
                                weight_grams: { type: 'integer', minimum: 1 },
                                length_mm: { type: 'integer', minimum: 1 },
                                width_mm: { type: 'integer', minimum: 1 },
                                height_mm: { type: 'integer', minimum: 1 },
                            },
                        },
                        shipping_class: { type: 'string', enum: ['standard', 'oversized', 'hazmat'] },
                    },
                },
                DigitalProductDetails: {
                    type: 'object',
                    required: ['download_url_ttl_seconds', 'license_type'],
                    properties: {
                        type: { type: 'string', enum: ['digital'] },
                        download_url_ttl_seconds: { type: 'integer', minimum: 300, maximum: 604800 },
                        license_type: { type: 'string', enum: ['single_user', 'team', 'enterprise'] },
                    },
                },
                CreateProductRequest: {
                    allOf: [
                        { $ref: '#/components/schemas/ProductBase' },
                        {
                            oneOf: [
                                { $ref: '#/components/schemas/PhysicalProductDetails' },
                                { $ref: '#/components/schemas/DigitalProductDetails' },
                            ],
                            discriminator: {
                                propertyName: 'type',
                                mapping: {
                                    physical: '#/components/schemas/PhysicalProductDetails',
                                    digital: '#/components/schemas/DigitalProductDetails',
                                },
                            },
                        },
                    ],
                },
                UpdateProductRequest: {
                    type: 'object',
                    properties: {
                        name: { type: 'string' },
                        description: { type: 'string' },
                        base_price: { $ref: '#/components/schemas/Money' },
                        status: { type: 'string', enum: ['draft', 'active', 'archived'] },
                        metadata: { type: 'object', additionalProperties: { type: 'string' } },
                    },
                },
                Product: {
                    allOf: [
                        {
                            type: 'object',
                            required: ['id', 'status', 'created_at'],
                            properties: {
                                id: { type: 'string', readOnly: true },
                                status: { type: 'string', enum: ['draft', 'active', 'archived'] },
                                created_at: { type: 'string', format: 'date-time', readOnly: true },
                                updated_at: { type: 'string', format: 'date-time', readOnly: true },
                            },
                        },
                        { $ref: '#/components/schemas/CreateProductRequest' },
                    ],
                },
                ProductVariant: {
                    type: 'object',
                    required: ['id', 'sku', 'options'],
                    properties: {
                        id: { type: 'string' },
                        sku: { type: 'string' },
                        options: {
                            type: 'object',
                            additionalProperties: { type: 'string' },
                            example: { color: 'midnight', size: 'L' },
                        },
                        price_override: { $ref: '#/components/schemas/Money' },
                        barcode: { type: 'string', nullable: true },
                    },
                },
                CreateVariantRequest: {
                    type: 'object',
                    required: ['sku', 'options'],
                    properties: {
                        sku: { type: 'string' },
                        options: { type: 'object', additionalProperties: { type: 'string' } },
                        price_override: { $ref: '#/components/schemas/Money' },
                    },
                },
                ProductList: {
                    type: 'object',
                    required: ['data', 'pagination'],
                    properties: {
                        data: { type: 'array', items: { $ref: '#/components/schemas/Product' } },
                        pagination: { $ref: '#/components/schemas/Pagination' },
                    },
                },
                InventoryRecord: {
                    type: 'object',
                    properties: {
                        id: { type: 'string' },
                        product_id: { type: 'string' },
                        variant_id: { type: 'string', nullable: true },
                        warehouse_id: { type: 'string' },
                        on_hand: { type: 'integer', minimum: 0 },
                        reserved: { type: 'integer', minimum: 0 },
                        available: { type: 'integer', minimum: 0 },
                        reorder_threshold: { type: 'integer', minimum: 0 },
                    },
                },
                ReserveInventoryRequest: {
                    type: 'object',
                    required: ['items', 'ttl_seconds'],
                    properties: {
                        items: {
                            type: 'array',
                            minItems: 1,
                            items: {
                                type: 'object',
                                required: ['product_id', 'quantity'],
                                properties: {
                                    product_id: { type: 'string' },
                                    variant_id: { type: 'string' },
                                    quantity: { type: 'integer', minimum: 1 },
                                },
                            },
                        },
                        ttl_seconds: { type: 'integer', minimum: 60, maximum: 1800, default: 900 },
                    },
                },
                InventoryReservation: {
                    type: 'object',
                    properties: {
                        id: { type: 'string' },
                        expires_at: { type: 'string', format: 'date-time' },
                        items: {
                            type: 'array',
                            items: {
                                type: 'object',
                                properties: {
                                    product_id: { type: 'string' },
                                    quantity: { type: 'integer' },
                                    reserved: { type: 'boolean' },
                                },
                            },
                        },
                    },
                },
                Customer: {
                    type: 'object',
                    required: ['id', 'email', 'created_at'],
                    properties: {
                        id: { type: 'string' },
                        email: { type: 'string', format: 'email' },
                        first_name: { type: 'string' },
                        last_name: { type: 'string' },
                        phone: { type: 'string', nullable: true },
                        default_address: { $ref: '#/components/schemas/Address' },
                        tags: { type: 'array', items: { type: 'string' } },
                        lifetime_value: { $ref: '#/components/schemas/Money' },
                        created_at: { type: 'string', format: 'date-time' },
                    },
                },
                CreateCustomerRequest: {
                    type: 'object',
                    required: ['email'],
                    properties: {
                        email: { type: 'string', format: 'email' },
                        first_name: { type: 'string' },
                        last_name: { type: 'string' },
                        phone: { type: 'string' },
                        default_address: { $ref: '#/components/schemas/Address' },
                    },
                },
                CustomerList: {
                    type: 'object',
                    properties: {
                        data: { type: 'array', items: { $ref: '#/components/schemas/Customer' } },
                        pagination: { $ref: '#/components/schemas/Pagination' },
                    },
                },
                OrderStatus: {
                    type: 'string',
                    enum: ['pending', 'confirmed', 'processing', 'shipped', 'delivered', 'cancelled', 'refunded'],
                },
                OrderLineItem: {
                    type: 'object',
                    required: ['product_id', 'quantity', 'unit_price'],
                    properties: {
                        product_id: { type: 'string' },
                        variant_id: { type: 'string', nullable: true },
                        sku: { type: 'string' },
                        name: { type: 'string' },
                        quantity: { type: 'integer', minimum: 1 },
                        unit_price: { $ref: '#/components/schemas/Money' },
                        tax_amount: { $ref: '#/components/schemas/Money' },
                    },
                },
                CreateOrderRequest: {
                    type: 'object',
                    required: ['customer_id', 'currency', 'line_items'],
                    properties: {
                        customer_id: { type: 'string' },
                        currency: { type: 'string', minLength: 3, maxLength: 3 },
                        line_items: { type: 'array', minItems: 1, items: { $ref: '#/components/schemas/OrderLineItem' } },
                        shipping_address: { $ref: '#/components/schemas/Address' },
                        billing_address: { $ref: '#/components/schemas/Address' },
                        reservation_id: { type: 'string', description: 'Optional inventory reservation from checkout' },
                        payment_method_token: { type: 'string' },
                        metadata: { type: 'object', additionalProperties: { type: 'string' } },
                    },
                },
                Order: {
                    type: 'object',
                    required: ['id', 'status', 'total', 'line_items', 'placed_at'],
                    properties: {
                        id: { type: 'string' },
                        status: { $ref: '#/components/schemas/OrderStatus' },
                        customer_id: { type: 'string' },
                        line_items: { type: 'array', items: { $ref: '#/components/schemas/OrderLineItem' } },
                        subtotal: { $ref: '#/components/schemas/Money' },
                        tax: { $ref: '#/components/schemas/Money' },
                        shipping: { $ref: '#/components/schemas/Money' },
                        total: { $ref: '#/components/schemas/Money' },
                        shipping_address: { $ref: '#/components/schemas/Address' },
                        placed_at: { type: 'string', format: 'date-time' },
                        cancelled_at: { type: 'string', format: 'date-time', nullable: true },
                    },
                },
                OrderList: {
                    type: 'object',
                    properties: {
                        data: { type: 'array', items: { $ref: '#/components/schemas/Order' } },
                        pagination: { $ref: '#/components/schemas/Pagination' },
                    },
                },
                Fulfillment: {
                    type: 'object',
                    properties: {
                        id: { type: 'string' },
                        carrier: { type: 'string' },
                        tracking_number: { type: 'string' },
                        status: { type: 'string', enum: ['label_created', 'in_transit', 'delivered', 'exception'] },
                        shipped_at: { type: 'string', format: 'date-time', nullable: true },
                        delivered_at: { type: 'string', format: 'date-time', nullable: true },
                    },
                },
                WebhookEndpoint: {
                    type: 'object',
                    properties: {
                        id: { type: 'string' },
                        url: { type: 'string', format: 'uri' },
                        events: {
                            type: 'array',
                            items: {
                                type: 'string',
                                enum: ['order.created', 'order.shipped', 'product.updated', 'inventory.low_stock'],
                            },
                        },
                        secret: { type: 'string', writeOnly: true },
                        enabled: { type: 'boolean' },
                        created_at: { type: 'string', format: 'date-time' },
                    },
                },
                CreateWebhookRequest: {
                    type: 'object',
                    required: ['url', 'events'],
                    properties: {
                        url: { type: 'string', format: 'uri' },
                        events: { type: 'array', minItems: 1, items: { type: 'string' } },
                    },
                },
                WebhookEvent: {
                    type: 'object',
                    required: ['id', 'type', 'created_at', 'data'],
                    properties: {
                        id: { type: 'string' },
                        type: { type: 'string' },
                        created_at: { type: 'string', format: 'date-time' },
                        data: { type: 'object', additionalProperties: true },
                    },
                },
                WebhookDelivery: {
                    type: 'object',
                    properties: {
                        id: { type: 'string' },
                        event_id: { type: 'string' },
                        status: { type: 'string', enum: ['pending', 'delivered', 'failed'] },
                        response_status: { type: 'integer', nullable: true },
                        attempt_count: { type: 'integer' },
                        next_retry_at: { type: 'string', format: 'date-time', nullable: true },
                    },
                },
                AnalyticsSummary: {
                    type: 'object',
                    properties: {
                        from: { type: 'string', format: 'date' },
                        to: { type: 'string', format: 'date' },
                        granularity: { type: 'string' },
                        totals: {
                            type: 'object',
                            properties: {
                                revenue: { $ref: '#/components/schemas/Money' },
                                orders: { type: 'integer' },
                                average_order_value: { $ref: '#/components/schemas/Money' },
                                conversion_rate: { type: 'number', format: 'float', minimum: 0, maximum: 1 },
                            },
                        },
                        series: {
                            type: 'array',
                            items: {
                                type: 'object',
                                properties: {
                                    date: { type: 'string', format: 'date' },
                                    revenue: { type: 'integer' },
                                    orders: { type: 'integer' },
                                },
                            },
                        },
                    },
                },
                Error: {
                    type: 'object',
                    required: ['code', 'message'],
                    properties: {
                        code: { type: 'string', example: 'invalid_request' },
                        message: { type: 'string', example: 'The limit parameter must be between 1 and 100' },
                        request_id: { type: 'string', format: 'uuid' },
                        documentation_url: { type: 'string', format: 'uri' },
                    },
                },
                ValidationError: {
                    allOf: [
                        { $ref: '#/components/schemas/Error' },
                        {
                            type: 'object',
                            properties: {
                                errors: {
                                    type: 'array',
                                    items: {
                                        type: 'object',
                                        properties: {
                                            field: { type: 'string' },
                                            code: { type: 'string' },
                                            message: { type: 'string' },
                                        },
                                    },
                                },
                            },
                        },
                    ],
                },
                PaymentRequiredError: {
                    allOf: [
                        { $ref: '#/components/schemas/Error' },
                        {
                            type: 'object',
                            properties: {
                                payment_intent_id: { type: 'string' },
                                client_secret: { type: 'string' },
                            },
                        },
                    ],
                },
            },
        },
    },
    null,
    2,
);
