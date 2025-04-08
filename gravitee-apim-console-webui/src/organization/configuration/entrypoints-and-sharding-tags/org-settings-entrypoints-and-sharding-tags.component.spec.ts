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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness, GioLicenseTestingModule } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';

import { OrgSettingsEntrypointsAndShardingTagsComponent } from './org-settings-entrypoints-and-sharding-tags.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Tag } from '../../../entities/tag/tag';
import { fakeTag } from '../../../entities/tag/tag.fixture';
import { Group } from '../../../entities/group/group';
import { fakeGroup } from '../../../entities/group/group.fixture';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { fakePortalSettings } from '../../../entities/portal/portalSettings.fixture';
import { Entrypoint } from '../../../entities/entrypoint/entrypoint';
import { fakeEntrypoint } from '../../../entities/entrypoint/entrypoint.fixture';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { Environment } from '../../../entities/environment/environment';
import { fakeEnvironment } from '../../../entities/environment/environment.fixture';

describe('OrgSettingsEntrypointsAndShardingTagsComponent', () => {
  let fixture: ComponentFixture<OrgSettingsEntrypointsAndShardingTagsComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  function prepareTestTagsComponent(withLicense = false) {
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        GioTestingModule,
        OrganizationSettingsModule,
        MatIconTestingModule,
        GioLicenseTestingModule.with(withLicense),
      ],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: [
            'organization-tag-c',
            'organization-tag-u',
            'organization-tag-d',
            'organization-entrypoint-c',
            'organization-entrypoint-u',
            'organization-entrypoint-d',
          ],
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        isTabbable: () => true,
      },
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsEntrypointsAndShardingTagsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();

    expectEnvironmentListRequest([fakeEnvironment({ id: 'DEFAULT', name: 'Environment DEFAULT' })]);
  }

  describe('without license', () => {
    beforeEach(() => {
      prepareTestTagsComponent();
    });

    describe('Configuration', () => {
      beforeEach(() => {
        expectTagsListRequest([fakeTag({ restricted_groups: ['group-a'] })]);
        expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
        expectEntrypointsListRequest([fakeEntrypoint()]);
      });

      it('should edit default configuration', async () => {
        expectPortalSettingsGetRequest(
          fakePortalSettings({
            portal: {
              entrypoint: 'https://api.company.com',
              tcpPort: 4082,
              kafkaDomain: 'kafka.domain.com',
              kafkaPort: 9092,
            },
          }),
        );
        const entrypointInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=entrypoint]' }));
        const saveBar = await loader.getHarness(GioSaveBarHarness);

        await entrypointInput.setValue('https://my-new-api.company.com');

        const tcpPortInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=tcpPort]' }));
        expect(await tcpPortInput.getValue()).toEqual('4082');
        await tcpPortInput.setValue('8888');

        const kafkaDomainInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=kafkaDomain]' }));
        await kafkaDomainInput.setValue('kafka-gateway.company.com');

        const kafkaPortInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=kafkaPort]' }));
        expect(await kafkaPortInput.getValue()).toEqual('9092');
        await kafkaPortInput.setValue('1600');

        expect(await saveBar.isVisible()).toBeTruthy();
        await saveBar.clickSubmit();

        expectPortalSettingsSaveRequest(
          fakePortalSettings({
            portal: {
              entrypoint: 'https://my-new-api.company.com',
              tcpPort: 8888,
              kafkaDomain: 'kafka-gateway.company.com',
              kafkaPort: 1600,
            },
          }),
        );
      });

      it('should lock default configuration inputs', async () => {
        expectPortalSettingsGetRequest(
          fakePortalSettings({
            portal: {
              entrypoint: 'https://api.company.com',
              tcpPort: 4082,
              kafkaDomain: 'kafka.domain.com',
              kafkaPort: 9092,
            },
            metadata: { readonly: ['portal.entrypoint', 'portal.tcpPort', 'portal.kafkaDomain', 'portal.kafkaPort'] },
          }),
        );

        const entrypointInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=entrypoint]' }));

        expect(await entrypointInput.getValue()).toEqual('https://api.company.com');
        expect(await entrypointInput.isDisabled()).toEqual(true);

        const tcpPortInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=tcpPort]' }));

        expect(await tcpPortInput.getValue()).toEqual('4082');
        expect(await tcpPortInput.isDisabled()).toEqual(true);

        const kafkaDomainInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=kafkaDomain]' }));

        expect(await kafkaDomainInput.getValue()).toEqual('kafka.domain.com');
        expect(await kafkaDomainInput.isDisabled()).toEqual(true);

        const kafkaPortInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=kafkaPort]' }));

        expect(await kafkaPortInput.getValue()).toEqual('9092');
        expect(await kafkaPortInput.isDisabled()).toEqual(true);
      });

      it.each([
        '!invalid',
        'in valid',
        'too-long-string.ertmkcowxvhpzjaqfiptpddcmmcfqfkwha.xergowgbqkwrsajowkjjqjxmshzibwraekfv.lmdozkplwxxvrfjlwvpebcmzskivwezndfcjgpmld.zilqrkgamlbczjemrkhtsylfpkumhbpnk.qrcxgoeggadkewfbqpjnatmsrjflrqjhryzth',
        'sarzahlltkxeevpajsnncxgvqfqmnmfmvvtnzalgrwqvltvllqvvttneeylokkso.too-long-segment',
        'https://my-domain.org',
      ])('should have invalid Kafka Domain', async (value: string) => {
        expectPortalSettingsGetRequest(
          fakePortalSettings({
            portal: {
              entrypoint: 'https://api.company.com',
              tcpPort: 4082,
              kafkaDomain: '',
              kafkaPort: 9092,
            },
          }),
        );

        const kafkaDomainInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=kafkaDomain]' }));
        await kafkaDomainInput.setValue('kafka.domain.com');

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);

        await kafkaDomainInput.setValue(value);

        expect(await saveBar.isSubmitButtonInvalid()).toEqual(true);
      });

      it.each([
        'valid',
        'nice-domain.dev',
        'long-string.ertmkcowxvhpzjaqfiptpddcmmcfqfkwha.xergowgbqkwrsajowkjjqjxmshzibwraekfv.lmdozkplwxxvrfjlwvpebcmzskivwezndfcjgpmld.zilqrkgamlbczjemrkhtsylfpkumhbpnk',
        'sarzahlltkxeevpajsnncxgvqfqmnmfmvvtnzalgrwqvltvllqvvttneeylokks.long-segment',
        '',
        undefined,
      ])('should have valid Kafka Domain', async (value: string) => {
        expectPortalSettingsGetRequest(
          fakePortalSettings({
            portal: {
              entrypoint: 'https://api.company.com',
              tcpPort: 4082,
              kafkaDomain: '',
              kafkaPort: 9092,
            },
          }),
        );

        const kafkaDomainInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=kafkaDomain]' }));
        await kafkaDomainInput.setValue('kafka.domain.com');

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);

        await kafkaDomainInput.setValue(value);

        expect(await saveBar.isSubmitButtonInvalid()).toEqual(false);
      });
    });

    it('should display tags table', async () => {
      expectTagsListRequest([fakeTag({ restricted_groups: ['group-a'] })]);
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest([fakeEntrypoint()]);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#tagsTable' }));
      const headerRows = await table.getHeaderRows();
      const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

      const rows = await table.getRows();
      const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));

      expect(headerCells).toEqual([
        {
          description: 'Description',
          id: 'ID',
          name: 'Name',
          restrictedGroupsName: 'Restricted groups',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([
        {
          description: 'A tag for all external stuff',
          id: expect.stringContaining('external'),
          name: 'External',
          restrictedGroupsName: 'Group A',
          actions: 'editdelete',
        },
      ]);
    });

    it('should display entrypoint mappings table', async () => {
      expectTagsListRequest([fakeTag({ restricted_groups: ['group-a'] })]);
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest([fakeEntrypoint()]);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#entrypointsTable' }));
      const headerRows = await table.getHeaderRows();
      const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

      const rows = await table.getRows();
      const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));

      expect(headerCells).toEqual([
        {
          target: 'Target',
          entrypoint: 'Entrypoint',
          tags: 'Sharding Tags',
          actions: '',
        },
      ]);
      expect(rowCells).toEqual([
        {
          target: 'HTTP',
          entrypoint: expect.stringContaining('https://googl.co'),
          tags: 'External',
          actions: 'editdelete',
        },
      ]);
    });

    it('should delete a tag', async () => {
      expectTagsListRequest([fakeTag({ id: 'tag-1', restricted_groups: ['group-a'] }), fakeTag({ id: 'tag-2' })]);
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest([fakeEntrypoint({ tags: ['tag-1', 'tag-2'] }), fakeEntrypoint({ id: 'epIdB', tags: ['tag-1'] })]);
      fixture.detectChanges();

      const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to delete a tag"]' }));
      await deleteButton.click();

      const confirmDialogButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Delete' }));
      await confirmDialogButton.click();

      // Update entrypoint to delete tag
      const updateEntrypointReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/`,
      });
      expect(updateEntrypointReq.request.body.tags).toEqual(['tag-2']);
      updateEntrypointReq.flush(null);

      // Delete entrypoint with only this tag
      httpTestingController
        .expectOne({
          method: 'DELETE',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/epIdB`,
        })
        .flush(null);

      // Delete tag
      httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags/tag-1`,
      });
      // no flush stop test here
    });

    it('should delete a tag without entrypoint mapping', async () => {
      expectTagsListRequest([fakeTag({ id: 'tag-1' })]);
      expectGroupListByOrganizationRequest([]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest([]);
      fixture.detectChanges();

      const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to delete a tag"]' }));
      await deleteButton.click();

      const confirmDialogButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Delete' }));
      await confirmDialogButton.click();

      httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags/tag-1`,
      });
    });

    it('should delete a mapping', async () => {
      expectTagsListRequest([fakeTag({ id: 'tag-1', restricted_groups: ['group-a'] }), fakeTag({ id: 'tag-2' })]);
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest([fakeEntrypoint({ id: 'entrypoint-1', tags: ['tag-1', 'tag-2'] })]);
      fixture.detectChanges();

      const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to delete a mapping"]' }));
      await deleteButton.click();

      const confirmDialogButton = await rootLoader.getHarness(MatButtonHarness.with({ text: 'Delete' }));
      await confirmDialogButton.click();

      // Delete entrypoint
      httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/entrypoint-1`,
      });
      // no flush stop test here
    });

    it('should not create a new tag without license', async () => {
      expectTagsListRequest();
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest();

      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to add a tag"]' }));
      await addButton.click();

      const eeUnlockDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#gioLicenseDialog' }));
      expect(eeUnlockDialog).not.toBeNull();

      await eeUnlockDialog.close();
    });

    it('should update a tag', async () => {
      expectTagsListRequest([fakeTag({ id: 'tag-1', restricted_groups: ['group-a'] })]);
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' }), fakeGroup({ id: 'group-b', name: 'Group B' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest();

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#tagsTable' }));
      const rows = await table.getRows();
      const firstRowActionsCell = (await rows[0].getCells({ columnName: 'actions' }))[0];

      const editButton = await firstRowActionsCell.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to edit a tag"]' }));
      await editButton.click();

      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' }), fakeGroup({ id: 'group-b', name: 'Group B' })]);

      const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));

      const nameInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('New tag name');

      const descriptionInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('New tag description');

      const restrictedGroupSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName=restrictedGroups' }));
      await restrictedGroupSelect.clickOptions({ text: 'Group B' });

      await submitButton.click();

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags/tag-1` });
      expect(req.request.body).toStrictEqual({
        id: 'tag-1',
        name: 'New tag name',
        description: 'New tag description',
        restricted_groups: ['group-a', 'group-b'],
      });
      req.flush(null);

      // expect new ngOnInit()
      expectEnvironmentListRequest([fakeEnvironment({ id: 'DEFAULT', name: 'Environment DEFAULT' })]);
      expectTagsListRequest();
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest();
    });

    it('should create a new HTTP mapping', async () => {
      expectTagsListRequest([fakeTag({ id: 'tag-1', name: 'Tag 1' })]);
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest();

      const addButtonMenu = await loader.getHarness(MatMenuHarness.with({ triggerText: /Add a mapping/ }));
      await addButtonMenu.clickItem({ text: 'HTTP' });

      expectTagsListRequest([fakeTag({ id: 'tag-1', name: 'Tag 1' })]);

      const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const entrypointUrlInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=httpValue]' }));
      await entrypointUrlInput.setValue('https://my.entry');

      const tagsSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName=tags' }));
      await tagsSelect.clickOptions({ text: /^Tag 1/ });

      await submitButton.click();

      const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/` });
      expect(req.request.body).toStrictEqual({
        target: 'HTTP',
        value: 'https://my.entry',
        tags: ['tag-1'],
      });
    });

    it('should create a new KAFKA mapping', async () => {
      expectTagsListRequest([fakeTag({ id: 'tag-1', name: 'Tag 1' })]);
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest();

      const addButtonMenu = await loader.getHarness(MatMenuHarness.with({ triggerText: /Add a mapping/ }));
      await addButtonMenu.clickItem({ text: 'Kafka' });

      expectTagsListRequest([fakeTag({ id: 'tag-1', name: 'Tag 1' })]);

      const addMappingDialog = await rootLoader.getHarness(MatDialogHarness.with({ selector: '#addMappingDialog' }));

      const submitButton = await addMappingDialog.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const entrypointKafkaDomainInput = await addMappingDialog.getHarness(
        MatInputHarness.with({ selector: '[formControlName=kafkaDomain]' }),
      );
      await entrypointKafkaDomainInput.setValue('entry.my');

      const entrypointKafkaPortInput = await addMappingDialog.getHarness(MatInputHarness.with({ selector: '[formControlName=kafkaPort]' }));
      await entrypointKafkaPortInput.setValue('9092');

      const tagsSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName=tags' }));
      await tagsSelect.clickOptions({ text: /^Tag 1/ });
      await submitButton.click();

      const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/` });
      expect(req.request.body).toStrictEqual({
        target: 'KAFKA',
        value: 'entry.my:9092',
        tags: ['tag-1'],
      });
    });

    it('should update a HTTP mapping', async () => {
      expectTagsListRequest([fakeTag({ id: 'tag-1', name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);
      expectGroupListByOrganizationRequest([]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest([fakeEntrypoint({ id: 'entrypointIdA', tags: ['tag-1'] })]);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#entrypointsTable' }));
      const rows = await table.getRows();
      const firstRowActionsCell = (await rows[0].getCells({ columnName: 'actions' }))[0];

      const editButton = await firstRowActionsCell.getHarness(
        MatButtonHarness.with({ selector: '[aria-label="Button to edit a mapping"]' }),
      );
      await editButton.click();

      expectTagsListRequest([fakeTag({ id: 'tag-1', name: 'Tag 1' }), fakeTag({ id: 'tag-2', name: 'Tag 2' })]);

      const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));

      const entrypointUrlInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=httpValue]' }));
      await entrypointUrlInput.setValue('https://my.new.entry');

      const tagsSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName=tags' }));
      await tagsSelect.clickOptions({ text: /^Tag 1/ });
      await tagsSelect.clickOptions({ text: /^Tag 2/ });

      await submitButton.click();

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints/` });
      expect(req.request.body).toStrictEqual({
        id: 'entrypointIdA',
        target: 'HTTP',
        value: 'https://my.new.entry',
        tags: ['tag-2'],
      });
    });
  });

  describe('with license', () => {
    beforeEach(() => {
      prepareTestTagsComponent(true);
    });

    it('should create a new tag', async () => {
      expectTagsListRequest();
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest();

      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to add a tag"]' }));
      await addButton.click();

      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);

      const submitButton = await rootLoader.getHarness(MatButtonHarness.with({ selector: 'button[type=submit]' }));
      expect(await submitButton.isDisabled()).toBeTruthy();

      const nameInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=name]' }));
      await nameInput.setValue('Tag name');

      const descriptionInput = await rootLoader.getHarness(MatInputHarness.with({ selector: '[formControlName=description' }));
      await descriptionInput.setValue('Tag description');

      const restrictedGroupSelect = await rootLoader.getHarness(MatSelectHarness.with({ selector: '[formControlName=restrictedGroups' }));
      await restrictedGroupSelect.clickOptions({ text: 'Group A' });

      await submitButton.click();

      const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags` });
      expect(req.request.body).toStrictEqual({
        name: 'Tag name',
        description: 'Tag description',
        restricted_groups: ['group-a'],
      });
      req.flush(null);

      // expect new ngOnInit()

      expectEnvironmentListRequest([fakeEnvironment({ id: 'DEFAULT', name: 'Environment DEFAULT' })]);
      expectTagsListRequest();
      expectGroupListByOrganizationRequest([fakeGroup({ id: 'group-a', name: 'Group A' })]);
      expectPortalSettingsGetRequest(fakePortalSettings());
      expectEntrypointsListRequest();
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectTagsListRequest(tags: Tag[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags`,
      })
      .flush(tags);
  }

  function expectGroupListByOrganizationRequest(groups: Group[] = []) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/groups`,
      })
      .flush(groups);
  }

  function expectPortalSettingsGetRequest(portalSettings: PortalSettings) {
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/settings` }).flush(portalSettings);
  }

  function expectPortalSettingsSaveRequest(portalSettings: PortalSettings) {
    const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.baseURL}/settings` });
    expect(req.request.body).toStrictEqual(portalSettings);
    // no flush to stop test here
  }

  function expectEntrypointsListRequest(entrypoints: Entrypoint[] = []) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/entrypoints`,
        method: 'GET',
      })
      .flush(entrypoints);
  }

  function expectEnvironmentListRequest(environments: Environment[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments`);
    expect(req.request.method).toEqual('GET');
    req.flush(environments);
    fixture.detectChanges();
  }
});
