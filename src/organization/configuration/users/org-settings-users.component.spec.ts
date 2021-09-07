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
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';

import { OrgSettingsUsersComponent } from './org-settings-users.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { UIRouterStateParams, UIRouterState } from '../../../ajs-upgraded-providers';

describe('OrgSettingsUsersComponent', () => {
  let fixture: ComponentFixture<OrgSettingsUsersComponent>;
  let loader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OrganizationSettingsModule],
      providers: [
        { provide: UIRouterState, useValue: { go: jest.fn() } },
        { provide: UIRouterStateParams, useValue: {} },
      ],
    });
    fixture = TestBed.createComponent(OrgSettingsUsersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  });

  it('should display an empty table', async () => {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#usersTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await await parallel(() => rows.map((row) => row.getCellTextByIndex()));

    expect(headerCells).toEqual([
      {
        actions: '',
        displayName: 'Display name',
        email: 'Email',
        source: 'Source',
        status: 'Status',
        userPicture: '',
      },
    ]);
    expect(rowCells).toEqual([['No user']]);
  });
});
