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
export const k6Options = {
  insecureSkipTLSVerify: __ENV.SKIP_TLS_VERIFY === 'true',
  scenarios: {
    default: {
      executor: 'ramping-arrival-rate',

      // Our test with at a rate of 10 iterations started per second.
      startRate: 10,

      // It should start `startRate` iterations per minute
      timeUnit: '1s',

      // It should preallocate 2 VUs before starting the test.
      preAllocatedVUs: 2,

      // It is allowed to spin up to 50 maximum VUs in order to sustain the defined constant arrival rate.
      maxVUs: 50,

      stages: [
        // It should start 300 iterations per second for the first 5 minutes.
        { target: 300, duration: '5m' },

        // It should stay at 300 iterations per second during 2 minutes.
        { target: 300, duration: '2m' },

        // It should linearly ramp-up to 800 iterations per second over the following 5 minutes.
        { target: 800, duration: '5m' },

        // It should linearly ramp-up to 1000 iterations per second for the following 2 minutes.
        { target: 1000, duration: '2m' },

        // It should stay to 1000 iterations per second over the following 5 minutes.
        { target: 1000, duration: '5m' },

        // It should linearly ramp-down to 50 iterations per second over the last 3 minutes.
        { target: 50, duration: '3m' },
      ],
    },
  },
};
