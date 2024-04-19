import 'jest-preset-angular/setup-jest';
import '@angular/localize/init';

// mocking swagger-ui for tests as it contains some JS incompatible with Jest
// This means we will not be able to test Swagger in our components tests
jest.mock('swagger-ui', () => jest.fn(() => ({ initOAuth: () => jest.fn() })));
