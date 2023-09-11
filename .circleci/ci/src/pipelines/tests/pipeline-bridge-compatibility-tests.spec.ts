import * as fs from 'fs';
import { generateBridgeCompatibilityTestsConfig } from '../pipeline-bridge-compatibility-tests';

describe('Run bridge compatibility tests', () => {
  it('should generate bridge compatibility tests pipeline', () => {
    const result = generateBridgeCompatibilityTestsConfig({
      action: 'bridge_compatibility_tests',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      isDryRun: false,
      apimVersionPath: '',
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/bridge-compatibility-tests/bridge-compatibility-tests.yml`, 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });
});
