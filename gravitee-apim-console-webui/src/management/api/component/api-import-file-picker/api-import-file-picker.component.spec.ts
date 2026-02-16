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
import { GioFormFilePickerInputHarness } from '@gravitee/ui-particles-angular';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiImportFilePickerComponent } from './api-import-file-picker.component';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioTestingModule } from '../../../../shared/testing';

describe('FilePickerComponent', () => {
  let component: ApiImportFilePickerComponent;
  let fixture: ComponentFixture<ApiImportFilePickerComponent>;
  let harness: GioFormFilePickerInputHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiImportFilePickerComponent, GioTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiImportFilePickerComponent);
    component = fixture.componentInstance;
    const loader = TestbedHarnessEnvironment.loader(fixture);
    harness = await loader.getHarness(GioFormFilePickerInputHarness);
    fixture.detectChanges();
  });

  afterEach(() => jest.clearAllMocks());

  it('should not allow unsupported extension ', async () => {
    const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');

    await harness.dropFiles([new File([''], 'file.gif', { type: 'image/gif' })]);

    expect(snackBarSpy).toHaveBeenCalledWith('Invalid file format. Supported file formats: yml, yaml, json, wsdl, xml, js');
  });

  it('should emit GRAVITEE file data', async () => {
    const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
    const eventEmitterSpy = jest.spyOn(component.onFilePicked, 'emit');
    const newFile = new File(['{ "foo": "bar" }'], 'file.json', { type: 'application/json' });

    await harness.dropFiles([newFile]);

    expect(snackBarSpy).not.toHaveBeenCalled();
    expect(eventEmitterSpy).toHaveBeenCalledWith({
      importFile: newFile,
      importFileContent: '{ "foo": "bar" }',
      importType: 'GRAVITEE',
    });
  });

  it('should emit MAPI_V2 file data', async () => {
    const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
    const eventEmitterSpy = jest.spyOn(component.onFilePicked, 'emit');
    const newFile = new File([`{ "api": { "definitionVersion": "V4" }}`], 'file.json', { type: 'application/json' });

    await harness.dropFiles([newFile]);

    expect(snackBarSpy).not.toHaveBeenCalled();
    expect(eventEmitterSpy).toHaveBeenCalledWith({
      importFile: newFile,
      importFileContent: `{ "api": { "definitionVersion": "V4" }}`,
      importType: 'MAPI_V2',
    });
  });

  it.each(['swagger', 'swaggerVersion', 'openapi'])('should emit SWAGGER file data determined from file content', async openApiKey => {
    const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
    const eventEmitterSpy = jest.spyOn(component.onFilePicked, 'emit');
    const newFile = new File([`{ "${openApiKey}": "bar" }`], 'file.json', { type: 'application/json' });

    await harness.dropFiles([newFile]);

    expect(snackBarSpy).not.toHaveBeenCalled();
    expect(eventEmitterSpy).toHaveBeenCalledWith({
      importFile: newFile,
      importFileContent: `{ "${openApiKey}": "bar" }`,
      importType: 'SWAGGER',
    });
  });

  it.each(['yml', 'yaml'])('should emit SWAGGER file data determined from file extension', async fileExtension => {
    const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
    const eventEmitterSpy = jest.spyOn(component.onFilePicked, 'emit');
    const newFile = new File([''], `file.${fileExtension}`, { type: 'application/yaml' });

    await harness.dropFiles([newFile]);

    expect(snackBarSpy).not.toHaveBeenCalled();
    expect(eventEmitterSpy).toHaveBeenCalledWith({
      importFile: newFile,
      importFileContent: '',
      importType: 'SWAGGER',
    });
  });

  it.each(['xml', 'wsdl'])('should emit WSDL file data', async fileExtension => {
    const snackBarSpy = jest.spyOn(TestBed.inject(SnackBarService), 'error');
    const eventEmitterSpy = jest.spyOn(component.onFilePicked, 'emit');
    const newFile = new File([''], `file.${fileExtension}`, { type: 'application/xml' });

    await harness.dropFiles([newFile]);

    expect(snackBarSpy).not.toHaveBeenCalled();
    expect(eventEmitterSpy).toHaveBeenCalledWith({
      importFile: newFile,
      importFileContent: '',
      importType: 'WSDL',
    });
  });
});
