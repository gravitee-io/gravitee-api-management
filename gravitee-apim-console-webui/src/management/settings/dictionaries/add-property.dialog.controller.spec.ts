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
import DialogDictionaryAddPropertyController from './add-property.dialog.controller';

describe('DialogDictionaryAddPropertyController', () => {
  let $scope: any;
  let $mdDialog: any;
  let controller: any;

  const build = (existingKeys: string[] = []) => {
    $scope = {};
    $mdDialog = { hide: jest.fn() };
    controller = new (DialogDictionaryAddPropertyController as any)($scope, $mdDialog, { existingKeys });
    return controller;
  };

  describe('isDuplicate', () => {
    it('should report a key that already exists', () => {
      build(['apiKey', 'url']);

      expect(controller.isDuplicate('apiKey')).toBe(true);
    });

    it('should ignore surrounding whitespace', () => {
      build(['apiKey']);

      expect(controller.isDuplicate('  apiKey  ')).toBe(true);
    });

    it('should accept a new key', () => {
      build(['apiKey']);

      expect(controller.isDuplicate('dbPassword')).toBe(false);
    });

    it('should accept an empty key so the required validator owns that message', () => {
      build(['apiKey']);

      expect(controller.isDuplicate(undefined)).toBe(false);
      expect(controller.isDuplicate('')).toBe(false);
    });
  });

  describe('save', () => {
    it('should hand back the key, value and encryption intent', () => {
      build([]);
      $scope.property = { name: 'apiKey', value: 's3cr3t', encryptable: true };

      controller.save();

      expect($mdDialog.hide).toHaveBeenCalledWith({ key: 'apiKey', value: 's3cr3t', encryptable: true });
    });
  });
});
