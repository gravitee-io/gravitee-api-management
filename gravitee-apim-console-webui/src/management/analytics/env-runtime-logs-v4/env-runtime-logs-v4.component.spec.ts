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
import { MatCardModule } from '@angular/material/card';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

import { EnvRuntimeLogsV4Component } from './env-runtime-logs-v4.component';

import { GioTestingModule } from '../../../shared/testing';

describe('EnvRuntimeLogsV4Component', () => {
  let component: EnvRuntimeLogsV4Component;
  let fixture: ComponentFixture<EnvRuntimeLogsV4Component>;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [EnvRuntimeLogsV4Component],
      imports: [NoopAnimationsModule, GioTestingModule, MatCardModule, GioBannerModule],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvRuntimeLogsV4Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the title', () => {
    const compiled = fixture.nativeElement;
    const title = compiled.querySelector('h1');
    expect(title.textContent).toContain('Logs');
  });

  it('should render the banner warning', () => {
    const compiled = fixture.nativeElement;
    const banner = compiled.querySelector('gio-banner-info');
    expect(banner).toBeTruthy();
  });

  it('should display filters section', () => {
    const compiled = fixture.nativeElement;
    const filtersSection = compiled.querySelector('.filters-section');
    expect(filtersSection).toBeTruthy();
  });

  it('should display table section', () => {
    const compiled = fixture.nativeElement;
    const tableSection = compiled.querySelector('.table-section');
    expect(tableSection).toBeTruthy();
  });
});
