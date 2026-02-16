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
import { TestBed } from '@angular/core/testing';

import { CockpitService, UtmCampaign } from './cockpit.service';

describe('CockpitService', () => {
  let cockpitService: CockpitService;
  const COCKPIT_URL = 'https://fake.cockpit.url';

  beforeEach(() => {
    cockpitService = TestBed.inject<CockpitService>(CockpitService);
  });

  it('should configure URL with API_PROMOTION campaign with exsiting query params', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(`${COCKPIT_URL}?foo=bar`, UtmCampaign.API_PROMOTION);
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?foo=bar&utm_source=apim&utm_medium=InApp&utm_campaign=api_promotion`);
    done();
  });

  it('should configure URL with API_PROMOTION campaign', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.API_PROMOTION);
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=api_promotion`);
    done();
  });

  it('should configure URL with API_DESIGNER campaign and ACCEPTED installation status', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.API_DESIGNER, 'ACCEPTED');
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=api_designer&utm_term=registered`);
    done();
  });

  it('should configure URL with API_DESIGNER campaign and no installation status', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.API_DESIGNER);
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=api_designer&utm_term=not_registered`);
    done();
  });

  it('should configure URL with API_DESIGNER campaign and not ACCEPTED installation status', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.API_DESIGNER, 'unknown_status');
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=api_designer&utm_term=not_registered`);
    done();
  });

  it('should configure URL with DiscoverCockpit campaign and PENDING installation status', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.DISCOVER_COCKPIT, 'PENDING');
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=pending`);
    done();
  });

  it('should configure URL with DiscoverCockpit campaign and ACCEPTED installation status', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.DISCOVER_COCKPIT, 'ACCEPTED');
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=registered`);
    done();
  });

  it('should configure URL with DiscoverCockpit campaign and REJECTED installation status', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.DISCOVER_COCKPIT, 'REJECTED');
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=rejected`);
    done();
  });

  it('should configure URL with DiscoverCockpit campaign and DELETED installation status', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.DISCOVER_COCKPIT, 'DELETED');
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=removed`);
    done();
  });

  it('should configure URL with DiscoverCockpit campaign and no installation status', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.DISCOVER_COCKPIT);
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=not_registered`);
    done();
  });

  it('should configure URL with DiscoverCockpit campaign and unknown installation status', done => {
    const enhancedURL = cockpitService.addQueryParamsForAnalytics(COCKPIT_URL, UtmCampaign.DISCOVER_COCKPIT, 'UNKNOWN');
    expect(enhancedURL).toEqual(`${COCKPIT_URL}?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=not_registered`);
    done();
  });
});
