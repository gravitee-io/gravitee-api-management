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
import { isSameApplicationType } from './applicationTypeLabels';
import type {
    ApplicationCreateSettings,
    ApplicationTypeConfig,
    CreateApplicationRequest,
    RegisterApplicationDraft,
} from '../types/applicationCreate';

interface MetadataRow {
    key: string;
    value: string;
}

export function rowsToAdditionalClientMetadata(rows: MetadataRow[]): Record<string, string> | null {
    const record: Record<string, string> = {};

    rows.forEach(row => {
        const key = row.key.trim();
        if (key) {
            record[key] = row.value.trim();
        }
    });

    return Object.keys(record).length > 0 ? record : null;
}

export function mapDraftToCreateRequest(draft: RegisterApplicationDraft, selectedType: ApplicationTypeConfig): CreateApplicationRequest {
    const isSimple = isSameApplicationType(selectedType.id, 'simple');
    const groups = draft.groups.length > 0 ? draft.groups : undefined;
    const domain = draft.domain.trim() || undefined;

    const settings: ApplicationCreateSettings = isSimple
        ? {
              app: {
                  type: draft.appType.trim() || undefined,
                  client_id: draft.appClientId.trim() || undefined,
              },
          }
        : {
              oauth: {
                  application_type: selectedType.id,
                  grant_types: draft.grantTypes,
                  redirect_uris: draft.redirectUris,
                  ...(draft.additionalClientMetadata && { additional_client_metadata: draft.additionalClientMetadata }),
              },
          };

    if (draft.clientCertificate.trim()) {
        settings.tls = { client_certificate: draft.clientCertificate.trim() };
    }

    return {
        name: draft.name.trim(),
        description: draft.description.trim(),
        domain,
        groups,
        settings,
    };
}

export function defaultGrantTypesForType(type: ApplicationTypeConfig): string[] {
    return type.default_grant_types.map(grantType => grantType.type);
}

export function isMandatoryGrantType(type: ApplicationTypeConfig, grantType: string): boolean {
    return type.mandatory_grant_types.some(mandatory => mandatory.type === grantType);
}

export function isOAuthApplicationType(typeId: string): boolean {
    return !isSameApplicationType(typeId, 'simple');
}

export interface RegisterApplicationFormValidationContext {
    requireUserGroups: boolean;
}

export function isRegisterApplicationFormValid(
    draft: RegisterApplicationDraft,
    selectedType: ApplicationTypeConfig | undefined,
    validation?: RegisterApplicationFormValidationContext,
): boolean {
    if (!selectedType) {
        return false;
    }

    if (!draft.name.trim() || !draft.description.trim()) {
        return false;
    }

    if (validation?.requireUserGroups && draft.groups.length === 0) {
        return false;
    }

    if (!isOAuthApplicationType(selectedType.id)) {
        return true;
    }

    if (draft.grantTypes.length === 0) {
        return false;
    }

    const hasMandatoryGrantTypes = selectedType.mandatory_grant_types.every(mandatory => draft.grantTypes.includes(mandatory.type));
    if (!hasMandatoryGrantTypes) {
        return false;
    }

    if (selectedType.requires_redirect_uris && draft.redirectUris.length === 0) {
        return false;
    }

    return true;
}
