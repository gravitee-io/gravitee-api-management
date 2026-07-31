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

export type ApiImportFormat = 'gravitee' | 'openapi' | 'wsdl';

/**
 * Mirrors the backend `ImportSwaggerDescriptor`. There's no `type: INLINE | URL` field — the backend
 * (`OAIDomainServiceImpl.convert`) auto-detects whether `payload` is a URL and fetches it server-side.
 */
export interface ImportSwaggerDescriptor {
    payload: string;
    withDocumentation?: boolean;
    withOASValidationPolicy?: boolean;
}

/** Mirrors the backend `ImportWsdlDescriptor` — unlike OpenAPI, WSDL requires an explicit `type` flag. */
export interface ImportWsdlDescriptor {
    payload: string;
    type: 'INLINE' | 'URL';
    withDocumentation?: boolean;
    withOASValidationPolicy?: boolean;
    withPolicies?: string[];
}

export type ApiImportSubmission =
    | { format: 'gravitee'; source: 'local'; definition: unknown }
    | { format: 'gravitee'; source: 'remote'; url: string }
    | { format: 'openapi'; descriptor: ImportSwaggerDescriptor }
    | { format: 'wsdl'; descriptor: ImportWsdlDescriptor };
