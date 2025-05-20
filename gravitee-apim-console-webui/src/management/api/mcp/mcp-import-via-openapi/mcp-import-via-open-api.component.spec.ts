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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { McpImportViaOpenApiComponent } from './mcp-import-via-open-api.component';

import { GioTestingModule } from '../../../../shared/testing';

describe('McpImportViaOpenapiComponent', () => {
  let component: McpImportViaOpenApiComponent;
  let fixture: ComponentFixture<McpImportViaOpenApiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [McpImportViaOpenApiComponent, NoopAnimationsModule, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(McpImportViaOpenApiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
