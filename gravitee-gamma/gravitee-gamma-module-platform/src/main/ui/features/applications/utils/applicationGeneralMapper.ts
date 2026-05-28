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
import type { UpdateApplicationPayload } from '../services/applicationDetail';
import type { ApplicationListItem } from '../types/application';
import type { ApplicationTypeConfig } from '../types/applicationCreate';

export interface ApplicationGeneralForm {
    name: string;
    description: string;
    domain: string;
    picture: string | null;
    background: string | null;
    pictureRemoved: boolean;
    backgroundRemoved: boolean;
    simpleClientId: string;
    oauthClientId: string;
    oauthClientSecret: string;
    grantTypes: string[];
    redirectUrisText: string;
}

export interface ApplicationGeneralValidation {
    name?: string;
    description?: string;
}

export function formFromApplication(application: ApplicationListItem): ApplicationGeneralForm {
    const isSimple = application.type === 'SIMPLE';
    return {
        name: application.name ?? '',
        description: application.description ?? '',
        domain: application.domain ?? '',
        picture: application.picture ?? application.picture_url ?? null,
        background: application.background ?? null,
        pictureRemoved: false,
        backgroundRemoved: false,
        simpleClientId: isSimple ? (application.settings?.app?.client_id ?? '') : '',
        oauthClientId: !isSimple ? (application.settings?.oauth?.client_id ?? '') : '',
        oauthClientSecret: !isSimple ? (application.settings?.oauth?.client_secret ?? '') : '',
        grantTypes: !isSimple ? [...(application.settings?.oauth?.grant_types ?? [])] : [],
        redirectUrisText: !isSimple ? (application.settings?.oauth?.redirect_uris ?? []).join('\n') : '',
    };
}

export function validateApplicationGeneralForm(form: ApplicationGeneralForm): ApplicationGeneralValidation {
    const errors: ApplicationGeneralValidation = {};
    if (!form.name.trim()) {
        errors.name = 'Application name is required.';
    } else if (form.name.length > 512) {
        errors.name = 'Application name must be 512 characters or fewer.';
    }
    if (!form.description.trim()) {
        errors.description = 'Application description is required.';
    }
    return errors;
}

export function hasApplicationGeneralValidationErrors(errors: ApplicationGeneralValidation): boolean {
    return Boolean(errors.name || errors.description);
}

function grantTypesEqual(a: string[], b: string[]): boolean {
    if (a.length !== b.length) {
        return false;
    }
    const sortedA = [...a].sort();
    const sortedB = [...b].sort();
    return sortedA.every((value, index) => value === sortedB[index]);
}

/** Returns true when local draft differs from the last saved snapshot. */
export function isApplicationGeneralFormDirty(current: ApplicationGeneralForm, saved: ApplicationGeneralForm): boolean {
    return (
        current.name !== saved.name ||
        current.description !== saved.description ||
        current.domain !== saved.domain ||
        current.picture !== saved.picture ||
        current.background !== saved.background ||
        current.pictureRemoved !== saved.pictureRemoved ||
        current.backgroundRemoved !== saved.backgroundRemoved ||
        current.simpleClientId !== saved.simpleClientId ||
        current.oauthClientId !== saved.oauthClientId ||
        current.oauthClientSecret !== saved.oauthClientSecret ||
        current.redirectUrisText !== saved.redirectUrisText ||
        !grantTypesEqual(current.grantTypes, saved.grantTypes)
    );
}

function parseRedirectUris(text: string): string[] {
    return text
        .split('\n')
        .map(line => line.trim())
        .filter(Boolean);
}

/** Console always sends both images on update; omitting one clears it server-side. */
function resolveFormPicture(application: ApplicationListItem, form: ApplicationGeneralForm): string | null {
    if (form.pictureRemoved) {
        return null;
    }
    if (form.picture) {
        return form.picture;
    }
    return application.picture ?? application.picture_url ?? null;
}

function resolveFormBackground(application: ApplicationListItem, form: ApplicationGeneralForm): string | null {
    if (form.backgroundRemoved) {
        return null;
    }
    if (form.background) {
        return form.background;
    }
    return application.background ?? null;
}

export function buildUpdatePayload(
    application: ApplicationListItem,
    form: ApplicationGeneralForm,
    applicationTypeConfig: ApplicationTypeConfig | undefined,
): UpdateApplicationPayload {
    const isSimple = application.type === 'SIMPLE';

    const settings = isSimple
        ? {
              app: {
                  ...application.settings?.app,
                  client_id: form.simpleClientId || undefined,
              },
          }
        : {
              oauth: {
                  ...application.settings?.oauth,
                  client_id: form.oauthClientId || application.settings?.oauth?.client_id,
                  client_secret: form.oauthClientSecret || application.settings?.oauth?.client_secret,
                  grant_types: form.grantTypes,
                  redirect_uris: parseRedirectUris(form.redirectUrisText),
                  application_type: applicationTypeConfig?.id ?? application.settings?.oauth?.application_type,
              },
          };

    return {
        name: form.name.trim(),
        description: form.description.trim(),
        domain: form.domain.trim() || undefined,
        settings,
        picture: resolveFormPicture(application, form),
        background: resolveFormBackground(application, form),
    };
}

export function isApplicationGeneralReadOnly(application: ApplicationListItem): boolean {
    return application.status === 'ARCHIVED' || application.origin === 'KUBERNETES';
}
