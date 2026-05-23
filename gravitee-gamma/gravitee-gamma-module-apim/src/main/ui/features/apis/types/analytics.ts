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

export interface AnalyticsLogging {
    mode?: { entrypoint?: boolean; endpoint?: boolean };
    phase?: { request?: boolean; response?: boolean };
    content?: { headers?: boolean; payload?: boolean };
    condition?: string;
}

export type MaskingType = 'FULL' | 'PARTIAL';

export interface MaskingStrategy {
    type: MaskingType;
    /** FULL: replacement text (defaults to [REDACTED]); PARTIAL: mask character (defaults to *) */
    replacement?: string;
    prefixLength?: number;
    suffixLength?: number;
}

export interface RedactionRule {
    attributeNamePattern: string;
    maskingStrategy?: MaskingStrategy;
    /** Regex partial-match filter; rule only fires when the attribute value matches */
    valuePattern?: string;
}

export interface AnalyticsTracing {
    enabled?: boolean;
    verbose?: boolean;
    redaction?: {
        defaultReplacement?: string;
        rules?: RedactionRule[];
    };
}

export interface Analytics {
    enabled?: boolean;
    logging?: AnalyticsLogging;
    tracing?: AnalyticsTracing;
    otelLogs?: { enabled?: boolean };
    sampling?: { type?: string; value?: string };
}
