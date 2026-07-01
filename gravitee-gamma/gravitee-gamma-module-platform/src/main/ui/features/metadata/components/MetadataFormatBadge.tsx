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

import { Badge, cn } from '@gravitee/graphene-core';

import type { MetadataFormat } from '../types/metadata';

const FORMAT_CONFIG: Record<MetadataFormat, { label: string; className: string }> = {
    STRING: { label: 'String', className: 'bg-blue-100 text-blue-700 border-transparent' },
    NUMERIC: { label: 'Numeric', className: 'bg-purple-100 text-purple-700 border-transparent' },
    BOOLEAN: { label: 'Boolean', className: 'bg-amber-100 text-amber-700 border-transparent' },
    DATE: { label: 'Date', className: 'bg-green-100 text-green-700 border-transparent' },
    MAIL: { label: 'Mail', className: 'bg-pink-100 text-pink-700 border-transparent' },
    URL: { label: 'URL', className: 'bg-cyan-100 text-cyan-700 border-transparent' },
};

export function MetadataFormatBadge({ format }: Readonly<{ format: MetadataFormat }>) {
    const config = FORMAT_CONFIG[format] ?? { label: format, className: '' };
    return <Badge className={cn('font-normal text-xs', config.className)}>{config.label}</Badge>;
}
