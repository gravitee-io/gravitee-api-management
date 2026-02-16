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
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { dereference, validate } from '@scalar/openapi-parser';

import { OpenApiToMcpToolsComponent } from './open-api-to-mcp-tools.component';
import { OpenApiToMcpToolsHarness } from './open-api-to-mcp-tools.harness';

import { GioTestingModule } from '../../../../../shared/testing';

// Cast the imported functions as jest.Mock to access mock methods
const mockValidate = validate as jest.Mock;
const mockDereference = dereference as jest.Mock;

@Component({
  template: `
    <form [formGroup]="form">
      <open-api-to-mcp-tools formControlName="openApiTools" />
    </form>
  `,
  imports: [OpenApiToMcpToolsComponent, ReactiveFormsModule],
})
class TestComponent {
  form: FormGroup;

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      openApiTools: [null],
    });
  }
}

describe('OpenApiToMcpToolsComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let harnessLoader: HarnessLoader;

  beforeEach(() => {
    // Reset all mocks before each test
    jest.clearAllMocks();

    // Set up default mock implementations
    mockValidate.mockResolvedValue(true);
    mockDereference.mockImplementation(async spec => ({ schema: spec }));
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  });

  it('should transform specification to tools', async () => {
    const jsonSpec = JSON.stringify({
      openapi: '3.0.0',
      info: { title: 'Sample API', version: '1.0.0' },
      paths: {
        '/user/{id}': {
          get: {
            operationId: 'getUser',
            summary: 'Get user by ID',
            parameters: [
              {
                name: 'id',
                in: 'path',
                required: true,
                schema: { type: 'string' },
              },
              {
                name: 'verbose',
                in: 'query',
                schema: { type: 'boolean' },
              },
              {
                name: 'X-Custom-Header',
                in: 'header',
                schema: { type: 'string' },
                description: 'A custom header for the request',
              },
            ],
            responses: {
              '200': {
                description: 'Successful response',
                content: {
                  'application/json': {
                    schema: {
                      type: 'object',
                      properties: {
                        id: { type: 'string' },
                        username: { type: 'string' },
                        email: { type: 'string' },
                      },
                    },
                  },
                },
              },
            },
          },
        },
        '/user': {
          post: {
            summary: 'Create a new user',
            requestBody: {
              content: {
                'application/json': {
                  schema: {
                    type: 'object',
                    properties: {
                      username: { type: 'string' },
                      email: { type: 'string' },
                    },
                    required: ['username', 'email'],
                  },
                },
              },
            },
            responses: {
              '201': {
                description: 'User created successfully',
              },
            },
          },
        },
      },
    });

    const openApiToMcpToolsHarness = await harnessLoader.getHarness(OpenApiToMcpToolsHarness);
    await openApiToMcpToolsHarness.setValue(jsonSpec);

    expect(await openApiToMcpToolsHarness.getErrors()).toHaveLength(0);
    expect(await openApiToMcpToolsHarness.getToolCount()).toEqual(2);
  });
  it('should show errors', async () => {
    const errorSpec = `
openapi: 3.0.0
  info:
    title: Sample API
    version
  paths:`;
    const openApiToMcpToolsHarness = await harnessLoader.getHarness(OpenApiToMcpToolsHarness);
    await openApiToMcpToolsHarness.setValue(errorSpec);

    fixture.detectChanges();

    expect(await openApiToMcpToolsHarness.getToolCount()).toEqual(0);

    const errors = await openApiToMcpToolsHarness.getErrors();
    expect(errors.length).toEqual(1);
    expect(errors[0]).toContain('Failed to parse specification');
  });

  it('should show empty message when no tools are generated', async () => {
    const openApiToMcpToolsHarness = await harnessLoader.getHarness(OpenApiToMcpToolsHarness);
    expect(await openApiToMcpToolsHarness.getToolCount()).toEqual(0);
  });
});
