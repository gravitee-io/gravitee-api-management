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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiDocumentationV4ListNavigationHeaderComponent } from './api-documentation-v4-list-navigation-header.component';

import { ApiDocumentationV4Module } from '../../api-documentation-v4.module';
import { ApiDocumentationV4BreadcrumbHarness } from '../api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.harness';
import { GioTestingModule } from '../../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiDocumentationV4NavigationHeaderComponent', () => {
  let fixture: ComponentFixture<ApiDocumentationV4ListNavigationHeaderComponent>;
  let component: ApiDocumentationV4ListNavigationHeaderComponent;
  let harnessLoader: HarnessLoader;

  const init = async (apiPermissions = ['api-documentation-u', 'api-documentation-c', 'api-documentation-r']) => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4ListNavigationHeaderComponent],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule, GioTestingModule],
      providers: [{ provide: GioTestingPermissionProvider, useValue: apiPermissions }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4ListNavigationHeaderComponent);
    component = fixture.componentInstance;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
  };

  it('should show breadcrumb', async () => {
    await init();
    const breadcrumb = await harnessLoader.getHarness(ApiDocumentationV4BreadcrumbHarness);
    expect(await breadcrumb.getContent()).toEqual('Home');
  });

  it('should emit event when clicking on add button', async () => {
    await init();
    const spy = jest.spyOn(component.addFolder, 'emit');
    const button = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Add new folder' }));
    await button.click();

    expect(spy).toHaveBeenCalled();
  });

  it('should not show add folder button if does not have update permission', async () => {
    await init(['api-documentation-c', 'api-documentation-r']);
    const buttonHarnesses = await harnessLoader.getAllHarnesses(MatButtonHarness.with({ text: 'Add new folder' }));
    expect(buttonHarnesses.length).toEqual(0);
  });

  it('should emit event when clicking on NewtAI button', async () => {
    await init(['api-documentation-c', 'api-documentation-r']);
    const spy = jest.spyOn(component.generate, 'emit');
    const button = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'NewtAI' }));
    await button.click();

    expect(spy).toHaveBeenCalled();
  });

  it('should not show NewtAI button if does not have update permission', async () => {
    await init([]);
    const buttonHarnesses = await harnessLoader.getAllHarnesses(MatButtonHarness.with({ text: 'NewtAI' }));
    expect(buttonHarnesses.length).toEqual(0);
  });
});
