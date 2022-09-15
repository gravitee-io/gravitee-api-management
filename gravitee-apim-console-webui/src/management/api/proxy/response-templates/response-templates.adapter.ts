/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { flatMap } from 'lodash';

import { Api } from '../../../../entities/api';

export type ResponseTemplate = {
  id: string;
  key: string;
  contentType: string;
  statusCode?: number;
  body?: string;
  headers?: Record<string, string>;
};

export const toResponseTemplates = (responseTemplates: Api['response_templates']): ResponseTemplate[] => {
  if (!responseTemplates) {
    return [];
  }

  return flatMap(Object.entries(responseTemplates), ([key, responseTemplates]) => {
    return [
      ...Object.entries(responseTemplates).map(([contentType, responseTemplate]) => ({
        id: `${key}-${contentType}`,
        key: key,
        contentType,
        statusCode: responseTemplate.status,
        body: responseTemplate.body,
        headers: responseTemplate.headers,
      })),
    ];
  });
};

export const fromResponseTemplates = (responseTemplates: ResponseTemplate[]): Api['response_templates'] => {
  return responseTemplates.reduce((acc, responseTemplate) => {
    const { key, contentType, statusCode, body, headers } = responseTemplate;
    if (!acc[key]) {
      acc[key] = {};
    }
    acc[key][contentType] = {
      status: statusCode,
      body,
      headers,
    };
    return acc;
  }, {} as Api['response_templates']);
};
