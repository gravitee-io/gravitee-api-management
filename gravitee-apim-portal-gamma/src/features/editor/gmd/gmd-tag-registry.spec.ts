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
import { buildGmdTag, GMD_BLOCK_TYPE_TO_TAG, GMD_TAG_NAME_TO_BLOCK_TYPE } from './gmd-tag-registry';

describe('gmd-tag-registry', () => {
    it('should map known gmd tags to block types', () => {
        expect(GMD_TAG_NAME_TO_BLOCK_TYPE['gmd-button']).toBe('graviteeButton');
        expect(GMD_TAG_NAME_TO_BLOCK_TYPE['gmd-install-mcp']).toBe('graviteeInstallMcp');
        expect(GMD_TAG_NAME_TO_BLOCK_TYPE['gmd-api-catalog']).toBe('graviteeApiCatalog');
    });

    it('should serialize button tags with inner label', () => {
        const mapping = GMD_BLOCK_TYPE_TO_TAG.graviteeButton;
        const tag = buildGmdTag(mapping, {
            label: 'Explore APIs',
            link: '/catalog',
            appearance: 'filled',
        });

        expect(tag).toBe('<gmd-button link="/catalog" appearance="filled">Explore APIs</gmd-button>');
    });

    it('should serialize install mcp as self-closing tag', () => {
        const mapping = GMD_BLOCK_TYPE_TO_TAG.graviteeInstallMcp;
        const tag = buildGmdTag(mapping, {
            name: 'My Server',
            transport: 'http',
            url: 'https://example.com/mcp',
        });

        expect(tag).toContain('<gmd-install-mcp');
        expect(tag).toContain('name="My Server"');
        expect(tag).toContain('url="https://example.com/mcp"');
    });
});
