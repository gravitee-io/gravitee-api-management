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
import { ComponentHarness } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { waitFor } from '@testing-library/dom';

import { PageSwaggerComponent } from './page-swagger.component';
import { fakePage } from '../../../entities/page/page.fixtures';
import { AppTestingModule } from '../../../testing/app-testing.module';

// setup-jest mocks swagger-ui globally; this suite renders the real one, so that a swagger-ui or
// React upgrade that breaks rendering (e.g. swagger-ui resolved onto React 19) fails here.
jest.unmock('swagger-ui');

class SwaggerOperationPathHarness extends ComponentHarness {
  public static hostSelector = '.opblock-summary-path';

  public async getPath(): Promise<string> {
    return (await this.host()).text();
  }
}

const SPEC = JSON.stringify({
  openapi: '3.0.0',
  info: { title: 'Petstore', version: '1.0.0' },
  paths: {
    '/pets': {
      get: { summary: 'List pets', responses: { '200': { description: 'OK' } } },
    },
  },
});

describe('PageSwaggerComponent rendering', () => {
  let fixture: ComponentFixture<PageSwaggerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageSwaggerComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PageSwaggerComponent);
    fixture.componentRef.setInput('page', fakePage({ type: 'SWAGGER', content: SPEC }));
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should render the operations of the spec', async () => {
    const loader = TestbedHarnessEnvironment.loader(fixture);

    await waitFor(async () => {
      const operationPaths = await loader.getAllHarnesses(SwaggerOperationPathHarness);
      expect(await Promise.all(operationPaths.map(operationPath => operationPath.getPath()))).toEqual(['/pets']);
    });
  });
});
