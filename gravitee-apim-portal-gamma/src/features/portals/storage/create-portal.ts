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
import {
    getPortalTemplateScreenshot,
    PORTAL_TEMPLATES,
    type PortalTemplateId,
} from '../templates/portal-templates';
import { DEFAULT_DOCUMENTATION_VIEWER, DEFAULT_PORTAL_LABEL, type DeveloperPortal } from '../types';
import { DEFAULT_PAGE_WIDTH } from '../../editor/constants/page-width';
import { createDefaultPortalTenant } from '../../tenants/storage/create-default-portal-tenant';
import { savePortal } from './portals.storage';
import { seedPortalFromTemplate } from './seed-portal-template';

export async function createPortalFromTemplate(templateId: PortalTemplateId): Promise<DeveloperPortal> {
    const template = PORTAL_TEMPLATES[templateId];
    const id = crypto.randomUUID();
    const portal: DeveloperPortal = {
        id,
        name: template.defaultName,
        description: '',
        screenshotDataUrl: getPortalTemplateScreenshot(template),
        updatedAt: new Date().toISOString(),
        layout: template.layout,
        showFooter: true,
        pageWidth: DEFAULT_PAGE_WIDTH,
        portalIconUrl: '',
        portalLabel: DEFAULT_PORTAL_LABEL,
        footerLinks: [],
        userMenuItems: [],
        portalUrl: '',
        documentationViewer: DEFAULT_DOCUMENTATION_VIEWER,
    };

    await savePortal(portal);
    await seedPortalFromTemplate(id, templateId);
    await createDefaultPortalTenant(id);

    return portal;
}
