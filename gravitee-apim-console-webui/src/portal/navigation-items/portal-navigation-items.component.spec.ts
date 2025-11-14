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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';

import { PortalNavigationItemsComponent } from './portal-navigation-items.component';
import { PortalNavigationItemsHarness } from './portal-navigation-items.harness';
import { AddSectionDialogHarness } from './add-section-dialog/add-section-dialog.harness';

import { GioTestingModule } from '../../shared/testing';
import { GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';

describe('PortalNavigationItemsComponent', () => {
  let fixture: ComponentFixture<PortalNavigationItemsComponent>;
  let harness: PortalNavigationItemsHarness;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, PortalNavigationItemsComponent],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-documentation-c'],
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PortalNavigationItemsComponent);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalNavigationItemsHarness);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  describe('adding a section', () => {
    describe('adding a page', () => {
      it('opens the dialog when the Add button is clicked and Page is selected', async () => {
        expectGetMenuLinks();

        await harness.clickAddButton();
        await harness.clickPageMenuItem();

        const dialogHarness = await rootLoader.getHarness(AddSectionDialogHarness);
        expect(dialogHarness).toBeTruthy();
      });
    });
  });

  function expectGetMenuLinks() {
    httpTestingController.expectOne('assets/mocks/portal-menu-links.json').flush({ data: [] });
  }
});

