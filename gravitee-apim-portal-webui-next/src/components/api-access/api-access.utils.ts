/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { PlanSecurityEnum } from '../../entities/plan/plan';

export function formatCurlCommandLine(
  entrypointUrl: string,
  planSecurity: PlanSecurityEnum | undefined,
  apiKeyHeader?: string,
  apiKey?: string,
): string {
  if (!entrypointUrl) {
    return '';
  }

  let curlHeader = '';
  switch (planSecurity) {
    case 'JWT':
    case 'OAUTH2':
      curlHeader = '--header "Authorization: Bearer {{ ACCESS_TOKEN }}" ';
      break;
    case 'API_KEY':
      if (apiKeyHeader) {
        curlHeader = `--header "${apiKeyHeader}: ${apiKey || '{{ API_KEY }}'}" `;
      }
      break;
  }

  return `curl ${curlHeader}${entrypointUrl}`;
}
