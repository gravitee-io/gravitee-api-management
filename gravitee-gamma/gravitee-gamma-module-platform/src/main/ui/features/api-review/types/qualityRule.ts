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
/** Manual review rule (Classic `QualityRule`). Weight only exists for v2 quality metrics, which Gamma does not surface. */
export interface QualityRule {
    id: string;
    name: string;
    description: string;
    weight: number;
    createdAt?: string;
    updatedAt?: string;
}

export interface QualityRuleWrite {
    name: string;
    description: string;
}

export const QUALITY_RULE_NAME_MAX_LENGTH = 64;
export const QUALITY_RULE_DESCRIPTION_MAX_LENGTH = 256;
