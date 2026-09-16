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
import { GRAVITEE_API_DEFINITION, type DefinitionFormatSelection, type RulesetFormat } from '../types/rulesets';

export const RULESET_NAME_MAX = 50;
export const RULESET_DESCRIPTION_MAX = 250;
export const FUNCTION_NAME_MAX = 50;
export const FUNCTION_NAME_PATTERN = /^[^/]+\.js$/;

export const GRAVITEE_API_FORMATS: ReadonlyArray<{ title: string; subtitle: string; value: RulesetFormat }> = [
    {
        title: 'Gravitee Proxy API',
        subtitle: 'v4 HTTP or TCP APIs that proxy REST, SOAP, gRPC, and GraphQL services.',
        value: 'GRAVITEE_PROXY',
    },
    {
        title: 'Gravitee Message API',
        subtitle: 'v4 message APIs that proxy Kafka, Solace, RabbitMQ, and MQTT services.',
        value: 'GRAVITEE_MESSAGE',
    },
    {
        title: 'Native Kafka',
        subtitle: 'Native Kafka-to-Kafka proxy, that sits between a Kafka cluster and a Kafka client.',
        value: 'GRAVITEE_NATIVE',
    },
    {
        title: 'Gravitee Federated API',
        subtitle: 'APIs & event streams ingested from 3rd-party providers like AWS, Azure, Confluent and Solace.',
        value: 'GRAVITEE_FEDERATION',
    },
    {
        title: 'Gravitee V2 API',
        subtitle: 'Classic Gravitee v2 REST API proxy.',
        value: 'GRAVITEE_V2',
    },
];

/** Classic Console `rulesetFormatPipe`. */
export function rulesetFormatLabel(format: RulesetFormat | undefined): string {
    switch (format) {
        case 'GRAVITEE_FEDERATION':
            return 'Gravitee Federated API';
        case 'GRAVITEE_MESSAGE':
            return 'Gravitee Message API';
        case 'GRAVITEE_PROXY':
            return 'Gravitee Proxy API';
        case 'GRAVITEE_NATIVE':
            return 'Gravitee Native API';
        case 'GRAVITEE_V2':
            return 'Gravitee V2 API';
        case 'OPENAPI':
            return 'OpenAPI';
        case 'ASYNCAPI':
            return 'AsyncAPI';
        default:
            return 'API';
    }
}

export function resolveImportedRulesetFormat(
    definitionFormat: DefinitionFormatSelection | '',
    graviteeApiFormat: RulesetFormat | '',
): RulesetFormat | undefined {
    if (definitionFormat === GRAVITEE_API_DEFINITION) {
        return graviteeApiFormat || undefined;
    }
    if (definitionFormat === 'OPENAPI' || definitionFormat === 'ASYNCAPI') {
        return definitionFormat;
    }
    return undefined;
}

export function isValidRulesetName(name: string): boolean {
    const trimmed = name.trim();
    return trimmed.length >= 1 && trimmed.length <= RULESET_NAME_MAX;
}

/** Console import uses "Ruleset name …"; edit uses "Name …". */
export function rulesetNameError(name: string, noun: 'Ruleset name' | 'Name'): string | null {
    if (isValidRulesetName(name)) {
        return null;
    }
    if (name.trim().length === 0) {
        return `${noun} is required.`;
    }
    return `${noun} can not exceed 50 characters.`;
}

export function isValidFunctionFileName(name: string): boolean {
    return name.length > 0 && name.length <= FUNCTION_NAME_MAX && FUNCTION_NAME_PATTERN.test(name);
}

/** Console snackbar copy, derived from the same pattern/max used to disable Import. */
export function functionFileNameErrors(name: string): string[] {
    const errors: string[] = [];
    if (!FUNCTION_NAME_PATTERN.test(name)) {
        errors.push(`File name should fulfill ${FUNCTION_NAME_PATTERN.source} pattern`);
    }
    if (name.length > FUNCTION_NAME_MAX) {
        errors.push('File name can not exceed 50 characters.');
    }
    return errors;
}
