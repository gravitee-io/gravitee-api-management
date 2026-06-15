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
import type { ApiType } from '../../apis/types/api';

/**
 * An AI Product IS an API Product (same backend resource) presented through the
 * AI packaging lens: its attached APIs are "components" typed by their apiType.
 */
export type { ApiProductListItem as AiProduct } from '../../api-products/types/apiProduct';

export interface ComponentTypeInfo {
    label: string;
    /** Badge tone — maps to existing Badge variants. */
    variant: 'default' | 'secondary' | 'outline';
}

const COMPONENT_TYPE_INFO: Partial<Record<ApiType, ComponentTypeInfo>> = {
    LLM_PROXY: { label: 'LLM Proxy', variant: 'default' },
    MCP_PROXY: { label: 'MCP Proxy', variant: 'secondary' },
    A2A_PROXY: { label: 'A2A Proxy', variant: 'secondary' },
    PROXY: { label: 'API Proxy', variant: 'outline' },
};

export function componentTypeInfo(type: ApiType | undefined): ComponentTypeInfo {
    return (type && COMPONENT_TYPE_INFO[type]) || { label: 'API', variant: 'outline' };
}

/** Model entry from the llm-proxy endpoint connector configuration. */
export interface LlmModel {
    name: string;
    aliases?: string[];
    inputPrice?: number;
    outputPrice?: number;
}
