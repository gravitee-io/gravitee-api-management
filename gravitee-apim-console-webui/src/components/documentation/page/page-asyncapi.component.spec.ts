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
import { PageAsyncApiComponent } from './page-asyncapi.component';

describe('PageAsyncApiComponent', () => {
  const ControllerClass = PageAsyncApiComponent.controller as new () => { pageContent: string; schema: string; $onChanges(): void };

  it('should render an XML Schema payload as its quoted schema', () => {
    const controller = new ControllerClass();
    controller.pageContent =
      'asyncapi: 3.0.0\ncomponents:\n  messages:\n    OrderCreated:\n      payload:\n        schemaFormat: application/xml\n        schema: <xs:schema/>\n';

    controller.$onChanges();

    const payload = JSON.parse(controller.schema).components.messages.OrderCreated.payload;
    expect(payload.type).toEqual('string');
    expect(payload.description).toContain('<xs:schema/>');
  });

  it('should render any other content as written', () => {
    const controller = new ControllerClass();
    controller.pageContent = 'asyncapi: 3.0.0\ninfo:\n  title: Users\n';

    controller.$onChanges();

    expect(controller.schema).toEqual(controller.pageContent);
  });
});
