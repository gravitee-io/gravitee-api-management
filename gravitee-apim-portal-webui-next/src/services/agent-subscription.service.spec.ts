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
import { of, Subject, throwError } from 'rxjs';

import { AgentSubscriptionService } from './agent-subscription.service';
import { SubscriptionService } from './subscription.service';
import { fakeSubscription, fakeSubscriptionResponse, Subscription, SubscriptionDataKeys } from '../entities/subscription';

describe('AgentSubscriptionService', () => {
  let service: AgentSubscriptionService;
  let subscriptionService: { list: jest.Mock; get: jest.Mock };

  const aKey = (key: string, applicationName: string, revoked = false): SubscriptionDataKeys => ({
    id: `${key}-id`,
    key,
    application: { id: 'app-1', name: applicationName },
    ...(revoked ? { revoked_at: '2020-01-01T00:00:00Z' } : {}),
  });

  const accepted = (id: string, keys: SubscriptionDataKeys[]): Subscription => fakeSubscription({ id, status: 'ACCEPTED', keys });

  const listing = (subscriptions: Subscription[]) => of(fakeSubscriptionResponse({ data: subscriptions }));

  beforeEach(() => {
    subscriptionService = { list: jest.fn(), get: jest.fn() };
    TestBed.configureTestingModule({
      providers: [AgentSubscriptionService, { provide: SubscriptionService, useValue: subscriptionService }],
    });
    service = TestBed.inject(AgentSubscriptionService);
  });

  it('returns the key of the first accepted subscription that has an active one', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', [])]));
    subscriptionService.get.mockReturnValue(of(accepted('sub-1', [aKey('key-1', 'My App')])));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access).toEqual({ apiKey: 'key-1', applicationName: 'My App' });
      done();
    });
  });

  it('asks only for accepted subscriptions of that agent', () => {
    subscriptionService.list.mockReturnValue(listing([]));

    service.findForAgent('agent-1').subscribe();

    expect(subscriptionService.list).toHaveBeenCalledWith(expect.objectContaining({ apiIds: ['agent-1'], statuses: ['ACCEPTED'] }));
  });

  it('skips a subscription whose key is revoked and takes the next usable one', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', []), accepted('sub-2', [])]));
    subscriptionService.get.mockImplementation((id: string) =>
      of(id === 'sub-1' ? accepted('sub-1', [aKey('dead', 'Old App', true)]) : accepted('sub-2', [aKey('key-2', 'Live App')])),
    );

    service.findForAgent('agent-1').subscribe(access => {
      expect(access).toEqual({ apiKey: 'key-2', applicationName: 'Live App' });
      done();
    });
  });

  it('returns nothing when the viewer has no subscription', done => {
    subscriptionService.list.mockReturnValue(listing([]));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access).toBeNull();
      done();
    });
  });

  it('returns nothing when no subscription issued a usable key', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', [])]));
    subscriptionService.get.mockReturnValue(of(accepted('sub-1', [])));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access).toBeNull();
      done();
    });
  });

  it('returns nothing when the listing fails', done => {
    subscriptionService.list.mockReturnValue(throwError(() => new Error('boom')));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access).toBeNull();
      done();
    });
  });

  it('asks for every candidate at once rather than one after another', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', []), accepted('sub-2', [])]));
    const pending = new Subject<Subscription>();
    subscriptionService.get.mockReturnValue(pending);

    service.findForAgent('agent-1').subscribe();

    expect(subscriptionService.get).toHaveBeenCalledTimes(2);
    pending.complete();
    done();
  });

  it('returns nothing when reading the only subscription fails', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', [])]));
    subscriptionService.get.mockReturnValue(throwError(() => new Error('boom')));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access).toBeNull();
      done();
    });
  });
});
