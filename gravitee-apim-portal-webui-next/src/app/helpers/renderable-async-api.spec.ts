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

const ASYNCAPI_3_WITH_AVRO_ON_MESSAGE = `asyncapi: 3.0.0
info:
  title: Users
  version: 1.0.0
channels:
  userSignedUp:
    address: user/signedup
    messages:
      UserSignedUp:
        schemaFormat: application/vnd.apache.avro;version=1.9.0
        payload:
          type: record
          name: UserSignedUp
          fields:
            - name: userId
              type: string
`;

const ASYNCAPI_3_WITH_XSD_ON_MESSAGE = `asyncapi: 3.0.0
info:
  title: Orders
  version: 1.0.0
channels:
  orderCreated:
    address: order/created
    messages:
      OrderCreated:
        schemaFormat: application/xml
        payload: |
${indent(XSD, 10)}
`;

const ASYNCAPI_3_XSD_WITH_MERGE_KEY = `asyncapi: 3.0.0
info:
  title: Orders
  version: 1.0.0
x-defaults:
  message: &message
    contentType: application/xml
components:
  messages:
    OrderCreated:
      <<: *message
      payload:
        schemaFormat: application/xml
        schema: <xs:schema/>
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

  it('should recognise an XML Schema format named only by xsd', () => {
    const content = ASYNCAPI_3_XSD.replace('schemaFormat: application/xml', 'schemaFormat: application/xsd');

    const payload = JSON.parse(toRenderableAsyncApi(content)).components.messages.OrderCreated.payload;

    expect(payload.type).toEqual('string');
  });

  it('should leave a format that only mentions xml in its name alone', () => {
    const content = ASYNCAPI_3_XSD.replace('schemaFormat: application/xml', 'schemaFormat: application/vnd.example.xml-catalog');

    expect(toRenderableAsyncApi(content)).toBe(content);
  });

  it('should resolve YAML merge keys in a rewritten document', () => {
    const message = JSON.parse(toRenderableAsyncApi(ASYNCAPI_3_XSD_WITH_MERGE_KEY)).components.messages.OrderCreated;

    expect(message.contentType).toEqual('application/xml');
    expect(message['<<']).toBeUndefined();
  });

  it('should lay a rewritten document out on one line per value, so the viewer errors keep pointing at distinct lines', () => {
    const rendered = toRenderableAsyncApi(ASYNCAPI_3_XSD);

    expect(rendered).toEqual(JSON.stringify(JSON.parse(rendered), null, 2));
  });

  it('should return a document whose aliases would expand past the size a page can lay out unchanged', () => {
    const levels = Array.from({ length: 18 }, (_, i) => `l${i + 1}: &l${i + 1} [*l${i}, *l${i}]`).join('\n');
    const content = `${ASYNCAPI_3_XSD}x-fan-out:\n  l0: &l0 { leaf: true }\n${levels.replace(/^/gm, '  ')}\n`;

    expect(toRenderableAsyncApi(content)).toBe(content);
  });

  it('should return a document whose aliases repeat a long text past the size a page can lay out unchanged', () => {
    const levels = Array.from({ length: 10 }, (_, i) => `  l${i + 1}: &l${i + 1} [*l${i}, *l${i}]`).join('\n');
    const content = `${ASYNCAPI_3_XSD}x-fan-out:\n  l0: &l0 ${'x'.repeat(10000)}\n${levels}\n`;

    expect(toRenderableAsyncApi(content)).toBe(content);
  });

  it('should return a document whose aliases repeat a long key past the size a page can lay out unchanged', () => {
    const levels = Array.from({ length: 10 }, (_, i) => `  l${i + 1}: &l${i + 1} [*l${i}, *l${i}]`).join('\n');
    const content = `${ASYNCAPI_3_XSD}x-fan-out:\n  l0: &l0 { ${'k'.repeat(10000)}: 1 }\n${levels}\n`;

    expect(toRenderableAsyncApi(content)).toBe(content);
  });

  it('should replace an XML Schema payload reused through an alias wherever it appears', () => {
    const content = ASYNCAPI_3_XSD.replace(
      '      payload:\n        schemaFormat: application/xml',
      '      payload: &orderPayload\n        schemaFormat: application/xml',
    ).concat('    OrderUpdated:\n      payload: *orderPayload\n');

    const messages = JSON.parse(toRenderableAsyncApi(content)).components.messages;

    expect(messages.OrderCreated.payload.type).toEqual('string');
    expect(messages.OrderUpdated.payload).toEqual(messages.OrderCreated.payload);
  });

  it('should return a document whose aliases nest deeper than the call stack unchanged', () => {
    const chain = Array.from({ length: 20000 }, (_, i) => `  l${i + 1}: &l${i + 1} [*l${i}]`).join('\n');
    const content = `${ASYNCAPI_3_XSD}x-chain:\n  l0: &l0 { leaf: true }\n${chain}\n`;

    expect(toRenderableAsyncApi(content)).toBe(content);
  });

  it('should return a document whose anchors loop back unchanged', () => {
    const content = `${ASYNCAPI_3_XSD}x-loop: &loop\n  self: *loop\n`;

    expect(toRenderableAsyncApi(content)).toBe(content);
  });

  it.each(['application/xml;version=1.0', 'text/xml'])('should recognise %s as an XML Schema format', schemaFormat => {
    const content = ASYNCAPI_3_XSD.replace('schemaFormat: application/xml', `schemaFormat: ${schemaFormat}`);

    const payload = JSON.parse(toRenderableAsyncApi(content)).components.messages.OrderCreated.payload;

    expect(payload.type).toEqual('string');
  });

  it.each([
    ['an Avro payload', ASYNCAPI_3_AVRO],
    ['a JSON Schema payload', ASYNCAPI_3_JSON_SCHEMA],
    ['content that is not YAML', 'asyncapi: 3.0.0\ninfo: [unclosed'],
    ['content that is not an AsyncAPI document', 'openapi: 3.0.0\npayload:\n  schemaFormat: application/xml\n  schema: <xs:schema/>\n'],
    ['an AsyncAPI 3 document declaring an Avro format on the message, as AsyncAPI 2 did', ASYNCAPI_3_WITH_AVRO_ON_MESSAGE],
    ['an AsyncAPI 3 document declaring an XML Schema format on the message, as AsyncAPI 2 did', ASYNCAPI_3_WITH_XSD_ON_MESSAGE],
    ['a stream of several YAML documents', `${ASYNCAPI_3_XSD}---\n${ASYNCAPI_3_XSD}`],
    ['empty content', ''],
    ['no content', undefined],
    ['null content', null],
  ])('should return %s unchanged', (_, content) => {
    expect(toRenderableAsyncApi(content)).toBe(content);
  });
});
