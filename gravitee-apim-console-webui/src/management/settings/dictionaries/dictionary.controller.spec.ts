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
  let $mdDialog: any;
  let NotificationService: any;
  let DictionaryService: any;
  let ngRouter: any;

  beforeEach(() => {
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

    controller = new DictionaryController($mdDialog, NotificationService, DictionaryService, ngRouter);
    controller['dictionary'] = {
      properties: {
        large_value: 'short',
      },
    };
    controller['dictProperties'] = controller.computeProperties();
    controller['query'] = { total: 1 };
  });

  describe('editProperty', () => {
    it('should open the edit property dialog with the selected key and value', async () => {
      const event = { stopPropagation: jest.fn() };
      $mdDialog.show.mockResolvedValue(null);

      await controller.editProperty(event, 'large_value', 'short');

      expect(event.stopPropagation).toHaveBeenCalled();
      expect($mdDialog.show).toHaveBeenCalledWith(
        expect.objectContaining({
          controller: 'DialogDictionaryEditPropertyController',
          locals: { key: 'large_value', value: 'short' },
        }),
      );
    });

    it('should update the property with a value longer than 160 characters and refresh the table', async () => {
      const event = { stopPropagation: jest.fn() };
      const longValue = 'a'.repeat(200);
      $mdDialog.show.mockResolvedValue({ value: longValue });

      await controller.editProperty(event, 'large_value', 'short');

      expect(controller['dictionary'].properties.large_value).toBe(longValue);
      expect(controller['dictionary'].properties.large_value.length).toBeGreaterThan(160);
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: longValue, encrypted: false, encryptable: false }]);
      expect(controller['propertiesDirty']).toBe(true);
    });

    it('should not mark propertiesDirty when edit property dialog is cancelled', async () => {
      const event = { stopPropagation: jest.fn() };
      $mdDialog.show.mockRejectedValue('cancel');

      await controller.editProperty(event, 'large_value', 'short');

      expect(controller['dictionary'].properties).toEqual({ large_value: 'short' });
      expect(controller['propertiesDirty']).toBe(false);
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
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: 'saved', encrypted: false, encryptable: false }]);
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
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: 'initial', encrypted: false, encryptable: false }]);
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
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: 'updated', encrypted: false, encryptable: false }]);
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
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: 'deployed', encrypted: false, encryptable: false }]);
    });
  });

  describe('addProperty and deleteProperty', () => {
    it('should mark propertiesDirty and refresh the table when a property is added', async () => {
      $mdDialog.show.mockResolvedValue({ key: 'new_key', value: 'new_value' });

      await controller.addProperty();

      expect($mdDialog.show).toHaveBeenCalledWith(
        expect.objectContaining({
          controller: 'DialogDictionaryAddPropertyController',
        }),
      );
      expect(controller['dictionary'].properties.new_key).toBe('new_value');
      expect(controller['query'].total).toBe(2);
      expect(controller['propertiesDirty']).toBe(true);
      expect(controller['dictProperties']).toEqual(
        expect.arrayContaining([
          { key: 'large_value', value: 'short', encrypted: false, encryptable: false },
          { key: 'new_key', value: 'new_value', encrypted: false, encryptable: false },
        ]),
      );
    });

    it('should not mark propertiesDirty when add property dialog is cancelled', async () => {
      $mdDialog.show.mockRejectedValue('cancel');

      await controller.addProperty();

      expect(controller['dictionary'].properties).toEqual({ large_value: 'short' });
      expect(controller['query'].total).toBe(1);
      expect(controller['propertiesDirty']).toBe(false);
    });

    it('should mark propertiesDirty and refresh the table when a property is deleted', () => {
      controller.deleteProperty('large_value');

      expect(controller['dictionary'].properties.large_value).toBeUndefined();
      expect(controller['query'].total).toBe(0);
      expect(controller['propertiesDirty']).toBe(true);
      expect(controller['dictProperties']).toEqual([]);
    });

    it('should mark propertiesDirty when deleting selected properties', () => {
      controller['dictionary'].properties = {
        keep: '1',
        remove: '2',
      };
      controller['query'] = { total: 2 };
      controller['dictProperties'] = controller.computeProperties();
      controller['selectedProperties'] = { remove: true };

      controller.deleteSelectedProperties();

      expect(controller['dictionary'].properties).toEqual({ keep: '1' });
      expect(controller['selectedProperties'].remove).toBeUndefined();
      expect(controller['query'].total).toBe(1);
      expect(controller['propertiesDirty']).toBe(true);
      expect(controller['dictProperties']).toEqual([{ key: 'keep', value: '1', encrypted: false, encryptable: false }]);
    });
  });

  describe('encryption', () => {
    beforeEach(() => {
      controller['dictionary'] = {
        properties: { url: 'https://backend', apiKey: '\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022' },
        propertyOptions: { apiKey: { encrypted: true } },
      };
      controller['dictProperties'] = controller.computeProperties();
      controller['query'] = { total: 2 };
    });

    describe('computeProperties', () => {
      it('should carry the encrypted state onto the row', () => {
        const rows = controller.computeProperties();

        expect(rows).toEqual([
          { key: 'url', value: 'https://backend', encrypted: false, encryptable: false },
          { key: 'apiKey', value: expect.any(String), encrypted: true, encryptable: false },
        ]);
      });

      it('should treat every property as plain when no options are present', () => {
        controller['dictionary'] = { properties: { url: 'https://backend' } };

        expect(controller.computeProperties()).toEqual([{ key: 'url', value: 'https://backend', encrypted: false, encryptable: false }]);
      });
    });

    describe('encryptProperty', () => {
      it('should mark a plain property as encryptable without touching its value', () => {
        controller.encryptProperty('url');

        expect(controller['dictionary'].propertyOptions.url).toEqual({ encryptable: true });
        expect(controller['dictionary'].properties.url).toBe('https://backend');
        expect(controller['propertiesDirty']).toBe(true);
      });

      it('should create the options map when the dictionary has none', () => {
        controller['dictionary'] = { properties: { url: 'https://backend' } };

        controller.encryptProperty('url');

        expect(controller['dictionary'].propertyOptions).toEqual({ url: { encryptable: true } });
      });

      it('should expose the pending mark on the row', () => {
        controller.encryptProperty('url');

        expect(controller['dictProperties']).toContainEqual(expect.objectContaining({ key: 'url', encrypted: false, encryptable: true }));
      });
    });

    describe('undoEncryptProperty', () => {
      it('should clear a pending mark', () => {
        controller.encryptProperty('url');

        controller.undoEncryptProperty('url');

        expect(controller['dictionary'].propertyOptions.url).toBeUndefined();
        expect(controller['dictProperties']).toContainEqual(expect.objectContaining({ key: 'url', encryptable: false }));
      });

      it('should not clear an already stored encrypted state', () => {
        controller.undoEncryptProperty('apiKey');

        expect(controller['dictionary'].propertyOptions.apiKey).toEqual({ encrypted: true });
      });
    });

    describe('renewProperty', () => {
      it('should replace the value and ask for re-encryption', async () => {
        const event = { stopPropagation: jest.fn() };
        $mdDialog.show.mockResolvedValue({ value: 'newS3cr3t' });

        await controller.renewProperty(event, 'apiKey');

        expect(controller['dictionary'].properties.apiKey).toBe('newS3cr3t');
        expect(controller['dictionary'].propertyOptions.apiKey).toEqual({ encryptable: true });
        expect(controller['propertiesDirty']).toBe(true);
      });

      it('should never hand the stored value to the dialog', async () => {
        const event = { stopPropagation: jest.fn() };
        $mdDialog.show.mockResolvedValue(null);

        await controller.renewProperty(event, 'apiKey');

        expect($mdDialog.show).toHaveBeenCalledWith(expect.objectContaining({ locals: { key: 'apiKey', value: '' } }));
      });
    });

    describe('deleteProperty', () => {
      it('should drop the options entry along with the property', () => {
        controller.deleteProperty('apiKey');

        expect(controller['dictionary'].properties.apiKey).toBeUndefined();
        expect(controller['dictionary'].propertyOptions.apiKey).toBeUndefined();
      });
    });

    describe('addProperty', () => {
      it('should mark a new property as encryptable when the dialog asks for it', async () => {
        $mdDialog.show.mockResolvedValue({ key: 'token', value: 's3cr3t', encryptable: true });

        await controller.addProperty();

        expect(controller['dictionary'].properties.token).toBe('s3cr3t');
        expect(controller['dictionary'].propertyOptions.token).toEqual({ encryptable: true });
      });

      it('should leave a new property plain when the dialog does not ask for encryption', async () => {
        $mdDialog.show.mockResolvedValue({ key: 'token', value: 'plain', encryptable: false });

        await controller.addProperty();

        expect(controller['dictionary'].propertyOptions.token).toBeUndefined();
      });
    });

    describe('isEncrypted', () => {
      it('should report a stored encrypted property', () => {
        expect(controller.isEncrypted('apiKey')).toBe(true);
        expect(controller.isEncrypted('url')).toBe(false);
      });
    });
  });
});
