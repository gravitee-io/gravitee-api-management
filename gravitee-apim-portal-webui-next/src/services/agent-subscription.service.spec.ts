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

import { AgentSubscriptionService, AgentSubscriptionSummary } from './agent-subscription.service';
import { ApplicationService } from './application.service';
import { SubscriptionService } from './subscription.service';
import { fakeApplication } from '../entities/application/application.fixture';
import { PlanSecurityEnum } from '../entities/plan/plan';
import { fakeSubscription, fakeSubscriptionResponse, Subscription, SubscriptionDataKeys } from '../entities/subscription';

describe('AgentSubscriptionService', () => {
  let service: AgentSubscriptionService;
  let subscriptionService: { list: jest.Mock; get: jest.Mock };
  let applicationService: { get: jest.Mock };

  const aKey = (key: string, applicationName: string, revoked = false): SubscriptionDataKeys => ({
    id: `${key}-id`,
    key,
    application: { id: 'app-1', name: applicationName },
    ...(revoked ? { revoked_at: '2020-01-01T00:00:00Z' } : {}),
  });

  const accepted = (id: string, keys: SubscriptionDataKeys[]): Subscription => fakeSubscription({ id, status: 'ACCEPTED', keys });

  const pending = (id: string): Subscription => fakeSubscription({ id, status: 'PENDING', keys: [] });

  const listing = (subscriptions: Subscription[], metadata: Record<string, { name?: string; securityType?: PlanSecurityEnum }> = {}) =>
    of(fakeSubscriptionResponse({ data: subscriptions, metadata }));

  const aSummary = (opts: { id?: string; application?: string; planSecurity?: PlanSecurityEnum } = {}): AgentSubscriptionSummary => ({
    subscription: fakeSubscription({
      id: opts.id ?? 'sub-1',
      application: opts.application ?? 'app-1',
      status: 'ACCEPTED',
    }),
    planName: 'Gold',
    planSecurity: opts.planSecurity ?? 'API_KEY',
    applicationName: 'My App',
  });

  beforeEach(() => {
    subscriptionService = { list: jest.fn(), get: jest.fn() };
    applicationService = { get: jest.fn() };
    TestBed.configureTestingModule({
      providers: [
        AgentSubscriptionService,
        { provide: SubscriptionService, useValue: subscriptionService },
        { provide: ApplicationService, useValue: applicationService },
      ],
    });
    service = TestBed.inject(AgentSubscriptionService);
  });

  it('returns the key of the first accepted subscription that has an active one', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', [])]));
    subscriptionService.get.mockReturnValue(of(accepted('sub-1', [aKey('key-1', 'My App')])));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access.chatCredentials).toEqual({ apiKey: 'key-1', applicationName: 'My App' });
      done();
    });
  });

  it('asks for accepted, pending and paused subscriptions of that agent', () => {
    subscriptionService.list.mockReturnValue(listing([]));

    service.findForAgent('agent-1').subscribe();

    expect(subscriptionService.list).toHaveBeenCalledWith(
      expect.objectContaining({ apiIds: ['agent-1'], statuses: ['ACCEPTED', 'PENDING', 'PAUSED'] }),
    );
  });

  it('describes each subscription with its plan and application from the listing metadata', done => {
    const sub = accepted('sub-1', []);
    subscriptionService.list.mockReturnValue(
      listing([sub], {
        [sub.plan]: { name: 'Gold', securityType: 'API_KEY' },
        [sub.application]: { name: 'Portal App' },
      }),
    );
    subscriptionService.get.mockReturnValue(of(accepted('sub-1', [aKey('key-1', 'Key App')])));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access.subscriptions).toEqual([
        expect.objectContaining({
          planName: 'Gold',
          planSecurity: 'API_KEY',
          applicationName: 'Portal App',
        }),
      ]);
      done();
    });
  });

  it('reads keys only for accepted subscriptions', () => {
    subscriptionService.list.mockReturnValue(listing([pending('sub-pending'), accepted('sub-accepted', [])]));
    subscriptionService.get.mockReturnValue(of(accepted('sub-accepted', [aKey('key-1', 'My App')])));

    service.findForAgent('agent-1').subscribe();

    expect(subscriptionService.get).toHaveBeenCalledTimes(1);
    expect(subscriptionService.get).toHaveBeenCalledWith('sub-accepted');
  });

  it('ignores a pending subscription when choosing the chat key', done => {
    subscriptionService.list.mockReturnValue(listing([pending('sub-pending'), accepted('sub-2', [])]));
    subscriptionService.get.mockReturnValue(of(accepted('sub-2', [aKey('key-2', 'Live App')])));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access.chatCredentials).toEqual({ apiKey: 'key-2', applicationName: 'Live App' });
      done();
    });
  });

  it('skips a subscription whose key is revoked and takes the next usable one', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', []), accepted('sub-2', [])]));
    subscriptionService.get.mockImplementation((id: string) =>
      of(id === 'sub-1' ? accepted('sub-1', [aKey('dead', 'Old App', true)]) : accepted('sub-2', [aKey('key-2', 'Live App')])),
    );

    service.findForAgent('agent-1').subscribe(access => {
      expect(access.chatCredentials).toEqual({ apiKey: 'key-2', applicationName: 'Live App' });
      done();
    });
  });

  it('returns an empty result when the viewer has no subscription', done => {
    subscriptionService.list.mockReturnValue(listing([]));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access).toEqual({ subscriptions: [], chatCredentials: null });
      done();
    });
  });

  it('returns nothing when no subscription issued a usable key', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', [])]));
    subscriptionService.get.mockReturnValue(of(accepted('sub-1', [])));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access.chatCredentials).toBeNull();
      expect(access.subscriptions).toHaveLength(1);
      done();
    });
  });

  it('returns an empty result when the listing fails', done => {
    subscriptionService.list.mockReturnValue(throwError(() => new Error('boom')));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access).toEqual({ subscriptions: [], chatCredentials: null });
      done();
    });
  });

  it('asks for every candidate at once rather than one after another', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', []), accepted('sub-2', [])]));
    const pendingGets = new Subject<Subscription>();
    subscriptionService.get.mockReturnValue(pendingGets);

    service.findForAgent('agent-1').subscribe();

    expect(subscriptionService.get).toHaveBeenCalledTimes(2);
    pendingGets.complete();
    done();
  });

  it('keeps the subscription in the list when reading its keys fails', done => {
    subscriptionService.list.mockReturnValue(listing([accepted('sub-1', [])]));
    subscriptionService.get.mockReturnValue(throwError(() => new Error('boom')));

    service.findForAgent('agent-1').subscribe(access => {
      expect(access.subscriptions).toEqual([expect.objectContaining({ subscription: expect.objectContaining({ id: 'sub-1' }) })]);
      expect(access.chatCredentials).toBeNull();
      done();
    });
  });

  describe('loadAccessContexts', () => {
    it('fetches client credentials only for an OAUTH2 plan', done => {
      applicationService.get.mockReturnValue(
        of(fakeApplication({ settings: { oauth: { client_id: 'client-1', client_secret: 'secret-1' } } })),
      );

      const oauth = aSummary({ id: 'sub-oauth', planSecurity: 'OAUTH2' });
      const apiKey = aSummary({ id: 'sub-key', planSecurity: 'API_KEY' });

      service.loadAccessContexts([oauth, apiKey]).subscribe(contexts => {
        expect(applicationService.get).toHaveBeenCalledTimes(1);
        expect(applicationService.get).toHaveBeenCalledWith(oauth.subscription.application);
        expect(contexts.get('sub-oauth')).toEqual({
          clientId: 'client-1',
          clientSecret: 'secret-1',
        });
        expect(contexts.get('sub-key')).toEqual({});
        done();
      });
    });

    it('issues no application call for an empty list', done => {
      service.loadAccessContexts([]).subscribe(contexts => {
        expect(contexts.size).toBe(0);
        expect(applicationService.get).not.toHaveBeenCalled();
        done();
      });
    });
  });
});
