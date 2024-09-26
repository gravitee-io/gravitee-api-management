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

import { ApiV1, ApiV2 } from '../../../../entities/management-api-v2';

export type ResponseTemplate = {
  id: string;
  key: string;
  contentType: string;
  statusCode?: number;
  body?: string;
  headers?: Record<string, string>;
  propagateErrorKeyToLogs?: boolean;
};

export const toResponseTemplates = (responseTemplates: (ApiV1 | ApiV2)['responseTemplates']): ResponseTemplate[] => {
  if (!responseTemplates) {
    return [];
  }

  return flatMap(Object.entries(responseTemplates), ([key, responseTemplates]) => {
    return [
      ...Object.entries(responseTemplates).map(([contentType, responseTemplate]) => ({
        id: `${key}-${contentType}`,
        key: key,
        contentType,
        statusCode: responseTemplate.statusCode,
        body: responseTemplate.body,
        headers: responseTemplate.headers,
        propagateErrorKeyToLogs: responseTemplate.propagateErrorKeyToLogs,
      })),
    ];
  });
};

export const fromResponseTemplates = (responseTemplates: ResponseTemplate[]): (ApiV1 | ApiV2)['responseTemplates'] => {
  return responseTemplates.reduce((acc, responseTemplate) => {
    const { key, contentType, statusCode, body, headers, propagateErrorKeyToLogs } = responseTemplate;
    if (!acc[key]) {
      acc[key] = {};
    }
    acc[key][contentType] = {
      statusCode,
      body,
      headers,
      propagateErrorKeyToLogs,
    };
    return acc;
  }, {} as (ApiV1 | ApiV2)['responseTemplates']);
};
