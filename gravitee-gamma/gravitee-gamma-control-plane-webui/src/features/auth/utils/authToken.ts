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
import { jwtDecode, type JwtPayload } from 'jwt-decode';

export interface AuthTokenClaims extends JwtPayload {
    email?: string;
    firstname?: string;
    lastname?: string;
}

export function parseAuthToken(token: string): AuthTokenClaims | null {
    try {
        return jwtDecode<AuthTokenClaims>(token);
    } catch {
        return null;
    }
}

export function isAuthTokenExpired(claims: AuthTokenClaims): boolean {
    return typeof claims.exp === 'number' && claims.exp * 1000 < Date.now();
}
