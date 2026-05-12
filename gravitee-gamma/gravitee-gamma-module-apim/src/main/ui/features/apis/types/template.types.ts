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
import type { ApiCreationState } from './models';
import type { StepId } from '../../../utils/schema';

export type TemplateIconKind = 'api-key' | 'jwt' | 'oauth2' | 'keyless';

export type ApiCreationTemplate = {
    readonly id: string;
    readonly label: string;
    readonly headline?: string;
    readonly description: string;
    readonly tags: readonly string[];
    readonly icon: TemplateIconKind;
    readonly featured?: boolean;
    readonly caution?: { readonly label: string; readonly description: string };
    readonly steps: readonly StepId[];
    readonly defaults: Partial<ApiCreationState>;
};
