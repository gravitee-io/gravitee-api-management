import { computeApimVersion, parse } from '../versions';

describe('version', function () {
  describe('parse', function () {
    it.each`
      versionToParse     | versionFull | major  | minor  | patch  | qualifierFull | qualifierName | qualifierVersion
      ${'1.2.3-alpha.4'} | ${'1.2.3'}  | ${'1'} | ${'2'} | ${'3'} | ${'alpha.4'}  | ${'alpha'}    | ${'4'}
      ${'1.2.3'}         | ${'1.2.3'}  | ${'1'} | ${'2'} | ${'3'} | ${''}         | ${''}         | ${''}
    `(
      'returns parsed version $versionToParse',
      ({ versionToParse, versionFull, major, minor, patch, qualifierFull, qualifierName, qualifierVersion }) => {
        expect(parse(versionToParse)).toEqual({
          version: {
            full: versionFull,
            major,
            minor,
            patch,
          },
          qualifier: {
            full: qualifierFull,
            name: qualifierName,
            version: qualifierVersion,
          },
        });
      },
    );
  });

  describe('computeApimVersion', () => {
    it.each`
      path                                                   | expected
      ${'./src/utils/tests/resources/pom.xml'}               | ${'4.2.0'}
      ${'./src/utils/tests/resources/pom-sha1-snapshot.xml'} | ${'4.2.0-alpha.1-SNAPSHOT'}
      ${'./src/utils/tests/resources/pom-snapshot.xml'}      | ${'4.2.0-SNAPSHOT'}
    `('should parse apim version', ({ path, expected }) => {
      expect(computeApimVersion(path)).toStrictEqual(expected);
    });
  });
});
