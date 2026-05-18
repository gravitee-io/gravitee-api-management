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

import { MaskingStrategy } from './redactionV4';

export type PayloadFormat = 'JSON' | 'XML' | 'AUTO';
export type PayloadPhase = 'REQUEST' | 'RESPONSE' | 'BOTH';

export interface PayloadMaskingRule {
  /** JSONPath (e.g. "$.password") or XPath (e.g. "//password") */
  path: string;
  maskingStrategy?: MaskingStrategy;
  /** Which traffic direction(s) this rule applies to. Default: BOTH */
  phase?: PayloadPhase;
  /** Body format. AUTO detects from Content-Type and body heuristics. Default: AUTO */
  format?: PayloadFormat;
}

export interface PayloadMaskingConfig {
  /** Fallback replacement text for FULL rules with no per-rule replacement. */
  defaultReplacement?: string;
  rules: PayloadMaskingRule[];
}
