import '@angular/localize/init';
import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

setupZoneTestEnv();

// mocking swagger-ui for tests as it contains some JS incompatible with Jest
// This means we will not be able to test Swagger in our components tests
jest.mock('swagger-ui', () => jest.fn(() => ({ initOAuth: () => jest.fn() })));

// Set the mock date globally for all tests
const MOCK_DATE = new Date(1466424490000); // UTC Time: Mon Jun 20 2016 12:08:10.000

// Need to define advanceTimers for async calls
jest.useFakeTimers({ advanceTimers: 1, now: MOCK_DATE }); // advance 1ms every 10ms

// Mock Date.now() so that it always returns the same date
Date.now = jest.fn(() => MOCK_DATE.getTime());
