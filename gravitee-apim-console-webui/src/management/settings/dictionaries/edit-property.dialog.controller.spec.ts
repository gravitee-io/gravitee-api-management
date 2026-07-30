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
import DialogDictionaryEditPropertyController from './edit-property.dialog.controller';

describe('DialogDictionaryEditPropertyController', () => {
  let $scope: any;
  let $mdDialog: { hide: jest.Mock };
  let controller: any;

  beforeEach(() => {
    $scope = {};
    $mdDialog = { hide: jest.fn() };
    controller = new (DialogDictionaryEditPropertyController as any)($scope, $mdDialog, {
      key: 'large_value',
      value: 'initial',
    });
  });

  it('should seed the scope from dialog locals using add-dialog naming (name/value)', () => {
    expect($scope.property).toEqual({
      name: 'large_value',
      value: 'initial',
    });
  });

  it('should emit only the edited value on save', () => {
    $scope.property.value = 'updated-value';

    controller.save();

    expect($mdDialog.hide).toHaveBeenCalledWith({ value: 'updated-value' });
  });

  it('should close the dialog without a payload on cancel', () => {
    controller.hide();

    expect($mdDialog.hide).toHaveBeenCalledWith();
  });
});
