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
import { inlineXsdPayloads } from './asyncapi-xsd';

const XSD = '<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">\n  <xs:element name="order" type="xs:string"/>\n</xs:schema>';

const ASYNCAPI_3 = `
asyncapi: 3.0.0
info: { title: Orders, version: 1.0.0 }
channels:
  orders:
    address: orders
    messages:
      OrderCreated:
        payload:
          schemaFormat: application/xml
          schema: |
            ${XSD.replace(/\n/g, '\n            ')}
      OrderCancelled:
        payload:
          type: object
          properties:
            id: { type: string }
`;

describe('inlineXsdPayloads', () => {
  it('should replace an AsyncAPI 3 XSD payload by a string schema quoting the XSD', () => {
    const result = JSON.parse(inlineXsdPayloads(ASYNCAPI_3));

    expect(result.channels.orders.messages.OrderCreated.payload).toEqual({
      type: 'string',
      description: `XML Schema (XSD):\n\n\`\`\`xml\n${XSD}\n\`\`\``,
    });
  });

  it('should keep the other payloads and the rest of the document', () => {
    const result = JSON.parse(inlineXsdPayloads(ASYNCAPI_3));

    expect(result.info).toEqual({ title: 'Orders', version: '1.0.0' });
    expect(result.channels.orders.messages.OrderCancelled.payload).toEqual({ type: 'object', properties: { id: { type: 'string' } } });
  });

  it('should replace an AsyncAPI 2 XSD payload and drop the message schemaFormat', () => {
    const content = JSON.stringify({
      asyncapi: '2.6.0',
      channels: {
        orders: { subscribe: { message: { schemaFormat: 'application/xml; version=1.0', payload: XSD, name: 'OrderCreated' } } },
      },
    });

    const message = JSON.parse(inlineXsdPayloads(content)).channels.orders.subscribe.message;

    expect(message.schemaFormat).toBeUndefined();
    expect(message.name).toBe('OrderCreated');
    expect(message.payload.type).toBe('string');
    expect(message.payload.description).toContain(XSD);
  });

  it('should use a longer fence when the XSD contains a code fence', () => {
    const xsd = '<xs:schema><!-- ``` --></xs:schema>';
    const content = JSON.stringify({ payload: { schemaFormat: 'text/xml', schema: xsd } });

    expect(JSON.parse(inlineXsdPayloads(content)).payload.description).toBe(`XML Schema (XSD):\n\n\`\`\`\`xml\n${xsd}\n\`\`\`\``);
  });

  it('should return the content untouched when it has no XSD payload', () => {
    const content = 'asyncapi: 3.0.0\ninfo: { title: XML feed, version: 1.0.0 }\n';

    expect(inlineXsdPayloads(content)).toBe(content);
  });

  it('should leave other schema formats alone', () => {
    const content = JSON.stringify({
      payload: { schemaFormat: 'application/vnd.aai.asyncapi+json;version=3.0.0', schema: { type: 'string' } },
    });

    expect(inlineXsdPayloads(content)).toBe(content);
  });

  it('should return content that is not a parsable document untouched', () => {
    const content = 'schemaFormat: application/xml\n  : [broken';

    expect(inlineXsdPayloads(content)).toBe(content);
  });

  it('should return empty content untouched', () => {
    expect(inlineXsdPayloads('')).toBe('');
    expect(inlineXsdPayloads(undefined as unknown as string)).toBeUndefined();
  });
});
