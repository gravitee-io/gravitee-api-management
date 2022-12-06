import { computeVersion, extractVersion } from '../helpers/version-helper.mjs';

console.log(chalk.magenta(`#############################################`));
console.log(chalk.magenta(`# 📰 Open APIM docs PR for new Release Note #`));
console.log(chalk.magenta(`#############################################`));

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);

const docRepository = 'https://github.com/gravitee-io/gravitee-docs';
const docApimChangelogFolder = 'pages/apim/3.x/changelog/';
const docApimChangelogFile = `${docApimChangelogFolder}changelog-${versions.trimmed}.adoc`;

const gitBranch = `release-apim-${releasingVersion}`;

echo(chalk.blue(`# Get feat & fix commits`));
const allFeatCommits = await $`git log $(git describe --tags --abbrev=0)..HEAD --no-merges --oneline --grep "^feat\\|^perf"`;
const allFixCommits = await $`git log $(git describe --tags --abbrev=0)..HEAD --no-merges --oneline --grep "^fix"`;

echo(chalk.blue(`# Clone gravitee-docs repository`));
await $`mkdir -p changelog`;
cd('changelog');
await $`rm -rf gravitee-docs`;
await $`git clone --depth 1  ${docRepository} --single-branch --branch=master`;
cd('gravitee-docs');

echo(chalk.blue(`# Write changelog to ${docApimChangelogFile}`));
await $`mkdir -p ${docApimChangelogFolder}`;

if (!fs.existsSync(docApimChangelogFile)) {
  echo(chalk.blue(`# Init new changelog file`));

  const apimSidebarFile = '_data/sidebars/apim_3_x_sidebar.yml';

  const apimSidebarDoc = YAML.parseDocument(fs.readFileSync(apimSidebarFile, 'utf8'));
  const apimSidebarFolders = apimSidebarDoc.get('entries').get(0).get('folders');

  const changelogIndex = Object.entries(apimSidebarFolders.items).findIndex(([key, value]) => value.get('title') === 'Changelog');

  let changelogItems = YAML.isCollection(apimSidebarFolders.get(changelogIndex).get('folderitems'))
    ? apimSidebarFolders.get(changelogIndex).get('folderitems')
    : new YAML.YAMLSeq();

  changelogItems.add({
    title: `Changelog ${versions.trimmed}`,
    output: `web`,
    url: `/apim/3.x/changelog-${versions.trimmed}.html`,
  });
  apimSidebarFolders.get(changelogIndex).set('folderitems', changelogItems);

  fs.writeFileSync(apimSidebarFile, apimSidebarDoc.toString());
  await $`git add ./${apimSidebarFile}`;
  fs.appendFileSync(
    `${docApimChangelogFile}`,
    `:page-sidebar: apim_3_x_sidebar
:page-permalink: apim/3.x/changelog-${versions.trimmed}.html
:page-folder: apim
:page-toc: false
:page-layout: apim3x

= APIM ${versions.trimmed} changelog

For upgrade instructions, please refer to https://docs.gravitee.io/apim/3.x/apim_installguide_migration.html[APIM Migration Guide]

*Important:* If you plan to skip versions when you upgrade, ensure that you read the version-specific upgrade notes for each intermediate version. You may be required to perform manual actions as part of the upgrade.

// NOTE: Global ${versions.trimmed} release info here

// <DO NOT REMOVE THIS COMMENT - ANCHOR FOR FUTURE RELEASES>
`,
  );
}

let changelogPatchTemplate = `
== APIM - ${releasingVersion} (${new Date().toISOString().slice(0, 10)})

=== Gateway

// TODO: List all Bug fixes & Improvements

=== API

// TODO: List all Bug fixes & Improvements

=== Console

// TODO: List all Bug fixes & Improvements

=== Portal

// TODO: List all Bug fixes & Improvements
`;
echo(changelogPatchTemplate);

// write after anchor
const changelogFileContent = fs.readFileSync(docApimChangelogFile, 'utf8');
const changelogFileContentWithPatch = changelogFileContent.replace(
  '// <DO NOT REMOVE THIS COMMENT - ANCHOR FOR FUTURE RELEASES>',
  `$&
 ${changelogPatchTemplate}`,
);
fs.writeFileSync(`${docApimChangelogFile}`, changelogFileContentWithPatch);

echo(chalk.blue(`# Commit and push changelog to ${gitBranch}`));
try {
  await $`gh pr close --delete-branch ${gitBranch}`;
} catch (e) {
  // Best effort to have no open PR before creating a new one
}
await $`git checkout -b ${gitBranch}`;
await $`git add ./${docApimChangelogFile}`;
await $`git commit -m "chore: add changelog for ${releasingVersion}"`;
await $`git push --set-upstream origin ${gitBranch}`;

const prBody = `
# New APIM version ${releasingVersion} has been released
📝 You can modify the changelog template online [here](https://github.com/gravitee-io/gravitee-docs/edit/${gitBranch}/${docApimChangelogFile})

Here is some information to help with the writing: 

## Commit messages 
<details>
  <summary>See all feats commit</summary>

${allFeatCommits.stdout}

</details>

<details>
  <summary>See all fixs commit</summary>

${allFixCommits.stdout}
</details>

## Jira issues

[See all Jira issues for ${versions.branch} version](https://gravitee.atlassian.net/jira/software/c/projects/APIM/issues/?jql=project%20%3D%20%22APIM%22%20and%20fixVersion%20%3D%20${versions.branch}%20and%20status%20%3D%20Done%20ORDER%20BY%20created%20DESC)
`;
echo(chalk.blue('# Create PR on Github Doc repository'));
echo(prBody);
process.env.PR_BODY = prBody;

const releaseNotesPrUrl = await $`gh pr create --title "[APIM] Add changelog for new ${releasingVersion} release" --body "$PR_BODY" --base master --head ${gitBranch}`;
$`echo ${releaseNotesPrUrl.stdout} > /tmp/releaseNotesPrUrl.txt`;