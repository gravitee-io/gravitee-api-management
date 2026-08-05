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

export const gatewayInstanceKeys = {
    all: ['gateway-instances'] as const,
    list: (envId: string, page: number, size: number) => [...gatewayInstanceKeys.all, 'list', envId, page, size] as const,
    detail: (envId: string, instanceId: string) => [...gatewayInstanceKeys.all, 'detail', envId, instanceId] as const,
    monitoring: (envId: string, instanceId: string, gatewayId: string) =>
        [...gatewayInstanceKeys.all, 'monitoring', envId, instanceId, gatewayId] as const,
} as const;
