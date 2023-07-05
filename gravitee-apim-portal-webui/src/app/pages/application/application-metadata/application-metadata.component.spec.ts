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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';

import { PermissionsResponse, PermissionsService } from '../../../../../projects/portal-webclient-sdk/src/lib';

import { ApplicationMetadataComponent } from './application-metadata.component';

describe('ApplicationMetadataComponent', () => {
  const createComponent = createComponentFactory({
    component: ApplicationMetadataComponent,
    imports: [HttpClientTestingModule, RouterTestingModule, FormsModule, ReactiveFormsModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
  });

  let spectator: Spectator<ApplicationMetadataComponent>;
  let component;
  let permissionService: PermissionsService;

  beforeEach(() => {
    spectator = createComponent();
    permissionService = spectator.inject(PermissionsService);
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have permissions', async () => {
    const response: PermissionsResponse = {
      METADATA: ['U', 'C', 'D'],
    };
    component.application = { id: 'id' };
    jest.spyOn(permissionService as any, 'getCurrentUserPermissions').mockReturnValue(of(response));
    await component.initPermissions();
    expect(component.hasUpdatePermission).toBeTruthy();
    expect(component.hasCreatePermission).toBeTruthy();
    expect(component.hasDeletePermission).toBeTruthy();
  });

  it('should add form line', () => {
    component.hasCreatePermission = true;
    expect(component._isAddFormLine({ _new: true })).toBeTruthy();
  });

  it('should be new line', () => {
    component.hasCreatePermission = true;
    expect(component._isNewLine({ _new: true, key: 'something' })).toBeTruthy();
  });

  it('should be an update line', () => {
    component.hasUpdatePermission = true;
    expect(component._isUpdateLine({ key: 'something' })).toBeTruthy();
  });

  it.each([
    [false, true, false],
    [false, false, true],
    [false, true, true],
    [true, false, false],
    [true, false, true],
    [true, true, false],
    [true, true, true],
  ])('should be an editable line with _isAddFormLine: %s, _isNewLine: %s, _isUpdateLine: %s', (addLine, newLine, updateLine) => {
    jest.spyOn(component as any, '_isAddFormLine').mockReturnValue(addLine);
    jest.spyOn(component as any, '_isNewLine').mockReturnValue(newLine);
    jest.spyOn(component as any, '_isUpdateLine').mockReturnValue(updateLine);
    expect(component._isEditableLine({ key: 'something' })).toBeTruthy();
  });

  it('should render format with gv-select', () => {
    jest.spyOn(component as any, '_isEditableLine').mockReturnValue(true);
    const result = component._renderFormat('label');
    expect(result.type({})).toEqual('gv-select');
    expect(result.attributes.innerText({})).toBeUndefined();
  });

  it('should render format with div', () => {
    jest.spyOn(component as any, '_isEditableLine').mockReturnValue(false);
    const result = component._renderFormat('label');
    expect(result.type({})).toEqual('div');
    expect(result.attributes.innerText({ format: 'displayedValue' })).toEqual('displayedValue');
  });

  it.each([
    { itemFormat: 'boolean', expectedComponent: 'gv-checkbox' },
    { itemFormat: 'date', expectedComponent: 'gv-date-picker' },
    { itemFormat: 'anything', expectedComponent: 'gv-input' },
  ])('should render value with $expectedComponent', ({ itemFormat, expectedComponent }) => {
    jest.spyOn(component as any, '_isEditableLine').mockReturnValue(true);
    const result = component._renderValue('label');
    expect(result.type({ format: itemFormat })).toEqual(expectedComponent);
    expect(result.attributes.innerText({})).toBeUndefined();
  });

  it('should render value with div', () => {
    jest.spyOn(component as any, '_isEditableLine').mockReturnValue(false);
    const result = component._renderValue('label');
    expect(result.type({})).toEqual('div');
    expect(result.attributes.innerText({ value: 'displayedValue' })).toEqual('displayedValue');
  });
});
