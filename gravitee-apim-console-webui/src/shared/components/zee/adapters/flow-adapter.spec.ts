/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

import { FLOW_ADAPTER } from './flow-adapter';

describe('FLOW_ADAPTER', () => {
  it('should have the correct preview label', () => {
    expect(FLOW_ADAPTER.previewLabel).toEqual('Generated Flow');
  });

  describe('transform()', () => {
    it('should correctly map base properties and set default values', () => {
      const generated = {
        name: 'My Generated Flow',
      };

      const result = FLOW_ADAPTER.transform(generated);

      expect(result).toEqual({
        name: 'My Generated Flow',
        enabled: true,
        selectors: [],
        request: [],
        response: [],
        subscribe: [],
        publish: [],
        tags: [],
      });
    });

    it('should map HTTP selector correctly', () => {
      const generated = {
        name: 'Flow',
        selectors: [
          {
            type: 'http',
            path: '/api/v1',
            pathOperator: 'STARTS_WITH',
            methods: ['GET', 'POST'],
            ignored: 'this should be ignored',
          },
        ],
      };

      const result = FLOW_ADAPTER.transform(generated);

      expect(result.selectors).toEqual([
        {
          type: 'HTTP',
          path: '/api/v1',
          pathOperator: 'STARTS_WITH',
          methods: ['GET', 'POST'],
        },
      ]);
    });

    it('should map CHANNEL selector correctly', () => {
      const generated = {
        name: 'Flow',
        selectors: [
          {
            type: 'channel',
            channel: 'my-channel',
            channelOperator: 'EQUALS',
            operations: ['PUBLISH', 'SUBSCRIBE'],
            entrypoints: ['webhook'],
          },
        ],
      };

      const result = FLOW_ADAPTER.transform(generated);

      expect(result.selectors).toEqual([
        {
          type: 'CHANNEL',
          channel: 'my-channel',
          channelOperator: 'EQUALS',
          operations: ['PUBLISH', 'SUBSCRIBE'],
          entrypoints: ['webhook'],
        },
      ]);
    });

    it('should map CONDITION selector correctly', () => {
      const generated = {
        name: 'Flow',
        selectors: [
          {
            type: 'condition',
            condition: "{#request.headers['X-My-Header'] != null}",
          },
        ],
      };

      const result = FLOW_ADAPTER.transform(generated);

      expect(result.selectors).toEqual([
        {
          type: 'CONDITION',
          condition: "{#request.headers['X-My-Header'] != null}",
        },
      ]);
    });

    it('should keep other selector types as is based on default switch', () => {
      const generated = {
        name: 'Flow',
        selectors: [
          {
            type: 'UNKNOWN',
            customValue: 123,
          },
        ],
      };

      const result = FLOW_ADAPTER.transform(generated);

      expect(result.selectors).toEqual([
        {
          type: 'UNKNOWN',
        },
      ]);
    });

    it('should parse stringified JSON configuration correctly for steps', () => {
      const generated = {
        name: 'Flow',
        request: [
          {
            name: 'Assign Content',
            policy: 'assign-content',
            enabled: false,
            description: 'Sets a response body',
            condition: "{#request.path == '/api'}",
            configuration: '{"body": "Hello World", "status": 200}',
          },
        ],
      };

      const result = FLOW_ADAPTER.transform(generated);

      expect(result.request).toEqual([
        {
          name: 'Assign Content',
          policy: 'assign-content',
          enabled: false,
          description: 'Sets a response body',
          condition: "{#request.path == '/api'}",
          configuration: {
            body: 'Hello World',
            status: 200,
          },
          messageCondition: undefined,
        },
      ]);
    });

    it('should handle already parsed JSON configuration for steps', () => {
        const generated = {
          name: 'Flow',
          request: [
            {
              name: 'Assign Content',
              policy: 'assign-content',
              condition: "{#request.path == '/api'}",
              configuration: { body: "Hello World", status: 200 },
            },
          ],
        };
  
        const result = FLOW_ADAPTER.transform(generated);
  
        expect(result.request).toEqual([
          {
            name: 'Assign Content',
            policy: 'assign-content',
            enabled: true,
            description: undefined,
            condition: "{#request.path == '/api'}",
            configuration: {
              body: 'Hello World',
              status: 200,
            },
            messageCondition: undefined,
          },
        ]);
      });

    it('should return empty object for invalid JSON configuration', () => {
      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();

      const generated = {
        name: 'Flow',
        response: [
          {
            name: 'Broken Step',
            policy: 'broken-policy',
            configuration: '{broken json}',
          },
        ],
      };

      const result = FLOW_ADAPTER.transform(generated);

      expect(result.response).toEqual([
        {
          name: 'Broken Step',
          policy: 'broken-policy',
          enabled: true,
          description: undefined,
          condition: undefined,
          configuration: {},
          messageCondition: undefined,
        },
      ]);

      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });
});
