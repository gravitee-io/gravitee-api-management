/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';

import {
  applicationInvitationsEnabledGuard,
  applicationMembershipEnabledGuard,
  applicationTransferOwnershipEnabledGuard,
} from './application-membership-enabled.guard';
import { ConfigService } from '../services/config.service';

const dummyRoute = {} as ActivatedRouteSnapshot;
const dummyState = {} as RouterStateSnapshot;

describe('applicationMembershipEnabledGuard', () => {
  it('should allow navigation when member mapping is enabled', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ConfigService,
          useValue: {
            configuration: { portalNext: { applications: { membership: { enabled: true } } } },
          },
        },
        { provide: Router, useValue: { parseUrl: jest.fn() } },
      ],
    });

    const result = TestBed.runInInjectionContext(() => applicationMembershipEnabledGuard(dummyRoute, dummyState));

    expect(result).toBe(true);
  });

  it('should redirect to home when member mapping is disabled', () => {
    const parseUrl = jest.fn().mockReturnValue('PARSED');
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ConfigService,
          useValue: {
            configuration: { portalNext: { applications: { membership: { enabled: false } } } },
          },
        },
        { provide: Router, useValue: { parseUrl } },
      ],
    });

    const result = TestBed.runInInjectionContext(() => applicationMembershipEnabledGuard(dummyRoute, dummyState));

    expect(parseUrl).toHaveBeenCalledWith('/');
    expect(result).toBe('PARSED');
  });

  it('should redirect to home when member mapping configuration is missing', () => {
    const parseUrl = jest.fn().mockReturnValue('PARSED');
    TestBed.configureTestingModule({
      providers: [
        { provide: ConfigService, useValue: { configuration: {} } },
        { provide: Router, useValue: { parseUrl } },
      ],
    });

    TestBed.runInInjectionContext(() => applicationMembershipEnabledGuard(dummyRoute, dummyState));

    expect(parseUrl).toHaveBeenCalledWith('/');
  });
});

describe('applicationTransferOwnershipEnabledGuard', () => {
  it('should allow navigation when transfer ownership is enabled', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ConfigService,
          useValue: {
            configuration: { portalNext: { applications: { membership: { transferOwnership: { enabled: true } } } } },
          },
        },
        { provide: Router, useValue: { parseUrl: jest.fn() } },
      ],
    });

    const result = TestBed.runInInjectionContext(() => applicationTransferOwnershipEnabledGuard(dummyRoute, dummyState));

    expect(result).toBe(true);
  });

  it('should redirect to home when transfer ownership is disabled', () => {
    const parseUrl = jest.fn().mockReturnValue('PARSED');
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ConfigService,
          useValue: {
            configuration: { portalNext: { applications: { membership: { transferOwnership: { enabled: false } } } } },
          },
        },
        { provide: Router, useValue: { parseUrl } },
      ],
    });

    const result = TestBed.runInInjectionContext(() => applicationTransferOwnershipEnabledGuard(dummyRoute, dummyState));

    expect(parseUrl).toHaveBeenCalledWith('/');
    expect(result).toBe('PARSED');
  });

  it('should redirect to home when transfer ownership configuration is missing', () => {
    const parseUrl = jest.fn().mockReturnValue('PARSED');
    TestBed.configureTestingModule({
      providers: [
        { provide: ConfigService, useValue: { configuration: {} } },
        { provide: Router, useValue: { parseUrl } },
      ],
    });

    TestBed.runInInjectionContext(() => applicationTransferOwnershipEnabledGuard(dummyRoute, dummyState));

    expect(parseUrl).toHaveBeenCalledWith('/');
  });
});

describe('applicationInvitationsEnabledGuard', () => {
  it('should allow navigation when membership and invitations are both enabled', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ConfigService,
          useValue: {
            configuration: {
              portalNext: { applications: { membership: { enabled: true, invitations: { enabled: true } } } },
            },
          },
        },
        { provide: Router, useValue: { parseUrl: jest.fn() } },
      ],
    });

    const result = TestBed.runInInjectionContext(() => applicationInvitationsEnabledGuard(dummyRoute, dummyState));

    expect(result).toBe(true);
  });

  it('should redirect to home when invitations toggle is disabled', () => {
    const parseUrl = jest.fn().mockReturnValue('PARSED');
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ConfigService,
          useValue: {
            configuration: {
              portalNext: { applications: { membership: { enabled: true, invitations: { enabled: false } } } },
            },
          },
        },
        { provide: Router, useValue: { parseUrl } },
      ],
    });

    const result = TestBed.runInInjectionContext(() => applicationInvitationsEnabledGuard(dummyRoute, dummyState));

    expect(parseUrl).toHaveBeenCalledWith('/');
    expect(result).toBe('PARSED');
  });

  it('should redirect to home when parent membership toggle is disabled', () => {
    const parseUrl = jest.fn().mockReturnValue('PARSED');
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ConfigService,
          useValue: {
            configuration: {
              portalNext: { applications: { membership: { enabled: false, invitations: { enabled: true } } } },
            },
          },
        },
        { provide: Router, useValue: { parseUrl } },
      ],
    });

    const result = TestBed.runInInjectionContext(() => applicationInvitationsEnabledGuard(dummyRoute, dummyState));

    expect(parseUrl).toHaveBeenCalledWith('/');
    expect(result).toBe('PARSED');
  });

  it('should redirect to home when invitations configuration is missing', () => {
    const parseUrl = jest.fn().mockReturnValue('PARSED');
    TestBed.configureTestingModule({
      providers: [
        { provide: ConfigService, useValue: { configuration: {} } },
        { provide: Router, useValue: { parseUrl } },
      ],
    });

    TestBed.runInInjectionContext(() => applicationInvitationsEnabledGuard(dummyRoute, dummyState));

    expect(parseUrl).toHaveBeenCalledWith('/');
  });
});
