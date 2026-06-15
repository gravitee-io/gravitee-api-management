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
// UserController is typed with `angular.material.IDialogService`; pull in the @types/angular-material
// augmentation so the controller type-checks here regardless of test compilation order.
import type {} from 'angular-material';

import UserController from './user.controller';

describe('UserController (my account)', () => {
  let userService: any;
  let notificationService: any;
  let controller: UserController;

  beforeEach(() => {
    userService = {
      save: jest.fn(() => Promise.resolve()),
      currentUserPicture: jest.fn(),
    };
    notificationService = { show: jest.fn(), showError: jest.fn() };

    controller = new UserController(userService, notificationService, {} as any, {} as any, {} as any);
    controller['onSaved'] = jest.fn();
  });

  describe('save', () => {
    it('should send an empty picture to remove the avatar when "Use default" cleared a previously set picture', () => {
      // "Use default" clears both picture and picture_url; there was an original picture on load.
      controller['originalPicture'] = '/management/organizations/DEFAULT/user/avatar?123&cacheBust=0';
      controller['user'] = { picture: null, picture_url: null } as any;

      controller.save();

      expect(userService.save).toHaveBeenCalledWith(expect.objectContaining({ picture: '' }));
    });

    it('should not alter the picture on a normal save when the avatar URL is still present', () => {
      controller['originalPicture'] = '/management/organizations/DEFAULT/user/avatar?123&cacheBust=0';
      controller['user'] = { picture: null, picture_url: '/management/organizations/DEFAULT/user/avatar?123&cacheBust=0' } as any;

      controller.save();

      expect(userService.save).toHaveBeenCalledWith(expect.objectContaining({ picture: null }));
    });

    it('should not send an empty picture when there was no original picture to remove', () => {
      controller['originalPicture'] = undefined;
      controller['user'] = { picture: null, picture_url: null } as any;

      controller.save();

      expect(userService.save).toHaveBeenCalledWith(expect.objectContaining({ picture: null }));
    });
  });

  describe('$onChanges', () => {
    it('should recompute the picture URL (with the refreshed cache-bust token) when the user is rebound after a save', () => {
      userService.currentUserPicture.mockReturnValue('/management/organizations/DEFAULT/user/avatar?123&cacheBust=999');
      controller['user'] = { id: 'user-id' } as any;

      controller.$onChanges({ user: { currentValue: controller['user'] } } as any);

      expect((controller['user'] as any).picture_url).toBe('/management/organizations/DEFAULT/user/avatar?123&cacheBust=999');
      expect(controller['originalPicture']).toBe('/management/organizations/DEFAULT/user/avatar?123&cacheBust=999');
    });

    it('should do nothing when the user binding did not change', () => {
      controller['user'] = { id: 'user-id', picture_url: 'unchanged' } as any;

      controller.$onChanges({});

      expect((controller['user'] as any).picture_url).toBe('unchanged');
      expect(userService.currentUserPicture).not.toHaveBeenCalled();
    });
  });
});
