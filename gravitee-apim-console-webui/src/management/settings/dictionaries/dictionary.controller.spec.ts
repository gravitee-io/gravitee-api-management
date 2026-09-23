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
      type: 'MANUAL',
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
      DictionaryService.get.mockResolvedValue({ data: { type: 'MANUAL', properties: { large_value: 'short' } } });
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

    it('should keep propertiesDirty after a general update, which does not save properties', async () => {
      controller['updateMode'] = true;
      DictionaryService.get.mockResolvedValue({ data: { type: 'MANUAL', properties: { large_value: 'short' } } });
      DictionaryService.update.mockResolvedValue({
        data: {
          properties: { large_value: 'updated' },
        },
      });

      await controller.update();

      expect(DictionaryService.update).toHaveBeenCalled();
      expect(controller['propertiesDirty']).toBe(true);
      expect(controller['dictProperties']).toEqual([{ key: 'large_value', value: 'short', encrypted: false, encryptable: false }]);
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
        controller['dictionary'] = { type: 'MANUAL', properties: { url: 'https://backend' } };

        expect(controller.computeProperties()).toEqual([{ key: 'url', value: 'https://backend', encrypted: false, encryptable: false }]);
      });
    });

    describe('masking', () => {
      it('should mask an encrypted value whatever the API returned', () => {
        controller['dictionary'] = {
          properties: { apiKey: 'hK3nB2xQ-raw-ciphertext', url: 'https://backend' },
          propertyOptions: { apiKey: { encrypted: true } },
        };

        const rows = controller.computeProperties();

        expect(rows).toContainEqual({ key: 'apiKey', value: '\u2022'.repeat(12), encrypted: true, encryptable: false });
        expect(rows).toContainEqual({ key: 'url', value: 'https://backend', encrypted: false, encryptable: false });
      });

      it('should not let the mask reach the values that are sent back', () => {
        controller['dictionary'] = {
          properties: { apiKey: 'hK3nB2xQ-raw-ciphertext' },
          propertyOptions: { apiKey: { encrypted: true } },
        };

        controller.computeProperties();

        expect(controller['dictionary'].properties.apiKey).toBe('hK3nB2xQ-raw-ciphertext');
      });
    });

    describe('masking a pending secret', () => {
      it('should mask a value that is only marked for encryption, as it already does for a stored one', () => {
        controller['dictionary'] = { properties: { apiKey: 'just-typed-secret' }, propertyOptions: {} };

        controller.encryptProperty('apiKey');

        expect(controller['dictProperties']).toContainEqual({
          key: 'apiKey',
          value: '\u2022'.repeat(12),
          encrypted: false,
          encryptable: true,
        });
        expect(controller['dictionary'].properties.apiKey).toBe('just-typed-secret');
      });
    });

    describe('editing a masked row', () => {
      it('should treat a pending mark as masked, so the table cannot offer to edit it', () => {
        controller['dictionary'] = { type: 'MANUAL', properties: { apiKey: 'real-secret' }, propertyOptions: {} };
        controller.encryptProperty('apiKey');

        const row = controller['dictProperties'].find(entry => entry.key === 'apiKey');

        expect(controller.isMasked(row)).toBe(true);
      });

      it('should not mask a plain row', () => {
        controller['dictionary'] = { type: 'MANUAL', properties: { url: 'https://backend' } };

        const row = controller.computeProperties().find(entry => entry.key === 'url');

        expect(controller.isMasked(row)).toBe(false);
      });

      it('should never hand the mask to the edit dialog', async () => {
        controller['dictionary'] = { type: 'MANUAL', properties: { apiKey: 'real-secret' }, propertyOptions: {} };
        controller.encryptProperty('apiKey');
        const row = controller['dictProperties'].find(entry => entry.key === 'apiKey');
        $mdDialog.show.mockResolvedValue({ value: row.value });

        await controller.editProperty({ stopPropagation: jest.fn() }, 'apiKey', row.value);

        expect(controller['dictionary'].properties.apiKey).toBe('real-secret');
      });
    });

    describe('a dynamic dictionary is read-only', () => {
      it('should refuse to edit a plain value, since the provider owns it', async () => {
        controller['dictionary'] = { type: 'DYNAMIC', properties: { url: 'https://backend' } };

        await controller.editProperty({ stopPropagation: jest.fn() }, 'url', 'https://backend');

        expect($mdDialog.show).not.toHaveBeenCalled();
        expect(controller['dictionary'].properties.url).toBe('https://backend');
      });
    });

    describe('each form saves only its own section', () => {
      const serverState = {
        id: 'dic-1',
        type: 'DYNAMIC',
        name: 'NameOnTheServer',
        description: 'DescriptionOnTheServer',
        properties: { apiKey: 'fresh-value' },
      };

      beforeEach(() => {
        controller['dictionary'] = {
          id: 'dic-1',
          type: 'DYNAMIC',
          name: 'RenamedButNotSaved',
          description: 'DescriptionOnTheServer',
          properties: { apiKey: 'stale-value' },
          propertyOptions: { apiKey: { encryptable: true } },
        };
        DictionaryService.get.mockResolvedValue({ data: { ...serverState } });
        DictionaryService.update.mockResolvedValue({ data: { ...serverState } });
      });

      it('should let the properties form write the classification, never the provider values', async () => {
        await controller.saveProperties();

        expect(DictionaryService.get).toHaveBeenCalledWith('dic-1');
        expect(DictionaryService.update).toHaveBeenCalledWith(
          expect.objectContaining({
            properties: { apiKey: 'fresh-value' },
            propertyOptions: { apiKey: { encryptable: true } },
          }),
        );
      });

      it('should not let the properties form save an unsaved name', async () => {
        await controller.saveProperties();

        expect(DictionaryService.update).toHaveBeenCalledWith(expect.objectContaining({ name: 'NameOnTheServer' }));
      });

      it('should let the general form save the name without touching the provider values', async () => {
        controller['updateMode'] = true;

        await controller.update();

        expect(DictionaryService.update).toHaveBeenCalledWith(
          expect.objectContaining({ name: 'RenamedButNotSaved', properties: { apiKey: 'fresh-value' } }),
        );
      });

      it('should let the properties form write the values on a manual dictionary', async () => {
        controller['dictionary'] = { id: 'man-1', type: 'MANUAL', properties: { apiKey: 'typed-value' } };
        DictionaryService.get.mockResolvedValue({ data: { id: 'man-1', type: 'MANUAL', properties: { apiKey: 'on-server' } } });

        await controller.saveProperties();

        expect(DictionaryService.update).toHaveBeenCalledWith(expect.objectContaining({ properties: { apiKey: 'typed-value' } }));
      });
    });

    describe("a save keeps the other form's unsaved edits", () => {
      beforeEach(() => {
        const server = {
          id: 'dic-1',
          type: 'MANUAL',
          name: 'ServerName',
          description: 'ServerDescription',
          properties: { apiKey: 'server-value' },
        };
        controller['dictionary'] = { ...server, properties: { ...server.properties } };
        controller['updateMode'] = true;
        controller['query'] = { total: 1 };
        DictionaryService.get.mockResolvedValue({ data: { ...server, properties: { ...server.properties } } });
        DictionaryService.update.mockImplementation(sent => Promise.resolve({ data: { ...sent } }));
      });

      it('should keep an unsaved padlock mark when the general form is saved', async () => {
        controller.encryptProperty('apiKey');

        await controller.update();

        expect(controller['dictionary'].propertyOptions).toEqual({ apiKey: { encryptable: true } });
        expect(controller['propertiesDirty']).toBe(true);
      });

      it('should keep an unsaved name when the properties form is saved', async () => {
        controller['dictionary'].name = 'TypedButNotSaved';

        await controller.saveProperties();

        expect(controller['dictionary'].name).toBe('TypedButNotSaved');
      });
    });

    describe('reset after a save', () => {
      beforeEach(() => {
        controller['dictionary'] = { id: 'dic-1', type: 'MANUAL', name: 'Dictionary', properties: { a: '1' } };
        controller['initialDictionary'] = { id: 'dic-1', type: 'MANUAL', name: 'Dictionary', properties: { a: '1' } };
        controller['query'] = { total: 1 };
        controller['formDictionary'] = { $setPristine: jest.fn() };

        DictionaryService.get.mockResolvedValue({ data: { id: 'dic-1', type: 'MANUAL', name: 'Dictionary', properties: { a: '1' } } });
        DictionaryService.update.mockResolvedValue({
          data: { id: 'dic-1', type: 'MANUAL', name: 'Dictionary', properties: { a: '1', b: '2' } },
        });
      });

      it('should rewind to the last save, so a saved property cannot be dropped', async () => {
        controller['dictionary'].properties.b = '2';

        await controller.saveProperties();
        controller.reset();

        expect(controller['dictionary'].properties).toEqual({ a: '1', b: '2' });
      });

      it('should keep the property count in step with the last save', async () => {
        controller['dictionary'].properties.b = '2';

        await controller.saveProperties();

        expect(controller['query'].total).toBe(2);
      });

      it('should rewind to the deployed state, not to the page load state', async () => {
        DictionaryService.deploy.mockResolvedValue({
          data: { id: 'dic-1', type: 'MANUAL', name: 'Dictionary', properties: { a: '1', b: '2' } },
        });

        await controller.deploy();
        controller.reset();

        expect(controller['dictionary'].properties).toEqual({ a: '1', b: '2' });
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
        controller['dictionary'] = { type: 'MANUAL', properties: { url: 'https://backend' } };

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
        expect(controller['dictionary'].propertyOptions.apiKey).toEqual({ encrypted: true, encryptable: true });
        expect(controller['propertiesDirty']).toBe(true);
      });

      it('should keep the stored encrypted state so a renewed row cannot be downgraded', async () => {
        const event = { stopPropagation: jest.fn() };
        $mdDialog.show.mockResolvedValue({ value: 'newS3cr3t' });
        await controller.renewProperty(event, 'apiKey');

        controller.undoEncryptProperty('apiKey');

        expect(controller['dictionary'].propertyOptions.apiKey).toEqual({ encrypted: true, encryptable: true });
        expect(controller.isEncrypted('apiKey')).toBe(true);
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
