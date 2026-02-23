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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SidenavLayoutComponent } from './sidenav-layout.component';
import { SidenavLayoutComponentHarness } from './sidenav-layout.component.harness';
import { AppTestingModule } from '../../testing/app-testing.module';
import { Breadcrumb } from '../breadcrumbs/breadcrumbs.component';

@Component({
  selector: 'app-test-host',
  standalone: true,
  imports: [SidenavLayoutComponent],
  template: `
    <app-sidenav-layout [breadcrumbs]="breadcrumbs">
      <div sidenavContent>Sidenav Content</div>
      <div breadcrumbActions>Breadcrumb Actions</div>
      <div mainContent>Main Content</div>
    </app-sidenav-layout>
  `,
})
class TestHostComponent {
  breadcrumbs: Breadcrumb[] = [
    { id: 'level1', label: 'Parent' },
    { id: 'level2', label: 'Child' },
    { id: 'level3', label: 'Grandchild' },
  ];
}

describe('SidenavLayoutComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let harness: SidenavLayoutComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, NoopAnimationsModule, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SidenavLayoutComponentHarness);
  });

  it('should create', async () => {
    expect(await harness.host()).toBeTruthy();
  });

  it('should toggle sidenav', async () => {
    expect(await harness.isSidenavCollapsed()).toEqual(false);
    await harness.toggleSidenav();
    expect(await harness.isSidenavCollapsed()).toEqual(true);
  });

  it('should display breadcrumbs', async () => {
    const breadcrumbs = await harness.getBreadcrumbs();
    expect(await breadcrumbs?.getText()).toContain('Parent/Child/Grandchild');
  });

  it('should display breadcrumb actions', async () => {
    const breadcrumbActions = await harness.getBreadcrumbActions();
    expect(await breadcrumbActions?.getText()).toBe('Breadcrumb Actions');
  });
});
