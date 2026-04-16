/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { PermissionService } from './permission-service';

describe('PermissionService', () => {
    let service: PermissionService;

    beforeEach(() => {
        service = new PermissionService();
    });

    it('should load and match anyOf', () => {
        service.load('environment', ['environment-api-r', 'environment-api-c']);

        expect(service.hasAnyOf(['environment-api-u'])).toBe(false);
        expect(service.hasAnyOf(['environment-api-r'])).toBe(true);
    });

    it('should match allOf', () => {
        service.load('environment', ['environment-api-r', 'environment-api-u']);

        expect(service.hasAllOf(['environment-api-r', 'environment-api-u'])).toBe(true);
        expect(service.hasAllOf(['environment-api-r', 'environment-api-c'])).toBe(false);
    });

    it('should clear a scope without affecting others', () => {
        service.load('organization', ['organization-user-r']);
        service.load('environment', ['environment-api-r']);
        service.clear('environment');

        expect(service.hasAnyOf(['organization-user-r'])).toBe(true);
        expect(service.hasAnyOf(['environment-api-r'])).toBe(false);
    });

    it('should reset all scopes', () => {
        service.load('organization', ['organization-user-r']);
        service.reset();

        expect(service.getAllPermissions()).toEqual([]);
    });

    it('should notify subscribers on load', () => {
        const fn = jest.fn();
        service.subscribe(fn);

        service.load('environment', ['environment-api-r']);

        expect(fn).toHaveBeenCalled();
    });

    it('should return false for empty anyOf or allOf', () => {
        service.load('environment', ['environment-api-r']);

        expect(service.hasAnyOf([])).toBe(false);
        expect(service.hasAnyOf(undefined)).toBe(false);
        expect(service.hasAllOf([])).toBe(false);
    });
});
