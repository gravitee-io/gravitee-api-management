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
import type { LucideIcon } from '@gravitee/graphene-core/icons';

export type AuthType = 'keyless' | 'api-key' | 'jwt' | 'oauth2' | 'mtls';
export type WizardMode = 'picker' | 'template' | 'scratch';

export interface VirtualHostEntry {
    host: string;
    path: string;
    overrideAccess: boolean;
}

export interface ProxyTemplateDefaults {
    authType: AuthType;
    apiKeyPlanName?: string;
    jwtPlanName?: string;
    jwtSignature?: string;
    jwtJwksResolver?: string;
    jwtResolverParameter?: string;
    oauth2PlanName?: string;
    oauth2Resource?: string;
    mtlsPlanName?: string;
    descriptionHint?: string;
}

export type TemplateColor = 'blue' | 'violet' | 'amber' | 'rose';

export interface ProxyTemplate {
    id: string;
    title: string;
    subtitle: string;
    description: string;
    Icon: LucideIcon;
    color: TemplateColor;
    tags: readonly string[];
    defaults: ProxyTemplateDefaults;
    notRecommended?: boolean;
    warningMessage?: string;
}

export interface WizardFormState {
    apiName: string;
    apiVersion: string;
    apiDescription: string;
    contextPath: string;
    virtualHostsEnabled: boolean;
    virtualHosts: VirtualHostEntry[];
    targetUrl: string;
    authType: AuthType;
    apiKeyPlanName: string;
    jwtPlanName: string;
    jwtSignature: string;
    jwtJwksResolver: string;
    jwtResolverParameter: string;
    oauth2PlanName: string;
    oauth2Resource: string;
    mtlsPlanName: string;
    deployImmediately: boolean;
}

export interface WizardState {
    wizardMode: WizardMode;
    templatesOpen: boolean;
    selectedTemplate: ProxyTemplate | null;
    step: number;
    form: WizardFormState;
}
