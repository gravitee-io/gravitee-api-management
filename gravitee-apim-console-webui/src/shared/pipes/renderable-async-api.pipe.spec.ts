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
import { RenderableAsyncApiPipe } from './renderable-async-api.pipe';

describe('RenderableAsyncApiPipe', () => {
  const pipe = new RenderableAsyncApiPipe();

  it('should quote an XML Schema payload', () => {
    const content =
      'asyncapi: 2.6.0\nchannels:\n  orders:\n    subscribe:\n      message:\n        schemaFormat: text/xml\n        payload: <xs:schema/>\n';

    const message = JSON.parse(pipe.transform(content)).channels.orders.subscribe.message;

    expect(message.schemaFormat).toBeUndefined();
    expect(message.payload.description).toContain('```xml\n<xs:schema/>\n```');
  });

  it.each([undefined, null, ''])('should return %p unchanged', content => {
    expect(pipe.transform(content)).toBe(content);
  });
});
