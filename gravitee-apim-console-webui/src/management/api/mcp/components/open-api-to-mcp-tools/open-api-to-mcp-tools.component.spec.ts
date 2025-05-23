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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { OpenApiToMcpToolsComponent } from './open-api-to-mcp-tools.component';

@Component({
  template: `
    <form [formGroup]="form">
      <open-api-to-mcp-tools formControlName="openApiTools"></open-api-to-mcp-tools>
    </form>
  `,
  imports: [OpenApiToMcpToolsComponent, ReactiveFormsModule, NoopAnimationsModule],
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
  let component: OpenApiToMcpToolsComponent;
  let fixture: ComponentFixture<OpenApiToMcpToolsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(OpenApiToMcpToolsComponent);
    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
