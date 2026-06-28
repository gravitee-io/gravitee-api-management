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
import { DEFAULT_TILE_TEMPLATE, serializeTileTemplate } from '../../../blocks/ApiCatalogBlock/tile-template';
import { saveNavItem } from './navigation-items.storage';
import { savePageContent } from './page-contents.storage';

// ---------------------------------------------------------------------------
// Block helpers
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

const MEMBER_API_SPEC = JSON.stringify({
    openapi: '3.0.3',
    info: {
        title: 'Member Management API',
        version: '3.2.0',
        description: 'Manage gym members, memberships, check-ins, and profile data across ABC Fitness club management platforms.',
        contact: { name: 'ABC Fitness Partner Team', email: 'partners-api@abcfitness.example.com' },
    },
    servers: [
        { url: 'https://api.abcfitness.example.com/v3', description: 'Production' },
        { url: 'https://sandbox.abcfitness.example.com/v3', description: 'Sandbox' },
    ],
    security: [{ oauth2: ['members.read', 'members.write'] }],
    tags: [
        { name: 'Members', description: 'Member profile and lifecycle' },
        { name: 'Check-ins', description: 'Facility access and attendance' },
    ],
    paths: {
        '/members': {
            get: {
                operationId: 'listMembers',
                summary: 'List members',
                description: 'Retrieve a paginated list of club members, optionally filtered by status, membership type, or home club.',
                tags: ['Members'],
                parameters: [
                    { name: 'club_id', in: 'query', schema: { type: 'string' }, description: 'Filter by home club ID' },
                    { name: 'status', in: 'query', schema: { type: 'string', enum: ['active', 'frozen', 'cancelled', 'prospect'] } },
                    { name: 'limit', in: 'query', schema: { type: 'integer', default: 25, maximum: 100 } },
                    { name: 'cursor', in: 'query', schema: { type: 'string' }, description: 'Pagination cursor' },
                ],
                responses: {
                    '200': {
                        description: 'Paginated member list',
                        content: {
                            'application/json': {
                                schema: {
                                    type: 'object',
                                    properties: {
                                        data: { type: 'array', items: { $ref: '#/components/schemas/Member' } },
                                        next_cursor: { type: 'string', nullable: true },
                                    },
                                },
                            },
                        },
                    },
                },
            },
            post: {
                operationId: 'createMember',
                summary: 'Create a member',
                description: 'Register a new member prospect or convert a lead into an active membership record.',
                tags: ['Members'],
                requestBody: { required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/CreateMemberRequest' } } } },
                responses: {
                    '201': { description: 'Member created', content: { 'application/json': { schema: { $ref: '#/components/schemas/Member' } } } },
                    '400': { description: 'Validation error' },
                    '409': { description: 'Duplicate email or external ID' },
                },
            },
        },
        '/members/{memberId}': {
            get: {
                operationId: 'getMember',
                summary: 'Get member details',
                tags: ['Members'],
                parameters: [{ name: 'memberId', in: 'path', required: true, schema: { type: 'string' } }],
                responses: {
                    '200': { description: 'Member details', content: { 'application/json': { schema: { $ref: '#/components/schemas/Member' } } } },
                    '404': { description: 'Member not found' },
                },
            },
        },
        '/members/{memberId}/check-ins': {
            post: {
                operationId: 'recordCheckIn',
                summary: 'Record a check-in',
                description: 'Log a facility check-in for a member. Supports barcode, RFID, and mobile app check-in sources.',
                tags: ['Check-ins'],
                parameters: [{ name: 'memberId', in: 'path', required: true, schema: { type: 'string' } }],
                requestBody: {
                    required: true,
                    content: {
                        'application/json': {
                            schema: {
                                type: 'object',
                                required: ['club_id', 'source'],
                                properties: {
                                    club_id: { type: 'string' },
                                    source: { type: 'string', enum: ['barcode', 'rfid', 'mobile', 'manual'] },
                                    zone_id: { type: 'string', description: 'Optional facility zone' },
                                },
                            },
                        },
                    },
                },
                responses: {
                    '201': { description: 'Check-in recorded', content: { 'application/json': { schema: { $ref: '#/components/schemas/CheckIn' } } } },
                    '403': { description: 'Membership inactive or access denied' },
                },
            },
        },
    },
    components: {
        schemas: {
            CreateMemberRequest: {
                type: 'object',
                required: ['first_name', 'last_name', 'email', 'club_id'],
                properties: {
                    first_name: { type: 'string', example: 'Jordan' },
                    last_name: { type: 'string', example: 'Lee' },
                    email: { type: 'string', format: 'email' },
                    phone: { type: 'string' },
                    club_id: { type: 'string' },
                    membership_type: { type: 'string', enum: ['monthly', 'annual', 'day_pass', 'corporate'] },
                    external_id: { type: 'string', description: 'Your system\'s member identifier' },
                },
            },
            Member: {
                type: 'object',
                properties: {
                    id: { type: 'string', example: 'mbr_7kQ2xNpR9mW4' },
                    first_name: { type: 'string' },
                    last_name: { type: 'string' },
                    email: { type: 'string' },
                    status: { type: 'string', enum: ['active', 'frozen', 'cancelled', 'prospect'] },
                    club_id: { type: 'string' },
                    membership_type: { type: 'string' },
                    join_date: { type: 'string', format: 'date' },
                    last_check_in: { type: 'string', format: 'date-time', nullable: true },
                },
            },
            CheckIn: {
                type: 'object',
                properties: {
                    id: { type: 'string' },
                    member_id: { type: 'string' },
                    club_id: { type: 'string' },
                    source: { type: 'string' },
                    checked_in_at: { type: 'string', format: 'date-time' },
                },
            },
        },
        securitySchemes: {
            oauth2: {
                type: 'oauth2',
                flows: {
                    clientCredentials: {
                        tokenUrl: 'https://auth.abcfitness.example.com/oauth/token',
                        scopes: { 'members.read': 'Read member data', 'members.write': 'Create and update members' },
                    },
                },
            },
        },
    },
});

const CLUB_OPS_API_SPEC = JSON.stringify({
    openapi: '3.0.3',
    info: {
        title: 'Club Operations API',
        version: '2.0.1',
        description: 'Schedule classes, manage billing cycles, and coordinate multi-location club operations through a unified REST interface.',
        contact: { name: 'ABC Fitness Partner Team', email: 'partners-api@abcfitness.example.com' },
    },
    servers: [
        { url: 'https://api.abcfitness.example.com/v3', description: 'Production' },
        { url: 'https://sandbox.abcfitness.example.com/v3', description: 'Sandbox' },
    ],
    security: [{ apiKey: [] }],
    tags: [
        { name: 'Classes', description: 'Group fitness scheduling' },
        { name: 'Billing', description: 'Recurring billing and invoices' },
    ],
    paths: {
        '/clubs/{clubId}/classes': {
            get: {
                operationId: 'listClasses',
                summary: 'List scheduled classes',
                tags: ['Classes'],
                parameters: [
                    { name: 'clubId', in: 'path', required: true, schema: { type: 'string' } },
                    { name: 'date', in: 'query', schema: { type: 'string', format: 'date' } },
                    { name: 'category', in: 'query', schema: { type: 'string', enum: ['yoga', 'cycling', 'strength', 'hiit', 'pilates'] } },
                ],
                responses: {
                    '200': {
                        description: 'Class schedule',
                        content: {
                            'application/json': {
                                schema: { type: 'array', items: { $ref: '#/components/schemas/ClassSession' } },
                            },
                        },
                    },
                },
            },
        },
        '/clubs/{clubId}/classes/{classId}/bookings': {
            post: {
                operationId: 'bookClass',
                summary: 'Book a class spot',
                tags: ['Classes'],
                parameters: [
                    { name: 'clubId', in: 'path', required: true, schema: { type: 'string' } },
                    { name: 'classId', in: 'path', required: true, schema: { type: 'string' } },
                ],
                requestBody: {
                    required: true,
                    content: {
                        'application/json': {
                            schema: {
                                type: 'object',
                                required: ['member_id'],
                                properties: { member_id: { type: 'string' }, waitlist: { type: 'boolean', default: false } },
                            },
                        },
                    },
                },
                responses: {
                    '201': { description: 'Booking confirmed' },
                    '409': { description: 'Class full — use waitlist instead' },
                },
            },
        },
        '/billing/invoices': {
            get: {
                operationId: 'listInvoices',
                summary: 'List billing invoices',
                tags: ['Billing'],
                parameters: [
                    { name: 'member_id', in: 'query', schema: { type: 'string' } },
                    { name: 'status', in: 'query', schema: { type: 'string', enum: ['draft', 'open', 'paid', 'void'] } },
                ],
                responses: {
                    '200': {
                        description: 'Invoice list',
                        content: {
                            'application/json': {
                                schema: { type: 'array', items: { $ref: '#/components/schemas/Invoice' } },
                            },
                        },
                    },
                },
            },
        },
    },
    components: {
        schemas: {
            ClassSession: {
                type: 'object',
                properties: {
                    id: { type: 'string' },
                    name: { type: 'string', example: 'Power Cycle 45' },
                    instructor: { type: 'string' },
                    category: { type: 'string' },
                    start_time: { type: 'string', format: 'date-time' },
                    duration_minutes: { type: 'integer' },
                    capacity: { type: 'integer' },
                    spots_remaining: { type: 'integer' },
                },
            },
            Invoice: {
                type: 'object',
                properties: {
                    id: { type: 'string' },
                    member_id: { type: 'string' },
                    amount_cents: { type: 'integer' },
                    currency: { type: 'string', example: 'USD' },
                    status: { type: 'string' },
                    due_date: { type: 'string', format: 'date' },
                },
            },
        },
        securitySchemes: {
            apiKey: { type: 'apiKey', name: 'X-ABC-API-Key', in: 'header' },
        },
    },
});

// ---------------------------------------------------------------------------
// Page documents — fitness marketplace theme, layouts unlike Payments portal
// ---------------------------------------------------------------------------

function homeDocument(): BlockNoteDocument {
    return [
        banner(
            'Grow Your Fitness Business',
            'Welcome to the ABC Fitness Partner Marketplace — discover integrations that supercharge club operations and member experiences.',
            'gradient',
            [
                { label: 'Explore Marketplace', link: '/featured-abc002', variant: 'primary' },
                { label: 'Apply to Partner', link: '/partner-application-abc016', variant: 'secondary' },
            ],
        ),
        section(
            'Why Partner With ABC Fitness?',
            'Plug into a platform powering thousands of fitness businesses globally.',
            'accent',
            '2',
            [
                { icon: 'rocket', title: 'Scalable Ecosystem', description: 'Reach gyms, studios, and coaching businesses through Ignite, Glofox, Trainerize, and Evo.', buttonLabel: 'View Platforms', buttonLink: '/ignite-overview-abc007' },
                { icon: 'globe', title: 'Shared Growth', description: 'Co-selling, co-marketing, and marketplace listing for certified integrations.', buttonLabel: 'Partner Program', buttonLink: '/partner-program-abc023' },
            ],
        ),
        spacer(),
        card('Unified Experience', 'Native and API-based workflows keep member data consistent across your product and ABC club systems.', 'globe', 'blue'),
        card('Open API Platform', 'RESTful APIs with OAuth 2.0, webhooks, and sandbox clubs for every platform surface.', 'key', 'green'),
        spacer(),
        heading(2, 'New & Noteworthy'),
        paragraph('Latest partners joining the ABC Fitness ecosystem.'),
        section(
            '',
            '',
            'none',
            '3',
            [
                { icon: 'monitor', title: 'EGYM', description: 'Smarter, personalized fitness experiences brought to more gyms worldwide.', buttonLabel: 'Learn More', buttonLink: '/featured-abc002' },
                { icon: 'code', title: 'MyFitnessPal', description: 'Holistic health tracking integrated for gym members and coaching clients.', buttonLabel: 'Learn More', buttonLink: '/featured-abc002' },
                { icon: 'shield', title: 'Truemed', description: 'Unlock healthcare funds for eligible fitness investments at participating clubs.', buttonLabel: 'Learn More', buttonLink: '/featured-abc002' },
            ],
        ),
        spacer(),
        banner(
            'For App Partners & Solution Providers',
            'Reach thousands of gyms and fitness professionals by integrating with ABC Fitness.',
            'dark',
            [{ label: 'Become a Partner', link: '/partner-application-abc016', variant: 'primary' }],
        ),
    ];
}

function featuredPartnersDocument(): BlockNoteDocument {
    return [
        heading(1, 'New & Noteworthy'),
        paragraph('Discover the latest partners joining the ABC Fitness ecosystem — apps and integrations designed to elevate member experiences and drive smarter growth.'),
        spacer(),
        section(
            'Featured Partners',
            '',
            'gray',
            '3',
            [
                { icon: 'rocket', title: 'EGYM', description: 'ABC and EGYM expand market reach with AI-powered equipment and training plans synced to member profiles.' },
                { icon: 'globe', title: 'MyFitnessPal', description: 'Nutrition and activity data flows into ABC member dashboards for holistic wellness tracking.' },
                { icon: 'shield', title: 'Truemed', description: 'Eligible members use HSA/FSA funds for memberships at participating ABC-powered clubs.' },
            ],
        ),
        spacer(),
        apiList('Recently Certified', 'cards'),
        spacer(),
        button('Browse All Integrations', '/browse-integrations-abc006', 'filled'),
    ];
}

function memberExperienceDocument(): BlockNoteDocument {
    return [
        banner('Member Experience Integrations', 'Apps that engage members before, during, and after every workout.', 'indigo'),
        spacer(),
        card('Access & Check-in', 'Barcode, RFID, and mobile check-in APIs with real-time occupancy data.', 'key', 'blue'),
        card('Profile & Preferences', 'Sync member goals, communication preferences, and wearable connections.', 'globe', 'purple'),
        spacer(),
        heading(2, 'Common Use Cases'),
        bulletItem('Mobile app login with ABC member credentials (OAuth Authorization Code)'),
        bulletItem('Push notifications triggered by check-in or class booking webhooks'),
        bulletItem('Loyalty points awarded on attendance milestones'),
        bulletItem('Guest pass redemption through partner wellness apps'),
        spacer(),
        button('Members & Access API', '/ignite-members-api-abc008', 'outlined'),
    ];
}

function clubOperationsCategoryDocument(): BlockNoteDocument {
    return [
        heading(1, 'Club Operations'),
        paragraph('Integrations that help operators run facilities, schedules, and staff workflows more efficiently.'),
        spacer(),
        section(
            'Operational APIs',
            '',
            'dark',
            '2',
            [
                { icon: 'server', title: 'Class Scheduling', description: 'List schedules, manage capacity, waitlists, and instructor assignments across locations.', buttonLabel: 'Evo Operations API', buttonLink: '/evo-ops-api-abc015' },
                { icon: 'database', title: 'Facility Management', description: 'Zone occupancy, equipment maintenance logs, and access control rules.' },
            ],
        ),
        spacer(),
        numberedItem('Pull daily class rosters for digital signage partners'),
        numberedItem('Sync personal training appointments to external calendar systems'),
        numberedItem('Automate door access based on membership status and time-of-day rules'),
        numberedItem('Export payroll hours from instructor clock-in data'),
    ];
}

function wellnessCoachingDocument(): BlockNoteDocument {
    return [
        banner(
            'Wellness & Coaching',
            'Connect personal training, nutrition, and habit coaching into the ABC member journey.',
            'light',
            [{ label: 'Trainerize Overview', link: '/trainerize-overview-abc012', variant: 'primary' }],
        ),
        spacer(),
        section(
            'Coaching Integrations',
            'Built for Trainerize and hybrid coaching businesses.',
            'accent',
            '3',
            [
                { icon: 'book', title: 'Workout Plans', description: 'Assign and track custom programs linked to ABC member IDs.' },
                { icon: 'monitor', title: 'Progress Photos', description: 'Secure media sync with member consent and retention policies.' },
                { icon: 'rocket', title: 'Habit Coaching', description: 'Daily check-ins, streaks, and messaging tied to club membership tiers.' },
            ],
        ),
        spacer(),
        button('Client Engagement API', '/client-engagement-abc013', 'text'),
    ];
}

function browseIntegrationsDocument(): BlockNoteDocument {
    return [
        apiList('Partner Marketplace', 'list'),
        spacer(),
        section(
            'Filter by Platform',
            'APIs are grouped under each ABC platform in the navigation tree.',
            'none',
            '4',
            [
                { icon: 'monitor', title: 'Ignite', description: 'Traditional & HVLP clubs', buttonLabel: 'Ignite', buttonLink: '/ignite-overview-abc007' },
                { icon: 'cloud', title: 'Glofox', description: 'Boutique studios', buttonLabel: 'Glofox', buttonLink: '/glofox-overview-abc010' },
                { icon: 'code', title: 'Trainerize', description: 'Coaching apps', buttonLabel: 'Trainerize', buttonLink: '/trainerize-overview-abc012' },
                { icon: 'globe', title: 'Evo', description: 'Latin America operations', buttonLabel: 'Evo', buttonLink: '/evo-overview-abc014' },
            ],
        ),
    ];
}

function igniteOverviewDocument(): BlockNoteDocument {
    return [
        heading(1, 'ABC Ignite'),
        richParagraph(
            { t: 'Club management software for ' },
            { t: 'traditional gyms and HVLP clubs', bold: true },
            { t: '. Ignite powers billing, access control, and member lifecycle for big-box and full-service fitness centers.' },
        ),
        spacer(),
        section(
            'Ignite API Surfaces',
            '',
            'light',
            '2',
            [
                { icon: 'key', title: 'Members & Access', description: 'Member CRUD, check-ins, and freeze/cancel workflows.', buttonLabel: 'API Reference', buttonLink: '/ignite-members-api-abc008' },
                { icon: 'database', title: 'Billing & Contracts', description: 'Recurring billing, invoices, and payment retries.', buttonLabel: 'API Reference', buttonLink: '/ignite-billing-api-abc009' },
            ],
        ),
        spacer(),
        bulletItem('40% of US clubs run on Ignite — largest reachable audience in the ecosystem'),
        bulletItem('Webhook events for every membership state transition'),
        bulletItem('Multi-club franchise support with centralized reporting'),
    ];
}

function glofoxOverviewDocument(): BlockNoteDocument {
    return [
        banner('ABC Glofox', 'Member management for boutique fitness, gyms, and studios.', 'gradient'),
        spacer(),
        card('Studio-First Data Model', 'Classes, packs, and recurring memberships tailored to boutique operators.', 'book', 'orange'),
        card('Branded Member Apps', 'White-label apps with Glofox auth — extend via partner APIs.', 'rocket', 'purple'),
        spacer(),
        paragraph('Glofox APIs focus on high-velocity studio operations: drop-in bookings, pack balances, and automated waitlist promotion.'),
        spacer(),
        button('Studio Members API', '/glofox-members-api-abc011', 'filled'),
    ];
}

function trainerizeOverviewDocument(): BlockNoteDocument {
    return [
        heading(1, 'ABC Trainerize'),
        paragraph('The coaching app to deliver engaging client experiences — in-person or virtual.'),
        spacer(),
        section(
            'Integration Highlights',
            '',
            'gray',
            '3',
            [
                { icon: 'code', title: 'Client Sync', description: 'Map Trainerize clients to ABC member records for unified billing.' },
                { icon: 'globe', title: 'Content Library', description: 'Share workout templates across franchise coaching teams.' },
                { icon: 'shield', title: 'Compliance', description: 'HIPAA-aware media handling for health coaching partners.' },
            ],
        ),
        spacer(),
        button('Explore Client Engagement', '/client-engagement-abc013', 'outlined'),
    ];
}

function evoOverviewDocument(): BlockNoteDocument {
    return [
        banner(
            'ABC Evo',
            'Gym management solution built for Latin America — localized billing, tax, and operations.',
            'dark',
        ),
        spacer(),
        card('Multi-Country Support', 'MX, BR, CO club entities with region-specific payment rails.', 'globe', 'green'),
        spacer(),
        heading(2, 'Operations API'),
        paragraph('Schedule classes, manage locations, and coordinate staff across Evo-powered clubs.'),
        button('Club Operations API', '/evo-ops-api-abc015', 'filled'),
    ];
}

function clientEngagementDocument(): BlockNoteDocument {
    return [
        heading(1, 'Client Engagement API'),
        paragraph('Trainerize-facing endpoints for coaching partners — assign programs, log workouts, and message clients tied to ABC member IDs.'),
        spacer(),
        heading(2, 'Core Resources'),
        bulletItem('POST /clients — link a Trainerize client to an ABC member'),
        bulletItem('GET /programs — list assignable workout templates'),
        bulletItem('POST /clients/{id}/messages — send in-app coaching messages'),
        bulletItem('GET /clients/{id}/progress — aggregate compliance and streak metrics'),
        spacer(),
        banner(
            'Trainerize Certification Required',
            'This API surface requires Trainerize-specific partner certification beyond standard ABC Bronze tier.',
            'indigo',
            [{ label: 'Certification Checklist', link: '/certification-checklist-abc022', variant: 'primary' }],
        ),
    ];
}

function partnerApplicationDocument(): BlockNoteDocument {
    return [
        heading(1, 'Partner Application'),
        paragraph('Submit your partner registration to begin technical discovery and sandbox provisioning.'),
        spacer(),
        numberedItem('Complete the registration form at partners.abcfitness.example.com'),
        numberedItem('Describe your integration scope and target ABC platforms (Ignite, Glofox, etc.)'),
        numberedItem('Provide a technical contact and security questionnaire responses'),
        numberedItem('Await review — typical turnaround is 5–7 business days'),
        spacer(),
        section(
            'Minimum Requirements',
            '',
            'light',
            '2',
            [
                { icon: 'shield', title: 'Security', description: 'Ability to meet ABC infosec and data privacy policies, including SOC 2 or equivalent.' },
                { icon: 'code', title: 'Engineering', description: 'In-house developers or a vendor building for a mutual ABC customer.' },
            ],
        ),
        spacer(),
        button('Request Sandbox Access', '/sandbox-access-abc017', 'filled'),
    ];
}

function sandboxAccessDocument(): BlockNoteDocument {
    return [
        banner('Sandbox Access', 'Free sandbox credentials for approved partners — test with synthetic clubs and members.', 'light'),
        spacer(),
        subscriptionFlow(),
        spacer(),
        card('What You Get', 'Three sandbox clubs, 50 members each, webhook simulator, and relaxed rate limits.', 'book', 'blue'),
    ];
}

function firstApiCallDocument(): BlockNoteDocument {
    return [
        heading(1, 'Your First API Call'),
        richParagraph(
            { t: 'Make an authenticated request in ' },
            { t: 'under 10 minutes', bold: true },
            { t: ' after sandbox approval.' },
        ),
        spacer(),
        numberedItem('Create a partner application in My Workspace → Applications'),
        numberedItem('Subscribe to Member Management API (sandbox plan)'),
        numberedItem('POST /oauth/token with client_credentials grant'),
        numberedItem('GET /members?club_id={sandbox_club_id}&limit=5 with Bearer token'),
        spacer(),
        banner(
            'Stuck?',
            'Join partner office hours every Tuesday, or review authentication and webhook guides.',
            'light',
            [
                { label: 'Authentication', link: '/authentication-abc019', variant: 'primary' },
                { label: 'Webhooks', link: '/webhooks-abc020', variant: 'secondary' },
            ],
        ),
    ];
}

function authenticationDocument(): BlockNoteDocument {
    return [
        banner(
            'Authentication & Authorization',
            'Secure your integration with OAuth 2.0, API keys, and scoped access tailored to partner tiers.',
            'dark',
        ),
        spacer(),
        section(
            'Auth Methods',
            '',
            'light',
            '2',
            [
                { icon: 'key', title: 'OAuth 2.0 (Recommended)', description: 'Client Credentials for server-to-server integrations. Authorization Code for member-facing apps acting on behalf of users.' },
                { icon: 'shield', title: 'API Keys', description: 'Simple key-based auth for read-only reporting and internal tools. Keys are scoped per club and per API surface.' },
            ],
        ),
        spacer(),
        heading(2, 'OAuth 2.0 Client Credentials'),
        paragraph('Use this flow for backend services that sync member data, process webhooks, or run scheduled jobs.'),
        numberedItem('Exchange client_id and client_secret at the token endpoint'),
        numberedItem('Request only the scopes your integration needs'),
        numberedItem('Include Authorization: Bearer {token} on every API request'),
        numberedItem('Handle 401 responses by refreshing the token before retrying'),
        spacer(),
        heading(2, 'Partner Tier Scopes'),
        paragraph('Available scopes depend on your partner certification level:'),
        bulletItem('Bronze — members.read, classes.read'),
        bulletItem('Silver — members.write, check-ins.write, webhooks.manage'),
        bulletItem('Gold — billing.read, billing.write, multi-club.admin'),
        spacer(),
        section(
            'Security Requirements',
            'All partners must meet ABC Fitness infosec and data privacy policies.',
            'gray',
            '3',
            [
                { icon: 'shield', title: 'TLS 1.2+', description: 'All API traffic must use HTTPS. Certificate pinning is recommended for mobile SDKs.' },
                { icon: 'database', title: 'Data Minimization', description: 'Request only the member fields your integration requires. PII must be encrypted at rest.' },
                { icon: 'monitor', title: 'Audit Logging', description: 'Log all API calls with correlation IDs. Retain logs for 90 days minimum.' },
            ],
        ),
    ];
}

function webhooksDocument(): BlockNoteDocument {
    return [
        heading(1, 'Webhooks & Events'),
        paragraph('Receive real-time notifications when members check in, memberships change, classes book up, or billing events occur — without polling.'),
        spacer(),
        card('Push, Don\'t Poll', 'Webhooks deliver member lifecycle events within seconds. Ideal for CRM sync, engagement triggers, and analytics pipelines.', 'rocket', 'orange'),
        card('Signed Payloads', 'Every delivery includes an HMAC-SHA256 signature in the X-ABC-Signature header for verification.', 'shield', 'blue'),
        spacer(),
        heading(2, 'Setup'),
        numberedItem('Create a publicly accessible HTTPS endpoint (TLS 1.2+)'),
        numberedItem('Register the URL in your application settings under Webhooks'),
        numberedItem('Select event types and specify a signing secret'),
        numberedItem('Use the sandbox replay tool to test delivery before going live'),
        spacer(),
        section(
            'Event Catalog',
            'Subscribe to the events that drive your integration logic.',
            'dark',
            '2',
            [
                { icon: 'code', title: 'Member Events', description: 'member.created, member.updated, membership.frozen, membership.cancelled, check_in.recorded' },
                { icon: 'server', title: 'Operations Events', description: 'class.booked, class.cancelled, invoice.created, invoice.paid, payment.failed' },
            ],
        ),
        spacer(),
        heading(2, 'Delivery Guarantees'),
        bulletItem('At-least-once delivery with exponential backoff (up to 8 retries over 48 hours)'),
        bulletItem('Return 2xx within 10 seconds — process asynchronously after acknowledging'),
        bulletItem('Use event IDs for idempotent processing; duplicates may occur'),
        spacer(),
        banner(
            'Verify Every Payload',
            'Never process webhook events without validating the HMAC signature against your signing secret.',
            'indigo',
            [{ label: 'Security Policies', link: '/authentication-abc019', variant: 'primary' }],
        ),
    ];
}

function sandboxDocument(): BlockNoteDocument {
    return [
        banner(
            'Sandbox Environment',
            'Test integrations safely with synthetic clubs, members, and billing data that mirrors production behavior.',
            'light',
            [{ label: 'Get Sandbox Access', link: '/sandbox-access-abc017', variant: 'primary' }],
        ),
        spacer(),
        section(
            'Sandbox Resources',
            'Pre-provisioned test data to accelerate development.',
            'accent',
            '3',
            [
                { icon: 'database', title: 'Test Clubs', description: 'Three sandbox clubs: traditional gym, boutique studio, and coaching business — each with distinct member profiles.' },
                { icon: 'globe', title: 'Test Members', description: '50 synthetic members per club with varied membership types, freeze states, and check-in history.' },
                { icon: 'settings', title: 'Webhook Simulator', description: 'Trigger any event type on demand from the Partner Portal without waiting for real activity.' },
            ],
        ),
        spacer(),
        heading(2, 'Sandbox vs Production'),
        paragraph('Sandbox and production share identical API contracts. Only the base URL and credentials differ.'),
        bulletItem('Sandbox base URL: https://sandbox.abcfitness.example.com/v3'),
        bulletItem('Rate limits are relaxed (10,000 req/hour) for development'),
        bulletItem('Billing endpoints return mock invoices — no real charges'),
        bulletItem('Certification review requires a successful sandbox demo before production keys are issued'),
        spacer(),
        heading(2, 'Test Scenarios'),
        numberedItem('Create a member → verify member.created webhook fires'),
        numberedItem('Record a check-in → confirm access rules for frozen memberships'),
        numberedItem('Book a full class → validate waitlist behavior'),
        numberedItem('Simulate a failed payment → test dunning notification flow'),
        spacer(),
        button('Subscribe to Sandbox APIs', '/subscribe-abc024', 'filled'),
    ];
}

function certificationChecklistDocument(): BlockNoteDocument {
    return [
        heading(1, 'Certification Checklist'),
        paragraph('Complete these steps before requesting production credentials and marketplace listing.'),
        spacer(),
        numberedItem('Pass automated integration tests against all subscribed sandbox APIs'),
        numberedItem('Demonstrate webhook signature verification and idempotent event handling'),
        numberedItem('Submit security questionnaire and privacy policy URLs'),
        numberedItem('Record a 5-minute demo video of the end-to-end member workflow'),
        numberedItem('Schedule live review with ABC Partner Engineering'),
        spacer(),
        section(
            'Certification Tiers',
            '',
            'accent',
            '3',
            [
                { icon: 'code', title: 'Bronze', description: 'Read-only member data, single platform, community support.' },
                { icon: 'rocket', title: 'Silver', description: 'Read/write, webhooks, up to two platforms, standard SLA.' },
                { icon: 'shield', title: 'Gold', description: 'Multi-platform, billing access, co-marketing, dedicated partner manager.' },
            ],
        ),
        spacer(),
        banner(
            'Ready for Review?',
            'Submit your certification package from My Workspace once every item is complete.',
            'dark',
            [{ label: 'My Applications', link: '/applications-abc026', variant: 'primary' }],
        ),
    ];
}

function partnerProgramDocument(): BlockNoteDocument {
    return [
        banner(
            'Partner Program',
            'Join the ABC Fitness technology ecosystem and reach fitness businesses at every scale.',
            'gradient',
            [{ label: 'Apply Now', link: 'https://partners.abcfitness.example.com/register', variant: 'primary' }],
        ),
        spacer(),
        section(
            'Program Benefits',
            'Defined service levels create an unmatched client experience at every partner tier.',
            'none',
            '3',
            [
                { icon: 'rocket', title: 'Marketplace Listing', description: 'Featured placement in the ABC Fitness Partner Marketplace for certified integrations.' },
                { icon: 'globe', title: 'Co-Selling Support', description: 'Joint go-to-market with ABC sales teams for Gold-tier partners.' },
                { icon: 'monitor', title: 'Developer Resources', description: 'Dedicated Slack channel, office hours, and early access to new API surfaces.' },
            ],
        ),
        spacer(),
        heading(2, 'Frequently Asked Questions'),
        heading(3, 'What are the benefits of joining the ABC Partner Program?'),
        paragraph('The program delivers a complete ecosystem with defined integration and service levels. Partners receive fair value for access to ABC\'s privileged club relationships and marketplace visibility.'),
        heading(3, 'What does it cost to participate?'),
        paragraph('The partner program has several tiers with related terms discussed during your evaluation and onboarding process.'),
        heading(3, 'How do I get started?'),
        paragraph('Submit a partner registration at the Partner Portal. Our team will review your request and schedule a technical discovery call.'),
        heading(3, 'Are there minimum requirements?'),
        paragraph('You must either be an ABC customer with in-house developers, or a vendor supporting a mutual customer. All partners must meet ABC infosec and data privacy policies.'),
        heading(3, 'How do partner tiers work?'),
        paragraph('ABC reviews integrations regularly to ensure leading technology providers are positioned at the proper tier. Proven market leaders with complementary solutions may qualify for accelerated tier placement.'),
        spacer(),
        banner(
            'New & Noteworthy Partners',
            'EGYM, MyFitnessPal, and Truemed recently joined the ecosystem — see what\'s possible when you integrate with ABC Fitness.',
            'dark',
            [{ label: 'Browse Marketplace', link: '/browse-integrations-abc006', variant: 'secondary' }],
        ),
    ];
}

function subscribeDocument(): BlockNoteDocument {
    return [
        heading(1, 'Subscribe to Partner APIs'),
        paragraph('Select an API and plan to begin integration. Sandbox access is free for all approved partners.'),
        spacer(),
        subscriptionFlow(),
        spacer(),
        section(
            'Partner Plans',
            'Scale your integration as your club footprint grows.',
            'light',
            '2',
            [
                { icon: 'code', title: 'Developer', description: 'Free sandbox access, 5,000 API calls/month, community support. Perfect for building and certifying your integration.' },
                { icon: 'rocket', title: 'Production', description: 'Unlimited production calls, SLA-backed uptime, dedicated partner manager, and marketplace listing eligibility.' },
            ],
        ),
    ];
}

function subscriptionsDocument(): BlockNoteDocument {
    return [
        heading(1, 'My API Subscriptions'),
        paragraph('Monitor usage across your partner applications, rotate credentials, and manage plan upgrades.'),
        spacer(),
        subscriptionViewer(),
    ];
}

function applicationsDocument(): BlockNoteDocument {
    return [
        heading(1, 'Partner Applications'),
        paragraph('Each application represents an integration product. Manage credentials, webhook endpoints, and team access per application.'),
        spacer(),
        applications(),
    ];
}

// ---------------------------------------------------------------------------
// Navigation — standalone deep tree (not based on dummy-navigation)
// ---------------------------------------------------------------------------

function navId(portalId: string, key: string): string {
    return `${portalId}-${key}`;
}

function createAbcFitnessNavigation(portalId: string): PortalNavigationItem[] {
    const marketplace = navId(portalId, 'fld-marketplace');
    const byCategory = navId(portalId, 'fld-by-category');
    const platforms = navId(portalId, 'fld-platforms');
    const ignite = navId(portalId, 'fld-ignite');
    const igniteApis = navId(portalId, 'fld-ignite-apis');
    const glofox = navId(portalId, 'fld-glofox');
    const trainerize = navId(portalId, 'fld-trainerize');
    const evo = navId(portalId, 'fld-evo');
    const build = navId(portalId, 'fld-build');
    const onboarding = navId(portalId, 'fld-onboarding');
    const developerGuides = navId(portalId, 'fld-developer-guides');
    const certification = navId(portalId, 'fld-certification');
    const workspace = navId(portalId, 'fld-workspace');

    return [
        // ── Top level ──────────────────────────────────────────────────────
        { id: navId(portalId, 'nav-home'), portalId, title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-abc001' },

        { id: marketplace, portalId, title: 'Marketplace', type: 'FOLDER', parentId: null, order: 1, slug: 'marketplace-abc100' },
        { id: navId(portalId, 'nav-featured'), portalId, title: 'New & Noteworthy', type: 'PAGE', parentId: marketplace, order: 0, slug: 'featured-abc002' },
        { id: byCategory, portalId, title: 'By Category', type: 'FOLDER', parentId: marketplace, order: 1, slug: 'by-category-abc101' },
        { id: navId(portalId, 'nav-member-experience'), portalId, title: 'Member Experience', type: 'PAGE', parentId: byCategory, order: 0, slug: 'member-experience-abc003' },
        { id: navId(portalId, 'nav-club-ops-category'), portalId, title: 'Club Operations', type: 'PAGE', parentId: byCategory, order: 1, slug: 'club-ops-category-abc004' },
        { id: navId(portalId, 'nav-wellness-coaching'), portalId, title: 'Wellness & Coaching', type: 'PAGE', parentId: byCategory, order: 2, slug: 'wellness-coaching-abc005' },
        { id: navId(portalId, 'nav-browse-integrations'), portalId, title: 'Browse All', type: 'PAGE', parentId: marketplace, order: 2, slug: 'browse-integrations-abc006' },

        { id: platforms, portalId, title: 'Platforms', type: 'FOLDER', parentId: null, order: 2, slug: 'platforms-abc102' },

        { id: ignite, portalId, title: 'ABC Ignite', type: 'FOLDER', parentId: platforms, order: 0, slug: 'ignite-abc103' },
        { id: navId(portalId, 'nav-ignite-overview'), portalId, title: 'Overview', type: 'PAGE', parentId: ignite, order: 0, slug: 'ignite-overview-abc007' },
        { id: igniteApis, portalId, title: 'APIs', type: 'FOLDER', parentId: ignite, order: 1, slug: 'ignite-apis-abc104' },
        {
            id: navId(portalId, 'nav-ignite-members-api'), portalId, title: 'Members & Access', type: 'PAGE',
            contentType: 'OPENAPI', renderer: 'swagger', specSource: { type: 'INLINE', content: MEMBER_API_SPEC },
            parentId: igniteApis, order: 0, slug: 'ignite-members-api-abc008',
        },
        {
            id: navId(portalId, 'nav-ignite-billing-api'), portalId, title: 'Billing & Contracts', type: 'PAGE',
            contentType: 'OPENAPI', renderer: 'swagger', specSource: { type: 'INLINE', content: CLUB_OPS_API_SPEC },
            parentId: igniteApis, order: 1, slug: 'ignite-billing-api-abc009',
        },

        { id: glofox, portalId, title: 'ABC Glofox', type: 'FOLDER', parentId: platforms, order: 1, slug: 'glofox-abc105' },
        { id: navId(portalId, 'nav-glofox-overview'), portalId, title: 'Overview', type: 'PAGE', parentId: glofox, order: 0, slug: 'glofox-overview-abc010' },
        {
            id: navId(portalId, 'nav-glofox-members-api'), portalId, title: 'Studio Members', type: 'PAGE',
            contentType: 'OPENAPI', renderer: 'swagger', specSource: { type: 'INLINE', content: MEMBER_API_SPEC },
            parentId: glofox, order: 1, slug: 'glofox-members-api-abc011',
        },

        { id: trainerize, portalId, title: 'ABC Trainerize', type: 'FOLDER', parentId: platforms, order: 2, slug: 'trainerize-abc106' },
        { id: navId(portalId, 'nav-trainerize-overview'), portalId, title: 'Overview', type: 'PAGE', parentId: trainerize, order: 0, slug: 'trainerize-overview-abc012' },
        { id: navId(portalId, 'nav-client-engagement'), portalId, title: 'Client Engagement', type: 'PAGE', parentId: trainerize, order: 1, slug: 'client-engagement-abc013' },

        { id: evo, portalId, title: 'ABC Evo', type: 'FOLDER', parentId: platforms, order: 3, slug: 'evo-abc107' },
        { id: navId(portalId, 'nav-evo-overview'), portalId, title: 'Overview', type: 'PAGE', parentId: evo, order: 0, slug: 'evo-overview-abc014' },
        {
            id: navId(portalId, 'nav-evo-ops-api'), portalId, title: 'Club Operations', type: 'PAGE',
            contentType: 'OPENAPI', renderer: 'swagger', specSource: { type: 'INLINE', content: CLUB_OPS_API_SPEC },
            parentId: evo, order: 1, slug: 'evo-ops-api-abc015',
        },

        { id: build, portalId, title: 'Build', type: 'FOLDER', parentId: null, order: 3, slug: 'build-abc108' },

        { id: onboarding, portalId, title: 'Onboarding', type: 'FOLDER', parentId: build, order: 0, slug: 'onboarding-abc109' },
        { id: navId(portalId, 'nav-partner-application'), portalId, title: 'Partner Application', type: 'PAGE', parentId: onboarding, order: 0, slug: 'partner-application-abc016' },
        { id: navId(portalId, 'nav-sandbox-access'), portalId, title: 'Sandbox Access', type: 'PAGE', parentId: onboarding, order: 1, slug: 'sandbox-access-abc017' },
        { id: navId(portalId, 'nav-first-api-call'), portalId, title: 'First API Call', type: 'PAGE', parentId: onboarding, order: 2, slug: 'first-api-call-abc018' },

        { id: developerGuides, portalId, title: 'Developer Guides', type: 'FOLDER', parentId: build, order: 1, slug: 'developer-guides-abc110' },
        { id: navId(portalId, 'nav-authentication'), portalId, title: 'Authentication', type: 'PAGE', parentId: developerGuides, order: 0, slug: 'authentication-abc019' },
        { id: navId(portalId, 'nav-webhooks'), portalId, title: 'Webhooks & Events', type: 'PAGE', parentId: developerGuides, order: 1, slug: 'webhooks-abc020' },

        { id: certification, portalId, title: 'Certification', type: 'FOLDER', parentId: build, order: 2, slug: 'certification-abc111' },
        { id: navId(portalId, 'nav-sandbox'), portalId, title: 'Sandbox Environment', type: 'PAGE', parentId: certification, order: 0, slug: 'sandbox-testing-abc021' },
        { id: navId(portalId, 'nav-certification-checklist'), portalId, title: 'Certification Checklist', type: 'PAGE', parentId: certification, order: 1, slug: 'certification-checklist-abc022' },

        { id: navId(portalId, 'nav-partner-program'), portalId, title: 'Partner Program', type: 'PAGE', parentId: null, order: 4, slug: 'partner-program-abc023' },

        { id: workspace, portalId, title: 'My Workspace', type: 'FOLDER', parentId: null, order: 5, slug: 'workspace-abc112' },
        { id: navId(portalId, 'nav-subscribe'), portalId, title: 'Subscribe', type: 'PAGE', parentId: workspace, order: 0, slug: 'subscribe-abc024' },
        { id: navId(portalId, 'nav-subscriptions'), portalId, title: 'Subscriptions', type: 'PAGE', parentId: workspace, order: 1, slug: 'subscriptions-abc025' },
        { id: navId(portalId, 'nav-applications'), portalId, title: 'Applications', type: 'PAGE', parentId: workspace, order: 2, slug: 'applications-abc026' },

        // ── Footer & user menu ─────────────────────────────────────────────
        {
            id: navId(portalId, 'footer-support'), portalId, title: 'Partner Support', type: 'LINK',
            parentId: null, order: 0, slug: 'support-footer-abc201', url: 'https://support.abcfitness.example.com', area: 'FOOTER',
        },
        {
            id: navId(portalId, 'footer-status'), portalId, title: 'API Status', type: 'LINK',
            parentId: null, order: 1, slug: 'status-footer-abc202', url: 'https://status.abcfitness.example.com', area: 'FOOTER',
        },
        {
            id: navId(portalId, 'footer-privacy'), portalId, title: 'Privacy & Legal', type: 'LINK',
            parentId: null, order: 2, slug: 'privacy-footer-abc203', url: 'https://abcfitness.example.com/privacy', area: 'FOOTER',
        },
        {
            id: navId(portalId, 'menu-profile'), portalId, title: 'My Profile', type: 'LINK',
            parentId: null, order: 0, slug: 'profile-menu-abc301', url: '/profile', area: 'USER_MENU',
        },
        {
            id: navId(portalId, 'menu-dashboard'), portalId, title: 'Partner Dashboard', type: 'LINK',
            parentId: null, order: 1, slug: 'dashboard-menu-abc302', url: 'https://partners.abcfitness.example.com', area: 'USER_MENU',
        },
        {
            id: navId(portalId, 'menu-logout'), portalId, title: 'Log out', type: 'LINK',
            parentId: null, order: 2, slug: 'logout-menu-abc303', url: '/logout', area: 'USER_MENU',
        },
    ];
}

function createAbcFitnessPageContents(portalId: string, navItems: readonly PortalNavigationItem[]): PageContent[] {
    const documentMap: Record<string, () => BlockNoteDocument> = {
        'nav-home': homeDocument,
        'nav-featured': featuredPartnersDocument,
        'nav-member-experience': memberExperienceDocument,
        'nav-club-ops-category': clubOperationsCategoryDocument,
        'nav-wellness-coaching': wellnessCoachingDocument,
        'nav-browse-integrations': browseIntegrationsDocument,
        'nav-ignite-overview': igniteOverviewDocument,
        'nav-glofox-overview': glofoxOverviewDocument,
        'nav-trainerize-overview': trainerizeOverviewDocument,
        'nav-evo-overview': evoOverviewDocument,
        'nav-client-engagement': clientEngagementDocument,
        'nav-partner-application': partnerApplicationDocument,
        'nav-sandbox-access': sandboxAccessDocument,
        'nav-first-api-call': firstApiCallDocument,
        'nav-authentication': authenticationDocument,
        'nav-webhooks': webhooksDocument,
        'nav-sandbox': sandboxDocument,
        'nav-certification-checklist': certificationChecklistDocument,
        'nav-partner-program': partnerProgramDocument,
        'nav-subscribe': subscribeDocument,
        'nav-subscriptions': subscriptionsDocument,
        'nav-applications': applicationsDocument,
    };

    return navItems
        .filter((item): item is PortalNavigationItem & { type: 'PAGE' } => item.type === 'PAGE')
        .map(item => {
            const key = item.id.slice(`${portalId}-`.length);

            if (item.contentType === 'OPENAPI') {
                const openApiItem = item as PortalNavigationItem & {
                    contentType: 'OPENAPI';
                    renderer: 'swagger';
                    specSource: { type: 'INLINE'; content: string };
                };
                return {
                    id: `page-content-${item.id}`,
                    portalId,
                    navigationItemId: item.id,
                    contentType: 'OPENAPI' as const,
                    renderer: openApiItem.renderer,
                    specContent: openApiItem.specSource.content,
                };
            }

            const docFn = documentMap[key];
            if (!docFn) {
                throw new Error(`Missing ABC Fitness page document for navigation key: ${key}`);
            }

            return {
                id: `page-content-${item.id}`,
                portalId,
                navigationItemId: item.id,
                contentType: 'BLOCK' as const,
                document: docFn(),
            };
        });
}

// ---------------------------------------------------------------------------
// Public seed function — self-contained portal (nav + all page content)
// ---------------------------------------------------------------------------

export async function seedRichAbcFitnessPages(portalId: string): Promise<void> {
    const navItems = createAbcFitnessNavigation(portalId);
    const pageContents = createAbcFitnessPageContents(portalId, navItems);

    await Promise.all(navItems.map(item => saveNavItem(item)));
    await Promise.all(pageContents.map(content => savePageContent(content)));
}
