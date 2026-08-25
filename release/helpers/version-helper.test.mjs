import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { versionFromPom } from './version-helper.mjs';

const pom = (properties) => `<project>
  <artifactId>gravitee-api-management</artifactId>
  <version>\${revision}\${sha1}\${changelist}</version>
  <properties>
${properties}
  </properties>
</project>`;

describe('versionFromPom', () => {
  it('reads a final release, whose qualifier element is self-closing', () => {
    const xml = pom('    <revision>4.12.18</revision>\n    <sha1 />\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(xml), '4.12.18');
  });

  it('reads a final release whose qualifier element is empty', () => {
    const xml = pom('    <revision>4.12.18</revision>\n    <sha1></sha1>\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(xml), '4.12.18');
  });

  it('reads a pre-release', () => {
    const xml = pom('    <revision>4.13.0</revision>\n    <sha1>-alpha.1</sha1>\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(xml), '4.13.0-alpha.1');
  });

  it('reads a hotfix', () => {
    const xml = pom('    <revision>4.12.17</revision>\n    <sha1>-hotfix.2</sha1>\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(xml), '4.12.17-hotfix.2');
  });

  it('ignores the changelist, which the release clears', () => {
    const released = pom('    <revision>4.12.18</revision>\n    <sha1 />\n    <changelist />');
    const snapshot = pom('    <revision>4.12.18</revision>\n    <sha1 />\n    <changelist>-SNAPSHOT</changelist>');
    assert.equal(versionFromPom(released), versionFromPom(snapshot));
  });

  it('refuses a pom that carries no revision, rather than guessing', () => {
    assert.throws(() => versionFromPom('<project><version>4.12.18</version></project>'), /revision/);
  });
});
