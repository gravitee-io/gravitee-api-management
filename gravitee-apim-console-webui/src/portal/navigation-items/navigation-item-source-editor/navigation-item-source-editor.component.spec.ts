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
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { NavigationItemSourceEditorComponent, stripLegacyAutoFetchFromSchema } from './navigation-item-source-editor.component';
import { NavigationItemSourceEditorHarness } from './navigation-item-source-editor.harness';

import { fakeFetcherList } from '../../../entities/fetcher/fetcher.fixture';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { PortalNavigationItemSource } from '../../../entities/management-api-v2';

describe('stripLegacyAutoFetchFromSchema', () => {
  it('removes the legacy autoFetch and fetchCron properties, required entries and if/then clause', () => {
    const githubSchema = JSON.parse(fakeFetcherList().find(fetcher => fetcher.id === 'github-fetcher').schema);

    const stripped = stripLegacyAutoFetchFromSchema(githubSchema) as Record<string, unknown>;

    const properties = stripped['properties'] as Record<string, unknown>;
    expect(properties['autoFetch']).toBeUndefined();
    expect(properties['fetchCron']).toBeUndefined();
    expect(properties['owner']).toBeDefined();
    expect(stripped['required']).toEqual(['githubUrl', 'owner', 'repository']);
    expect(stripped['if']).toBeUndefined();
    expect(stripped['then']).toBeUndefined();
  });

  it('keeps an if/then clause that does not reference autoFetch', () => {
    const schema = {
      type: 'object',
      properties: {
        mode: { type: 'string' },
        detail: { type: 'string' },
      },
      if: { properties: { mode: { const: 'advanced' } } },
      then: { required: ['detail'] },
    } as unknown as GioJsonSchema;

    const stripped = stripLegacyAutoFetchFromSchema(schema) as Record<string, unknown>;

    expect(stripped['if']).toEqual({ properties: { mode: { const: 'advanced' } } });
    expect(stripped['then']).toEqual({ required: ['detail'] });
  });

  it('leaves schemas without legacy properties untouched', () => {
    const schema = {
      type: 'object',
      properties: { url: { type: 'string' } },
      required: ['url'],
    } as unknown as GioJsonSchema;

    expect(stripLegacyAutoFetchFromSchema(schema)).toEqual(schema);
  });
});

describe('NavigationItemSourceEditorComponent', () => {
  let fixture: ComponentFixture<NavigationItemSourceEditorComponent>;
  let harness: NavigationItemSourceEditorHarness;
  let httpTestingController: HttpTestingController;

  const githubSource: PortalNavigationItemSource = {
    type: 'github-fetcher',
    configuration: { githubUrl: 'https://api.github.com', owner: 'gravitee-io', repository: 'docs' },
    useAutoFetch: false,
  };

  async function createComponent(source: PortalNavigationItemSource | null, disabled: boolean) {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, NavigationItemSourceEditorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(NavigationItemSourceEditorComponent);
    fixture.componentRef.setInput('source', source);
    fixture.componentRef.setInput('disabled', disabled);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, NavigationItemSourceEditorHarness);
    fixture.detectChanges();

    httpTestingController
      .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/fetchers?expand=schema` })
      .flush(fakeFetcherList());
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  it('is fully read-only when disabled (no update permission)', async () => {
    await createComponent(githubSource, true);

    expect(await harness.isTypeSelectDisabled()).toBe(true);
    expect(await harness.hasSaveButton()).toBe(false);
    expect(await harness.hasRemoveButton()).toBe(false);
  });

  it('emits the source built from the form on save', async () => {
    await createComponent(githubSource, false);
    const emitted: PortalNavigationItemSource[] = [];
    fixture.componentInstance.saveSource.subscribe(source => emitted.push(source));

    await harness.toggleAutoFetch();
    fixture.detectChanges();
    await harness.setCron('0 0 * * * *');
    await harness.save();

    expect(emitted).toEqual([
      expect.objectContaining({
        type: 'github-fetcher',
        useAutoFetch: true,
        fetchCron: '0 0 * * * *',
        configuration: expect.objectContaining({ owner: 'gravitee-io' }),
      }),
    ]);
  });

  it('disables save when the persisted cron of an auto-fetch source is invalid', async () => {
    await createComponent({ ...githubSource, useAutoFetch: true, fetchCron: 'not a cron' }, false);

    expect(await harness.isSaveDisabled()).toBe(true);
  });

  it('edits the auto-fetch frequency with the cron builder and blocks save on an invalid expression', async () => {
    await createComponent(githubSource, false);

    await harness.toggleAutoFetch();
    fixture.detectChanges();

    await harness.setCron('not a cron');
    expect(await harness.isSaveDisabled()).toBe(true);

    await harness.setCron('0 */10 * * * *');
    expect(await harness.isSaveDisabled()).toBe(false);
  });
});
