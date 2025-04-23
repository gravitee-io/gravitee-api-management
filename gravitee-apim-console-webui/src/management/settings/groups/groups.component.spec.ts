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
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatRowHarness, MatTableHarness } from '@angular/material/table/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';

import { GroupsComponent } from './groups.component';

import { Group } from '../../../entities/group/group';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';

describe('GroupsComponent', () => {
  let fixture: ComponentFixture<GroupsComponent>;
  let harnessLoader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-group-u', 'environment-group-d', 'environment-group-c', 'environment-settings-r'],
        },
      ],
      imports: [GroupsComponent, GioTestingModule, NoopAnimationsModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(GroupsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Settings', () => {
    beforeEach(async () => {
      await init();
    });

    it('should be checked when at least one user group is required by applications', async () => {
      expectGetSettings(true);
      expectGetGroupsList([]);
      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness);
      expect(await toggle.isChecked()).toEqual(true);
    });

    it('should not be checked when groups are not required by applications', async () => {
      expectGetSettings(false);
      expectGetGroupsList([]);
      const toggle = await harnessLoader.getHarness(MatSlideToggleHarness);

      expect(await toggle.isChecked()).toEqual(false);
    });
  });

  describe('No groups available', () => {
    beforeEach(async () => {
      await init();
      expectGetSettings(false);
      expectGetGroupsList([]);
    });

    it('should display message when there are no groups', async () => {
      const tableRows = await getTableRows();

      expect(await tableRows[0].host().then((host) => host.text())).toContain('No groups available to display.');
    });
  });

  describe('List available groups', () => {
    beforeEach(async () => {
      await init();
      expectGetSettings(false);
      expectGetGroupsList([
        { id: '1', name: 'Group 1', manageable: true },
        { id: '2', name: 'Group 2', manageable: true },
      ]);
    });

    it('should display list of available groups sorted by name', async () => {
      expect(await getTableRows().then((rows) => rows.length)).toEqual(2);
      expect(await getTextByColumnNameAndRowIndex('name', 0)).toEqual('Group 1');
      expect(await getTextByColumnNameAndRowIndex('name', 1)).toEqual('Group 2');
    });
  });

  describe('Delete group', () => {
    beforeEach(async () => {
      await init();
      expectGetSettings(true);
    });

    it('should not delete when API role is primary owner', async () => {
      expectGetGroupsList([{ id: '1', name: 'Group 1', manageable: true, roles: { API: 'PRIMARY_OWNER' } }]);
      const deleteButton = await getButtonByRowIndexAndTooltip(0, 'Delete group');

      expect(await deleteButton.isDisabled()).toEqual(true);
    });

    it('should delete group', async () => {
      expectGetGroupsList([{ id: '1', name: 'Group 1', manageable: true, roles: { API: 'OWNER' } }]);
      const deleteButton = await getButtonByRowIndexAndTooltip(0, 'Delete group');
      expect(await deleteButton.isDisabled()).toEqual(false);

      await deleteButton.click();

      const confirmDialog = await rootLoader.getHarnessOrNull(GioConfirmDialogHarness);
      expect(confirmDialog).toBeTruthy();
      await confirmDialog.confirm();
      expectDeleteGroup('1');
      expectGetGroupsList([]);
    });
  });

  function expectGetGroupsList(groups: Group[]) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/groups`).flush(groups);
  }

  function expectDeleteGroup(id: string) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups/${id}`,
        method: 'DELETE',
      })
      .flush({});
  }

  function expectGetSettings(userGroupRequired: boolean) {
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`)
      .flush({ userGroup: { required: { enabled: userGroupRequired } } });
  }

  async function getTableRows(): Promise<MatRowHarness[]> {
    return await harnessLoader.getHarness(MatTableHarness).then((table) => table.getRows());
  }

  async function getTextByColumnNameAndRowIndex(columnName: string, index: number): Promise<string> {
    return await getTableRows()
      .then((rows) => rows[index])
      .then((row) => row.getCellTextByIndex({ columnName }).then((cell) => cell[0]));
  }

  async function getButtonByRowIndexAndTooltip(rowIndex: number, tooltipText: string): Promise<MatButtonHarness | null> {
    return await getTableRows()
      .then((rows) => rows[rowIndex].getCells({ columnName: 'actions' }))
      .then((cells) => cells[0])
      .then((actionCell) => actionCell.getHarnessOrNull(MatButtonHarness.with({ selector: `[mattooltip="${tooltipText}"]` })));
  }
});
