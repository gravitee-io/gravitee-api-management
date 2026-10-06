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
  {
    crossId: 'prod',
    name: 'Production',
    connections: [
      { crossId: 'internal', name: 'Internal' },
      { crossId: 'external', name: 'External' },
    ],
  },
  { crossId: 'dev', name: 'Dev', connections: [{ crossId: 'dev-plain', name: 'Plaintext' }] },
] as DeployedCluster[];

@Component({
  template: '<formly-form [form]="form" [fields]="fields" [model]="model"></formly-form>',
  standalone: false,
})
class HostComponent {
  form = new FormGroup({});
  model: Record<string, unknown> = {};
  fields: FormlyFieldConfig[] = [];
}

describe('ClusterConnectionTypeComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let httpTestingController: HttpTestingController;

  async function render(fields: FormlyFieldConfig[], model: Record<string, unknown>, expectedUrl: string) {
    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.fields = fields;
    fixture.componentInstance.model = model;
    fixture.detectChanges();
    httpTestingController.expectOne(expectedUrl).flush(CLUSTERS);
    fixture.detectChanges();
    return TestbedHarnessEnvironment.loader(fixture).getHarness(MatAutocompleteHarness);
  }

  const connectionField: FormlyFieldConfig = { key: 'connectionCrossId', type: 'cluster-connection-type', props: { label: 'Connection' } };

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [HostComponent],
      imports: [NoopAnimationsModule, GioTestingModule, FormlyModule.forRoot(), GioFormJsonSchemaExtendedModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTestingController.verify());

  it('offers the connections of the cluster picked in the same form', async () => {
    const autocomplete = await render(
      [{ key: 'clusterCrossId', type: 'input' }, connectionField],
      { clusterCrossId: 'dev' },
      `${CONSTANTS_TESTING.env.v2BaseURL}/clusters/deployed`,
    );

    await autocomplete.focus();
    const options = await autocomplete.getOptions();
    expect(await Promise.all(options.map(option => option.getText()))).toEqual(['Plaintext (dev-plain)']);
  });

  it('offers the connections of every deployed registered cluster when the cluster is picked in another form', async () => {
    const autocomplete = await render([connectionField], {}, `${CONSTANTS_TESTING.env.v2BaseURL}/clusters/deployed?type=KAFKA_CLUSTER`);

    await autocomplete.focus();
    const options = await autocomplete.getOptions();
    expect(await Promise.all(options.map(option => option.getText()))).toEqual([
      'Production / Internal (internal)',
      'Production / External (external)',
      'Dev / Plaintext (dev-plain)',
    ]);
  });

  it('filters by the typed text and warns about an unknown connection when the cluster is picked in another form', async () => {
    const autocomplete = await render([connectionField], {}, `${CONSTANTS_TESTING.env.v2BaseURL}/clusters/deployed?type=KAFKA_CLUSTER`);

    await autocomplete.enterText('extern');
    const options = await autocomplete.getOptions();
    expect(await Promise.all(options.map(option => option.getText()))).toEqual(['Production / External (external)']);
    expect(fixture.nativeElement.textContent).toContain('Connection not found!');

    await autocomplete.selectOption({ text: /External/ });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Connection not found!');
  });
});
