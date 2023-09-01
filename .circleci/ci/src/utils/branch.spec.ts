import { isMasterBranch, isSupportBranch, sanitizeBranch } from './branch';

describe('branch', function () {
  describe('sanitize', function () {
    it.each`
      branchToSanitize                                                                                                  | sanitizedBranch
      ${'APIM-1234-my-custom-branch'}                                                                                   | ${'apim-1234-my-custom-branch'}
      ${'feat/my#new!version~wip'}                                                                                      | ${'feat-my-new-versionwip'}
      ${'--a-very-long-branch-name-with-more-than-sixty-characters-because-the-sanitization-keeps-only-60-charaters--'} | ${'a-very-long-branch-name-with-more-than-sixty-characters-beca'}
    `('returns sanitized version of $branchToSanitize', ({ branchToSanitize, sanitizedBranch }) => {
      expect(sanitizeBranch(branchToSanitize)).toEqual(sanitizedBranch);
    });
  });

  describe('isMaster', function () {
    it.each`
      branch          | expected
      ${'not-master'} | ${false}
      ${'master'}     | ${true}
    `('returns $expected is $branch is master', ({ branch, expected }) => {
      expect(isMasterBranch(branch)).toEqual(expected);
    });
  });

  describe('isSupport', function () {
    it.each`
      branch                         | expected
      ${'APIM-1234-mycustom-branch'} | ${false}
      ${'master'}                    | ${false}
      ${'1.2.3'}                     | ${false}
      ${'1.2.x'}                     | ${true}
      ${'1.x'}                       | ${false}
      ${'x'}                         | ${false}
    `('returns $expected is $branch is support', ({ branch, expected }) => {
      expect(isSupportBranch(branch)).toEqual(expected);
    });
  });
});
