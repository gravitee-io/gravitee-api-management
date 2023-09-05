import { computeImagesTag } from '../tags';

describe('tags', () => {
  it('should compute docker images tag', () => {
    expect(computeImagesTag('apim-1234-toto')).toStrictEqual('apim-1234-toto-latest');
  });
});
