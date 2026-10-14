/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { toRenderableAsyncApi } from './renderable-async-api';

const XSD = `<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="orderId" type="xs:string"/>
</xs:schema>`;

const indent = (text: string, spaces: number) =>
  text
    .split('\n')
    .map(line => ' '.repeat(spaces) + line)
    .join('\n');

const ASYNCAPI_3_XSD = `asyncapi: 3.0.0
info:
  title: Orders
  version: 1.0.0
channels:
  orderCreated:
    address: order/created
    messages:
      OrderCreated:
        $ref: '#/components/messages/OrderCreated'
components:
  messages:
    OrderCreated:
      contentType: application/xml
      payload:
        schemaFormat: application/xml
        schema: |
${indent(XSD, 10)}
`;

const ASYNCAPI_2_XSD = `asyncapi: 2.6.0
info:
  title: Orders
  version: 1.0.0
channels:
  order/created:
    subscribe:
      message:
        contentType: application/xml
        schemaFormat: application/xsd+xml
        payload: |
${indent(XSD, 10)}
`;

const ASYNCAPI_3_AVRO = `asyncapi: 3.0.0
info:
  title: Users
  version: 1.0.0
channels:
  userSignedUp:
    address: user/signedup
    messages:
      UserSignedUp:
        payload:
          schemaFormat: application/vnd.apache.avro;version=1.9.0
          schema:
            type: record
            name: UserSignedUp
            fields:
              - name: userId
                type: string
`;

const ASYNCAPI_3_JSON_SCHEMA = `asyncapi: 3.0.0
info:
  title: Users
  version: 1.0.0
channels:
  userSignedUp:
    address: user/signedup
    messages:
      UserSignedUp:
        payload:
          type: object
          properties:
            userId:
              type: string
`;

describe('toRenderableAsyncApi', () => {
  it('should replace an AsyncAPI 3 XML Schema payload with a string schema quoting the XSD', () => {
    const rendered = JSON.parse(toRenderableAsyncApi(ASYNCAPI_3_XSD));

    const payload = rendered.components.messages.OrderCreated.payload;
    expect(payload.schemaFormat).toBeUndefined();
    expect(payload.schema).toBeUndefined();
    expect(payload.type).toEqual('string');
    expect(payload.description).toContain('XML Schema (`application/xml`)');
    expect(payload.description).toContain('```xml\n' + XSD + '\n```');
  });

  it('should keep the rest of the document when replacing an XML Schema payload', () => {
    const rendered = JSON.parse(toRenderableAsyncApi(ASYNCAPI_3_XSD));

    expect(rendered.info).toEqual({ title: 'Orders', version: '1.0.0' });
    expect(rendered.channels.orderCreated).toEqual({
      address: 'order/created',
      messages: { OrderCreated: { $ref: '#/components/messages/OrderCreated' } },
    });
    expect(rendered.components.messages.OrderCreated.contentType).toEqual('application/xml');
  });

  it('should replace an AsyncAPI 2 XML Schema payload and drop the message schema format', () => {
    const rendered = JSON.parse(toRenderableAsyncApi(ASYNCAPI_2_XSD));

    const message = rendered.channels['order/created'].subscribe.message;
    expect(message.schemaFormat).toBeUndefined();
    expect(message.payload.type).toEqual('string');
    expect(message.payload.description).toContain('XML Schema (`application/xsd+xml`)');
    expect(message.payload.description).toContain(XSD);
  });

  it('should quote a schema that is not text as JSON', () => {
    const content = ASYNCAPI_3_XSD.replace(/schema: \|[\s\S]*$/, "schema:\n          $ref: 'https://example.com/order.xsd'\n");

    const payload = JSON.parse(toRenderableAsyncApi(content)).components.messages.OrderCreated.payload;

    expect(payload.$ref).toBeUndefined();
    expect(payload.description).toContain('"$ref": "https://example.com/order.xsd"');
  });

  it('should use a code fence longer than any backtick run in the schema', () => {
    const content = ASYNCAPI_3_XSD.replace('<xs:element', '<!-- ``` --><xs:element');

    const payload = JSON.parse(toRenderableAsyncApi(content)).components.messages.OrderCreated.payload;

    expect(payload.description).toContain('````xml\n');
    expect(payload.description).toMatch(/\n````$/);
  });

  it.each([
    ['an Avro payload', ASYNCAPI_3_AVRO],
    ['a JSON Schema payload', ASYNCAPI_3_JSON_SCHEMA],
    ['content that is not YAML', 'asyncapi: 3.0.0\ninfo: [unclosed'],
    ['content that is not an AsyncAPI document', 'openapi: 3.0.0\npayload:\n  schemaFormat: application/xml\n  schema: <xs:schema/>\n'],
    ['empty content', ''],
  ])('should return %s unchanged', (_, content) => {
    expect(toRenderableAsyncApi(content)).toBe(content);
  });
});
