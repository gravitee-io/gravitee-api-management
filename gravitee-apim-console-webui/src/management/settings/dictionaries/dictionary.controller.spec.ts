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
import type {} from 'angular-material';

import DictionaryController from './dictionary.controller';

describe('DictionaryController', () => {
  let controller: DictionaryController;
  let $mdEditDialog: { small: jest.Mock };
  let $mdDialog: any;
  let NotificationService: any;
  let DictionaryService: any;
  let ngRouter: any;

  beforeEach(() => {
    $mdEditDialog = {
      small: jest.fn(),
    };
    $mdDialog = { show: jest.fn() };
    NotificationService = { show: jest.fn() };
    DictionaryService = {
      get: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
      deploy: jest.fn(),
      start: jest.fn(),
      stop: jest.fn(),
    };
    ngRouter = { navigate: jest.fn() };

    controller = new DictionaryController($mdEditDialog, $mdDialog, NotificationService, DictionaryService, ngRouter);
    controller['dictionary'] = {
      properties: {
        large_value: 'short',
      },
    };
    controller['dictProperties'] = controller.computeProperties();
    controller['query'] = { total: 1 };
  });

  describe('editProperty', () => {
    it('should open the inline edit dialog without an md-maxlength validator', () => {
      const event = { stopPropagation: jest.fn() };

      controller.editProperty(event, 'large_value', 'short');

      expect(event.stopPropagation).toHaveBeenCalled();
      expect($mdEditDialog.small).toHaveBeenCalledWith(
        expect.objectContaining({
          modelValue: 'short',
          placeholder: 'Set property value',
          targetEvent: event,
        }),
      );

      const dialogOptions = $mdEditDialog.small.mock.calls[0][0];
      expect(dialogOptions.validators).toBeUndefined();
    });

    it('should update the property with a value longer than 160 characters and refresh the table', () => {
      const event = { stopPropagation: jest.fn() };
      const longValue = 'a'.repeat(200);

      controller.editProperty(event, 'large_value', 'short');

      const dialogOptions = $mdEditDialog.small.mock.calls[0][0];
      dialogOptions.save({ $modelValue: longValue });

      expect(controller['dictionary'].properties.large_value).toBe(longValue);
      expect(controller['dictionary'].properties.large_value.length).toBeGreaterThan(160);
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: longValue }]);
      expect(controller['propertiesDirty']).toBe(true);
    });
  });

  describe('saveProperties', () => {
    it('should clear the unsaved properties hint after a successful save', async () => {
      controller['propertiesDirty'] = true;
      DictionaryService.update.mockResolvedValue({
        data: {
          properties: { large_value: 'saved' },
        },
      });

      await controller.saveProperties();

      expect(DictionaryService.update).toHaveBeenCalled();
      expect(NotificationService.show).toHaveBeenCalledWith('Properties has been updated');
      expect(controller['propertiesDirty']).toBe(false);
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: 'saved' }]);
    });
  });

  describe('propertiesDirty lifecycle', () => {
    beforeEach(() => {
      controller['propertiesDirty'] = true;
      controller['initialDictionary'] = {
        properties: { large_value: 'initial' },
      };
      controller['formDictionary'] = { $setPristine: jest.fn() };
      controller['updateMode'] = true;
    });

    it('should clear propertiesDirty on reset', () => {
      controller.reset();

      expect(controller['propertiesDirty']).toBe(false);
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: 'initial' }]);
      expect(controller['formDictionary'].$setPristine).toHaveBeenCalled();
    });

    it('should clear propertiesDirty after a successful general update', async () => {
      DictionaryService.update.mockResolvedValue({
        data: {
          properties: { large_value: 'updated' },
        },
      });

      await controller.update();

      expect(DictionaryService.update).toHaveBeenCalled();
      expect(controller['propertiesDirty']).toBe(false);
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: 'updated' }]);
    });

    it('should clear propertiesDirty after deploy reloads dictionary state', async () => {
      DictionaryService.deploy.mockResolvedValue({
        data: {
          properties: { large_value: 'deployed' },
        },
      });

      await controller.deploy();

      expect(DictionaryService.deploy).toHaveBeenCalled();
      expect(controller['propertiesDirty']).toBe(false);
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: 'deployed' }]);
    });
  });
});
