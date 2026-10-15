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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageAsyncApiComponent } from './page-async-api.component';
import { PageAsyncApiHarness } from './page-async-api.harness';

describe('PageAsyncApiComponent', () => {
  let component: PageAsyncApiComponent;
  let fixture: ComponentFixture<PageAsyncApiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageAsyncApiComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PageAsyncApiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  const renderedSchema = async (content: string | undefined): Promise<string> => {
    fixture.componentRef.setInput('content', content);
    const harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PageAsyncApiHarness);
    return harness.getSchema();
  };

  it('should render an Avro payload as written', async () => {
    const content = `asyncapi: 3.0.0
channels:
  userSignedUp:
    messages:
      UserSignedUp:
        payload:
          schemaFormat: application/vnd.apache.avro;version=1.9.0
          schema: { type: record, name: UserSignedUp, fields: [{ name: userId, type: string }] }
`;

    expect(await renderedSchema(content)).toEqual(content);
  });

  it('should render an XML Schema payload as its quoted schema', async () => {
    const content = `asyncapi: 3.0.0
channels:
  orderCreated:
    messages:
      OrderCreated:
        payload:
          schemaFormat: application/xml
          schema: '<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>'
`;

    const payload = JSON.parse(await renderedSchema(content)).channels.orderCreated.messages.OrderCreated.payload;

    expect(payload.schemaFormat).toBeUndefined();
    expect(payload.type).toEqual('string');
    expect(payload.description).toContain('<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"/>');
  });

  it('should render missing content as empty', async () => {
    expect(await renderedSchema(undefined)).toEqual('');
  });
});
