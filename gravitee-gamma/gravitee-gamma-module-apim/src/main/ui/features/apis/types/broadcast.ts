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

export type BroadcastChannel = 'PORTAL' | 'MAIL' | 'HTTP';

export interface BroadcastTextPayload {
    channel: 'PORTAL' | 'MAIL';
    title: string;
    text: string;
    recipient: {
        role_scope: 'APPLICATION';
        role_value: string[];
    };
}

export interface BroadcastHttpPayload {
    channel: 'HTTP';
    text: string;
    recipient: { url: string };
    params: Record<string, string>;
    useSystemProxy: boolean;
}

export type BroadcastPayload = BroadcastTextPayload | BroadcastHttpPayload;

export interface ApplicationRole {
    name: string;
    description?: string;
}

export interface RecipientOption {
    name: string;
    displayName: string;
}
