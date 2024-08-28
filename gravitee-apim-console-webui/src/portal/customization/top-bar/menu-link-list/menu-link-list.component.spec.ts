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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HttpTestingController } from '@angular/common/http/testing';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

import { MenuLinkListComponent } from './menu-link-list.component';
import { MenuLinkListHarness } from './menu-link-list.harness';

import { MenuLinkAddDialogHarness } from '../menu-link-dialog/menu-link-add-dialog.harness';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakePortalMenuLink, PortalMenuLink } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

describe('MenuLinkListComponent', () => {
  const fakeSnackBarService = {
    success: jest.fn(),
  };

  let component: MenuLinkListComponent;
  let fixture: ComponentFixture<MenuLinkListComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatIconTestingModule, NoopAnimationsModule, GioTestingModule],
      providers: [{ provide: SnackBarService, useValue: fakeSnackBarService }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true,
        },
      })
      .compileComponents();

    initComponent();
  });

  function initComponent(data?: PortalMenuLink[]) {
    fixture = TestBed.createComponent(MenuLinkListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    expectPortalMenuLinksRequest(data);
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should load list of menu links', async () => {
    const harness = await loader.getHarness(MenuLinkListHarness);
    expect(harness).toBeTruthy();

    expect(await harness.getAddLinkButton()).toBeTruthy();

    expect(await harness.countRows()).toEqual(2);
    const row1 = await harness.getRowByIndex(0);
    expect(row1.name).toEqual('public');
    expect(row1.type).toEqual('Link to a Website');
    expect(row1.target).toEqual('target1');
    expect(row1.visibility).toEqual('Public');
    expect(row1.updateButton).toBeTruthy();
    expect(row1.deleteButton).toBeTruthy();

    const row2 = await harness.getRowByIndex(1);
    expect(row2.name).toEqual('private');
    expect(row2.type).toEqual('Link to a Website');
    expect(row2.target).toEqual('target2');
    expect(row2.visibility).toEqual('Private');
    expect(row2.updateButton).toBeTruthy();
    expect(row2.deleteButton).toBeTruthy();
  });

  it('should create a link, display a success notification, refresh the list', async () => {
    const harness = await loader.getHarness(MenuLinkListHarness);
    expect(harness).toBeTruthy();

    expect(await harness.getAddLinkButton()).toBeTruthy();
    expect(await harness.addLinkButtonIsActive()).toBeTruthy();
    await harness.openAddLink();

    const dia = await rootLoader.getHarness(MenuLinkAddDialogHarness);
    expect(await dia.nameFieldExists()).toBeTruthy();
    expect(await dia.typeFieldExists()).toBeTruthy();
    expect(await dia.targetFieldExists()).toBeTruthy();
    expect(await dia.visibilitySelectExists()).toBeTruthy();

    await dia.selectType('Link to a Website');
    await dia.fillOutName('a name');
    await dia.fillOutTarget('https://my.target');
    await dia.selectVisibility('PRIVATE');
    expect(await dia.saveButtonEnabled()).toEqual(true);

    await dia.clickSave();

    const req = httpTestingController.expectOne({
      method: 'POST',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links`,
    });
    const newLink = {
      name: 'a name',
      type: 'EXTERNAL',
      target: 'https://my.target',
      visibility: 'PRIVATE',
    };
    expect(req.request.body).toEqual(newLink);

    req.flush(newLink);

    // Check that the snackbar has been displayed
    expect(fakeSnackBarService.success).toHaveBeenCalledWith("Menu link 'a name' created successfully");

    // Check that the table has been refreshed
    expectPortalMenuLinksRequest();
  });

  it('should not create more than 5 links', async () => {
    initComponent([
      fakePortalMenuLink({ id: 'id1', name: 'name1' }),
      fakePortalMenuLink({
        id: 'id2',
        name: 'name2',
      }),
      fakePortalMenuLink({
        id: 'id3',
        name: 'name3',
      }),

      fakePortalMenuLink({
        id: 'id4',
        name: 'name4',
      }),

      fakePortalMenuLink({
        id: 'id5',
        name: 'name5',
      }),
    ]);
    const harness = await loader.getHarness(MenuLinkListHarness);
    expect(harness).toBeTruthy();
    expect(await harness.countRows()).toEqual(5);

    expect(await harness.getAddLinkButton()).toBeTruthy();
    expect(await harness.addLinkButtonIsActive()).toBeFalsy();
  });

  it('should delete a link', async () => {
    const harness = await loader.getHarness(MenuLinkListHarness);
    expect(harness).toBeTruthy();
    const row1 = await harness.getRowByIndex(0);
    const deleteBtn = row1.deleteButton;
    expect(deleteBtn).toBeTruthy();

    await deleteBtn.click();
    const dia = await rootLoader.getHarness(GioConfirmDialogHarness);
    expect(dia).toBeTruthy();
    await dia.confirm();
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links/id1`,
        method: 'DELETE',
      })
      .flush(null);

    // Check that the snackbar has been displayed
    expect(fakeSnackBarService.success).toHaveBeenCalledWith("Menu link 'public' deleted successfully");
    // Check that the table has been refreshed
    expectPortalMenuLinksRequest();
  });

  it('should drop a link and update his order', async () => {
    const link1 = fakePortalMenuLink({ id: 'id1', name: 'Link 1️⃣', order: 1 });
    const link2 = fakePortalMenuLink({ id: 'id2', name: 'Link 2️⃣', order: 2 });
    initComponent([link1, link2]);

    const harness = await loader.getHarness(MenuLinkListHarness);
    let row1 = await harness.getRowByIndex(0);
    expect(row1.name).toEqual(link1.name);

    let row2 = await harness.getRowByIndex(1);
    expect(row2.name).toEqual(link2.name);

    component.drop({ previousIndex: 1, currentIndex: 0, item: { data: link2 } } as CdkDragDrop<string>);

    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links/id2`,
    });
    const updatedLink = {
      name: link2.name,
      target: link2.target,
      visibility: link2.visibility,
      order: 1,
    };
    expect(req.request.body).toEqual(updatedLink);
    req.flush(fakePortalMenuLink({ id: 'id2', name: 'Link 2️⃣', order: 1 }));
    fixture.detectChanges();

    row1 = await harness.getRowByIndex(0);
    expect(row1.name).toEqual(link2.name);

    row2 = await harness.getRowByIndex(1);
    expect(row2.name).toEqual(link1.name);
  });

  function expectPortalMenuLinksRequest(
    data: PortalMenuLink[] = [
      fakePortalMenuLink({ id: 'id1', name: 'public', target: 'target1', visibility: 'PUBLIC' }),
      fakePortalMenuLink({ id: 'id2', name: 'private', target: 'target2', visibility: 'PRIVATE' }),
    ],
  ) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links?page=1&perPage=9999`);
    req.flush({
      data,
    });
  }
});
