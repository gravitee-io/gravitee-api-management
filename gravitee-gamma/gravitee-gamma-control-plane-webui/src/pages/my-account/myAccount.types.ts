/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import type { CurrentUser } from '../../features/auth/auth.types';

export interface ConsoleAuthenticationSettings {
    readonly externalAuth?: { readonly enabled?: boolean };
    readonly externalAuthAccountDeletion?: { readonly enabled?: boolean };
}

export interface UpdateCurrentUserPayload {
    readonly firstname: string;
    readonly lastname: string;
    readonly email?: string;
    readonly picture?: string;
    readonly customFields?: Readonly<Record<string, string>>;
}

export interface ProfileDraft {
    readonly firstname: string;
    readonly lastname: string;
    readonly email: string;
    readonly customFields: Readonly<Record<string, string>>;
    /** New upload as a data URL. Absent when the picture is unchanged or reset. */
    readonly pictureDataUrl: string | null;
    /** Classic "Use default": send an empty picture string so the backend clears it. */
    readonly resetToDefault: boolean;
    readonly hadPictureOnLoad: boolean;
}

export interface PersonalAccessToken {
    readonly id: string;
    readonly name: string;
    readonly token?: string;
    readonly created_at?: number;
    readonly last_use_at?: number;
    readonly expires_at?: number;
}

export interface NamedEnvironment {
    readonly id: string;
    readonly name?: string;
}

export type AccountUser = CurrentUser;
