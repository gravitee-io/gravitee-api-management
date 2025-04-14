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

import { McpToolDisplayComponent } from './mcp-tool-display.component';

import { GioTestingModule } from '../../../../shared/testing';

describe('McpToolDisplayComponent', () => {
  let component: McpToolDisplayComponent;
  let fixture: ComponentFixture<McpToolDisplayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [McpToolDisplayComponent, NoopAnimationsModule, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(McpToolDisplayComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('tool', {});
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
