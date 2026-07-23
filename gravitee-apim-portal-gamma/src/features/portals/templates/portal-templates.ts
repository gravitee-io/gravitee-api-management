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
import { createDummyNavigation, createDummyPageContents } from '../storage/dummy-navigation';
import { createPlaceholderScreenshot } from '../storage/dummy-portals';
import { saveNavItem } from '../storage/navigation-items.storage';
import { savePageContent } from '../storage/page-contents.storage';
import { seedAiWorkspacePortal } from '../storage/seed-ai-workspace-portal';
import { seedRichActiveFitnessPages } from '../storage/rich-active-fitness-pages';
import { seedRichPaymentPages } from '../storage/rich-payment-pages';
import { seedDemoApiReferenceNav } from '../storage/seed-demo-api-reference';
import type { PortalLayout } from '../types';

export type PortalTemplateId = 'blank' | 'starter' | 'payments' | 'active-fitness' | 'ai-workspace';

export interface PortalTemplateDefinition {
    readonly id: PortalTemplateId;
    readonly label: string;
    readonly description: string;
    readonly defaultName: string;
    readonly layout: PortalLayout;
    readonly screenshotLabel: string;
    readonly screenshotColor: string;
    readonly seed: (portalId: string) => Promise<void>;
}

async function seedStarterTemplate(portalId: string): Promise<void> {
    const navItems = createDummyNavigation(portalId);
    const pageContents = createDummyPageContents(portalId, navItems);

    await Promise.all(navItems.map(item => saveNavItem(item)));
    await Promise.all(pageContents.map(content => savePageContent(content)));
}

async function seedPaymentsTemplate(portalId: string): Promise<void> {
    await seedStarterTemplate(portalId);
    await seedRichPaymentPages(portalId);
    await seedDemoApiReferenceNav(portalId);
}

export const PORTAL_TEMPLATES: Record<PortalTemplateId, PortalTemplateDefinition> = {
    blank: {
        id: 'blank',
        label: 'Blank',
        description: 'An empty portal with no pages. A Home page is created when you open the editor.',
        defaultName: 'New Portal',
        layout: 'header-content-footer',
        screenshotLabel: 'New Portal',
        screenshotColor: '#64748b',
        seed: async () => {},
    },
    starter: {
        id: 'starter',
        label: 'Starter',
        description: 'Basic navigation with placeholder pages and a sample OpenAPI page.',
        defaultName: 'New Portal',
        layout: 'header-content-footer',
        screenshotLabel: 'Starter',
        screenshotColor: '#64748b',
        seed: seedStarterTemplate,
    },
    payments: {
        id: 'payments',
        label: 'Payments API Portal',
        description: 'Full payments demo with API catalog, subscriptions, and composable API reference.',
        defaultName: 'Payments API Portal',
        layout: 'header-content-footer',
        screenshotLabel: 'Payments API',
        screenshotColor: '#2563eb',
        seed: seedPaymentsTemplate,
    },
    'active-fitness': {
        id: 'active-fitness',
        label: 'Active Fitness Partner APIs',
        description: 'Partner marketplace with platforms, certification flows, and workspace pages.',
        defaultName: 'Active Fitness Partner APIs',
        layout: 'sidebar-content',
        screenshotLabel: 'Active Fitness',
        screenshotColor: '#dc2626',
        seed: seedRichActiveFitnessPages,
    },
    'ai-workspace': {
        id: 'ai-workspace',
        label: 'AI Workspace Portal',
        description: 'Consumer-ready AI workspace with AI key, budgets, models, snippets, and usage monitoring.',
        defaultName: 'Enterprise AI Portal',
        layout: 'sidebar-content',
        screenshotLabel: 'AI Workspace',
        screenshotColor: '#7c3aed',
        seed: seedAiWorkspacePortal,
    },
};

export const PORTAL_TEMPLATE_OPTIONS = Object.values(PORTAL_TEMPLATES);

export function getPortalTemplateScreenshot(template: PortalTemplateDefinition): string {
    return createPlaceholderScreenshot(template.screenshotLabel, template.screenshotColor);
}
