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
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GioAsyncApiComponent } from './gio-async-api.component';
import { GioAsyncApiModule } from './gio-async-api-module';

describe('GioAsyncApiComponent', () => {
  let fixture: ComponentFixture<GioAsyncApiComponent>;

  const renderedSchema = (schema: string): string => {
    fixture.componentInstance.schema = schema;
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('asyncapi-component').schema;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [GioAsyncApiModule] }).compileComponents();
    fixture = TestBed.createComponent(GioAsyncApiComponent);
  });

  it('should render an XML Schema payload as its quoted schema', () => {
    const schema =
      'asyncapi: 3.0.0\ncomponents:\n  schemas:\n    Order:\n      schemaFormat: application/xml\n      schema: <xs:schema/>\n';

    const order = JSON.parse(renderedSchema(schema)).components.schemas.Order;

    expect(order.type).toEqual('string');
    expect(order.description).toContain('<xs:schema/>');
  });

  it('should render any other schema as written', () => {
    const schema = 'asyncapi: 3.0.0\ninfo:\n  title: Users\n';

    expect(renderedSchema(schema)).toEqual(schema);
  });
});
