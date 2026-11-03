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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { FormlyFieldConfig, FormlyModule } from '@ngx-formly/core';

import { GioFormJsonSchemaExtendedModule } from './form-json-schema-extended.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../testing';
import { DeployedCluster } from '../../../entities/management-api-v2';

const CLUSTERS = [
  { crossId: 'prod', name: 'Production', connections: [], bindable: false },
  { crossId: 'dev', name: 'Dev', connections: [], bindable: true },
  { crossId: 'staging', name: 'Staging', connections: [] },
] as DeployedCluster[];

@Component({
  template: '<formly-form [form]="form" [fields]="fields" [model]="model"></formly-form>',
  standalone: false,
})
class HostComponent {
  form = new FormGroup({});
  model: Record<string, unknown> = {};
  fields: FormlyFieldConfig[] = [{ key: 'clusterCrossId', type: 'cluster-type', props: { label: 'Cluster' } }];
}

describe('ClusterTypeComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let httpTestingController: HttpTestingController;

  async function render(model: Record<string, unknown>) {
    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.model = model;
    fixture.detectChanges();
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/clusters/deployed`).flush(CLUSTERS);
    fixture.detectChanges();
    const autocomplete = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatAutocompleteHarness);
    await autocomplete.clear();
    const options = await autocomplete.getOptions();
    return Promise.all(options.map(option => option.getText()));
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [HostComponent],
      imports: [NoopAnimationsModule, GioTestingModule, FormlyModule.forRoot(), GioFormJsonSchemaExtendedModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTestingController.verify());

  it('offers only the clusters the user may bind', async () => {
    expect(await render({})).toEqual(['Dev (dev)', 'Staging (staging)']);
  });

  it('still offers the cluster the API is bound to when the user is not a member of it', async () => {
    expect(await render({ clusterCrossId: 'prod' })).toEqual(['Production (prod)', 'Dev (dev)', 'Staging (staging)']);
    expect(fixture.nativeElement.textContent).not.toContain('Cluster not found!');
  });
});
