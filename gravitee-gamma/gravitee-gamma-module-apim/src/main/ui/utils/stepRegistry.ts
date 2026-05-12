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
import type { FieldConfig } from './fieldRegistry';
import { fieldRegistry } from './fieldRegistry';
import type { StepId } from './schema';
import { validateStep } from './schema';
import { apiCreationTemplates } from '../features/apis/templates';
import type { ApiCreationMode, ApiCreationState, SecurityConfig } from '../features/apis/types/models';
import type { ApiCreationTemplate } from '../features/apis/types/template.types';

export type { ApiCreationTemplate };
export { apiCreationTemplates };

export type StepConfig = {
    readonly id: StepId;
    readonly label: string;
    readonly fields: readonly FieldConfig[];
    readonly isVisible?: (state: ApiCreationState) => boolean;
    readonly validate: (state: ApiCreationState) => boolean;
};

const scratchSteps: readonly StepId[] = ['api-details', 'configure-proxy', 'secure', 'review-deploy'];

const stepById = (id: StepId): Omit<StepConfig, 'validate'> => {
    switch (id) {
        case 'essentials':
            return {
                id,
                label: 'Essentials',
                fields: [
                    fieldRegistry.apiName,
                    fieldRegistry.apiVersion,
                    fieldRegistry.apiDescription,
                    fieldRegistry.contextPath,
                    fieldRegistry.targetUrl,
                    fieldRegistry.jwtResolverParam,
                    fieldRegistry.oauth2Resource,
                ],
            };
        case 'api-details':
            return {
                id,
                label: 'API Details',
                fields: [fieldRegistry.apiName, fieldRegistry.apiVersion, fieldRegistry.apiDescription],
            };
        case 'configure-proxy':
            return {
                id,
                label: 'Configure Proxy',
                fields: [fieldRegistry.contextPath, fieldRegistry.enableVirtualHosts, fieldRegistry.targetUrl],
            };
        case 'secure':
            return {
                id,
                label: 'Secure',
                fields: [
                    fieldRegistry.authType,
                    fieldRegistry.planName,
                    fieldRegistry.jwtSignature,
                    fieldRegistry.jwtJwksResolver,
                    fieldRegistry.jwtResolverParam,
                    fieldRegistry.oauth2Resource,
                ],
            };
        case 'review-deploy':
            return {
                id,
                label: 'Review & Deploy',
                fields: [fieldRegistry.deployImmediately],
            };
    }
};

export function getTemplateById(templateId: string | undefined): ApiCreationTemplate | undefined {
    if (!templateId) return undefined;
    return apiCreationTemplates.find(template => template.id === templateId);
}

export function buildSteps(mode: ApiCreationMode, templateId: string | undefined, state: ApiCreationState): readonly StepConfig[] {
    const template = mode === 'template' ? getTemplateById(templateId) : undefined;
    const stepIds = template?.steps ?? scratchSteps;

    return stepIds
        .map(id => {
            const base = stepById(id);
            const validate = (state: ApiCreationState) => validateStep(id, state).success;
            return { ...base, validate };
        })
        .filter(step => (step.isVisible ? step.isVisible(state) : true));
}

export function defaultSecurityConfig(): SecurityConfig {
    return { type: 'keyless' };
}
