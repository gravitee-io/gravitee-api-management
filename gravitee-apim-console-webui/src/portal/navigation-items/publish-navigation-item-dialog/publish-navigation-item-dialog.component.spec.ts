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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { PublishNavigationItemDialogComponent, PublishNavigationItemDialogData } from './publish-navigation-item-dialog.component';
import { PublishNavigationItemDialogHarness } from './publish-navigation-item-dialog.harness';

import {
  fakePortalNavigationApi,
  fakePortalNavigationFolder,
  fakePortalNavigationPage,
  PortalNavigationItem,
} from '../../../entities/management-api-v2';

describe('PublishNavigationItemDialogComponent', () => {
  let fixture: ComponentFixture<PublishNavigationItemDialogComponent>;
  let harness: PublishNavigationItemDialogHarness;
  let dialogRefClose: jest.Mock;
  let dialogData: PublishNavigationItemDialogData;

  beforeEach(async () => {
    dialogRefClose = jest.fn();

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, PublishNavigationItemDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: dialogRefClose } },
        { provide: MAT_DIALOG_DATA, useFactory: () => dialogData },
      ],
    }).compileComponents();
  });

  it('should show propagation checkbox for publishing a folder and return selected value', async () => {
    await createComponent(fakePortalNavigationFolder({ title: 'My Folder', published: false }));

    expect(await harness.isPropagationCheckboxChecked()).toBe(false);
    expect(await harness.getContentText()).toContain('Also publish all nested documentation and APIs');

    await harness.checkPropagationCheckbox();
    await harness.confirm();

    expect(dialogRefClose).toHaveBeenCalledWith({
      confirmed: true,
      propagatePublishToChildren: true,
    });
  });

  it('should show propagation checkbox for publishing an API', async () => {
    await createComponent(fakePortalNavigationApi({ title: 'My API', published: false }));

    expect(await harness.isPropagationCheckboxVisible()).toBe(true);
    expect(await harness.getContentText()).toContain('Also publish all nested documentation');
    expect(await harness.getContentText()).not.toContain('and APIs');
  });

  it('should not show propagation checkbox for publishing a page', async () => {
    await createComponent(fakePortalNavigationPage({ title: 'My Page', published: false }));

    expect(await harness.isPropagationCheckboxVisible()).toBe(false);

    await harness.confirm();

    expect(dialogRefClose).toHaveBeenCalledWith({
      confirmed: true,
      propagatePublishToChildren: false,
    });
  });

  it('should not show propagation checkbox for unpublishing a container', async () => {
    await createComponent(fakePortalNavigationFolder({ title: 'My Folder', published: true }));

    expect(await harness.isPropagationCheckboxVisible()).toBe(false);
    expect(await harness.getContentText()).toContain('will also unpublish all nested documentation and APIs');
  });

  it('should describe API unpublish propagation without nested APIs', async () => {
    await createComponent(fakePortalNavigationApi({ title: 'My API', published: true }));

    expect(await harness.isPropagationCheckboxVisible()).toBe(false);
    expect(await harness.getContentText()).toContain('will also unpublish all nested documentation');
    expect(await harness.getContentText()).not.toContain('and APIs');
  });

  it('should close without result when cancelled', async () => {
    await createComponent(fakePortalNavigationFolder({ title: 'My Folder', published: false }));

    await harness.cancel();

    expect(dialogRefClose).toHaveBeenCalledWith();
  });

  async function createComponent(navItem: PortalNavigationItem): Promise<void> {
    dialogData = { navItem };
    fixture = TestBed.createComponent(PublishNavigationItemDialogComponent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PublishNavigationItemDialogHarness);
  }
});
