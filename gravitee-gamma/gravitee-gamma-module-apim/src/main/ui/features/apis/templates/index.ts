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
import { apiKeyTemplate } from './apiKeyTemplate';
import { jwtTemplate } from './jwtTemplate';
import { keylessTemplate } from './keylessTemplate';
import { oauth2Template } from './oauth2Template';
import type { ApiCreationTemplate } from '../types/template.types';

export { apiKeyTemplate, jwtTemplate, oauth2Template, keylessTemplate };

export const apiCreationTemplates: readonly ApiCreationTemplate[] = [apiKeyTemplate, jwtTemplate, oauth2Template, keylessTemplate];
