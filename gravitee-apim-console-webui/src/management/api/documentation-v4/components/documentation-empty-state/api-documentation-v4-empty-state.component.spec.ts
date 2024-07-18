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
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';

import { ApiDocumentationV4EmptyStateComponent } from './api-documentation-v4-empty-state.component';

import { ApiDocumentationV4Module } from '../../api-documentation-v4.module';
import { GioTestingModule } from '../../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiDocumentationV4EmptyStateComponent', () => {
  let fixture: ComponentFixture<ApiDocumentationV4EmptyStateComponent>;
  let component: ApiDocumentationV4EmptyStateComponent;
  let harnessLoader: HarnessLoader;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4EmptyStateComponent],
      imports: [NoopAnimationsModule, GioTestingModule, ApiDocumentationV4Module, GioTestingModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-documentation-u', 'api-documentation-c', 'api-documentation-r'],
        },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4EmptyStateComponent);
    component = fixture.componentInstance;
    harnessLoader = await TestbedHarnessEnvironment.loader(fixture);
  };

  beforeEach(async () => await init());

  it('should show empty state text', async () => {
    component.emptyPageTitle = 'No pages available yet';
    component.emptyPageMessage = 'Start creating pages to fill up your folder.';
    component.showAddPageButton = true; // Ensure the button is displayed

    fixture.detectChanges();

    const title = await harnessLoader.getHarness(DivHarness.with({ selector: '.mat-h2' }));
    expect(await title.getText()).toEqual('No pages available yet');
    const subtitle = await harnessLoader.getHarness(DivHarness.with({ selector: '.mat-body-2' }));
    expect(await subtitle.getText()).toEqual('Start creating pages to fill up your folder.');
  });

  it('should show menu to select page type and emit event when clicking on add button', async () => {
    component.showAddPageButton = true;
    fixture.detectChanges();

    const spy = jest.spyOn(component.addPage, 'emit');
    const button = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Add new page' }));
    await button.click();

    const menu = await harnessLoader.getHarness(MatMenuHarness);
    await menu.clickItem({ text: new RegExp('MARKDOWN', 'i') });

    expect(spy).toHaveBeenCalled();
  });
});
