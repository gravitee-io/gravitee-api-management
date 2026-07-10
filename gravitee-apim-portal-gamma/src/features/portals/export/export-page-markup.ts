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
import type { BlockPageContent } from '../types';

interface BlockNode {
    type?: string;
    props?: Record<string, unknown>;
}

function escapeAttr(value: string): string {
    return value.replace(/"/g, '&quot;');
}

function blockToMarkup(block: BlockNode): string {
    const type = block.type ?? '';
    const props = block.props ?? {};

    if (type === 'graviteeCard') {
        const attrs = [
            `title="${escapeAttr(String(props.title ?? ''))}"`,
            props.instanceStyle && props.instanceStyle !== '{}'
                ? `instance-style="${escapeAttr(String(props.instanceStyle))}"`
                : null,
        ].filter(Boolean).join(' ');
        return `<gravitee-card ${attrs}></gravitee-card>`;
    }

    if (type === 'graviteeHtml') {
        const html = String(props.html ?? '');
        const css = String(props.css ?? '');
        return `<gravitee-html>
  <content><![CDATA[${html}]]></content>
  <style><![CDATA[${css}]]></style>
</gravitee-html>`;
    }

    if (type === 'graviteeApiCatalog') {
        return '<gravitee-component type="api-catalog"></gravitee-component>';
    }

    if (type === 'heading') {
        const level = props.level ?? 1;
        return `<gravitee-heading level="${level}"></gravitee-heading>`;
    }

    if (type === 'paragraph') {
        return '<gravitee-paragraph></gravitee-paragraph>';
    }

    return `<!-- unsupported block: ${type} -->`;
}

function isBlockNode(value: unknown): value is BlockNode {
    return typeof value === 'object' && value !== null;
}

export function exportPageToMarkup(content: BlockPageContent): string {
    return (content.document ?? [])
        .filter(isBlockNode)
        .map(blockToMarkup)
        .join('\n\n');
}
