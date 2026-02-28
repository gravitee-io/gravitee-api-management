/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatTabNavBarHarness } from '@angular/material/tabs/testing';

import { ApiProductNavigationTabsComponent, ApiProductTabMenuItem } from './api-product-navigation-tabs.component';

import { GioTestingModule } from '../../../../shared/testing';

describe('ApiProductNavigationTabsComponent', () => {
  let fixture: ComponentFixture<ApiProductNavigationTabsComponent>;
  let loader: HarnessLoader;

  const tabs: ApiProductTabMenuItem[] = [
    { displayName: 'Plans', routerLink: './plans' },
    { displayName: 'Settings', routerLink: './settings' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiProductNavigationTabsComponent, NoopAnimationsModule, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductNavigationTabsComponent);
    fixture.componentRef.setInput('tabMenuItems', tabs);
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('renders one tab link per item in tabMenuItems input', async () => {
    const navBar = await loader.getHarness(MatTabNavBarHarness);
    const links = await navBar.getLinks();
    expect(links).toHaveLength(tabs.length);
  });

  it('displays correct label for each tab', async () => {
    const navBar = await loader.getHarness(MatTabNavBarHarness);
    const links = await navBar.getLinks();
    const labels = await Promise.all(links.map(link => link.getLabel()));
    expect(labels).toEqual(['Plans', 'Settings']);
  });

  it('renders no tab links when tabMenuItems is empty', async () => {
    fixture.componentRef.setInput('tabMenuItems', []);
    fixture.detectChanges();

    const navBar = await loader.getHarness(MatTabNavBarHarness);
    const links = await navBar.getLinks();
    expect(links).toHaveLength(0);
  });
});
