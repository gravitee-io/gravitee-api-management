import 'jest-preset-angular/setup-jest';
import '@angular/localize/init';

// mocking swagger-ui for tests as it contains some JS incompatible with Jest
// This means we will not be able to test Swagger in our components tests
jest.mock('swagger-ui', () => jest.fn(() => ({ initOAuth: () => jest.fn() })));

// Set the mock date globally for all tests
const MOCK_DATE = new Date(1466424490000); // UTC Time: Mon Jun 20 2016 12:08:10.000
jest.useFakeTimers({ advanceTimers: true }).setSystemTime(MOCK_DATE);
