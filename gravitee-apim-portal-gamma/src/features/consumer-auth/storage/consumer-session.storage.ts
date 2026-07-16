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
import type { PortalConsumerSession } from '../types/consumer-auth.types';

const SESSION_KEY_PREFIX = 'portal-gamma-consumer-session:';

function sessionKey(portalId: string): string {
    return `${SESSION_KEY_PREFIX}${portalId}`;
}

export function readConsumerSession(portalId: string): PortalConsumerSession | null {
    try {
        const raw = sessionStorage.getItem(sessionKey(portalId));
        if (!raw) {
            return null;
        }

        return JSON.parse(raw) as PortalConsumerSession;
    } catch {
        return null;
    }
}

export function writeConsumerSession(session: PortalConsumerSession): void {
    sessionStorage.setItem(sessionKey(session.portalId), JSON.stringify(session));
}

export function clearConsumerSession(portalId: string): void {
    sessionStorage.removeItem(sessionKey(portalId));
}
