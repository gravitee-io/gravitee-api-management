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
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { PageService } from '../../services/page.service';

import { GvPageAsyncApiComponent } from './gv-page-asyncapi.component';

jest.mock('@asyncapi/web-component/lib/asyncapi-web-component', () => ({}));

describe('GvPageAsyncApiComponent', () => {
  const createComponent = createComponentFactory({
    component: GvPageAsyncApiComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    detectChanges: false,
  });

  const renderedSchema = (content: string): string => {
    const spectator: Spectator<GvPageAsyncApiComponent> = createComponent({
      providers: [mockProvider(PageService, { getCurrentPage: () => ({ content }) })],
    });
    spectator.detectChanges();
    return spectator.query<HTMLElement & { schema: string }>('asyncapi-component').schema;
  };

  it('should render an Avro payload as written', () => {
    const content = `asyncapi: 3.0.0
channels:
  userSignedUp:
    messages:
      UserSignedUp:
        payload:
          schemaFormat: application/vnd.apache.avro;version=1.9.0
          schema: { type: record, name: UserSignedUp, fields: [{ name: userId, type: string }] }
`;

    expect(renderedSchema(content)).toEqual(content);
  });

  it('should render an XML Schema payload as its quoted schema', () => {
    const content = `asyncapi: 3.0.0
channels:
  orderCreated:
    messages:
      OrderCreated:
        payload:
          schemaFormat: application/xml
          schema: '<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>'
`;

    const payload = JSON.parse(renderedSchema(content)).channels.orderCreated.messages.OrderCreated.payload;

    expect(payload.schemaFormat).toBeUndefined();
    expect(payload.type).toEqual('string');
    expect(payload.description).toContain('<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>');
  });
});
