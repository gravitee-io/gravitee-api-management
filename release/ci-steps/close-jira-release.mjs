import { syncProcessCwd } from 'zx';
import { extractVersion } from '../helpers/version-helper.mjs';
import { isDryRun } from '../helpers/option-helper.mjs';
import { closeJiraRelease } from '../helpers/jira-helper.mjs';

syncProcessCwd(); // restores legacy v7 behavior

console.log(chalk.magenta(`##################################`));
console.log(chalk.magenta(`# 🏷️  Close the release in Jira  #`));
console.log(chalk.magenta(`##################################`));

const releasingVersion = await extractVersion();
const dryRun = isDryRun();
const today = new Date().toISOString().slice(0, 10);

const { skipped, unclosed, error } = await closeJiraRelease({ version: releasingVersion, today, dryRun });
if (skipped) {
  process.exit(0);
}

await reportUnclosed(unclosed);

if (error) {
  // Raised only now, so the report above is out whatever Jira refused.
  echo(chalk.red(`❌ Could not update the Jira versions: ${error.message}`));
  process.exit(1);
}

async function reportUnclosed(unclosed) {
  if (unclosed.length === 0) {
    echo(chalk.green(`Every ticket of ${releasingVersion} is closed.`));
    return;
  }

  echo(chalk.yellow(`⚠️  ${unclosed.length} ticket(s) of ${releasingVersion} shipped without being closed:`));
  unclosed.forEach((issue) => echo(`  ${issue.key}  [${issue.type} · ${issue.status}]  ${issue.summary}`));

  const channel = process.env.SLACK_CHANNEL;
  if (dryRun || !process.env.SLACK_ACCESS_TOKEN || !channel) {
    echo(chalk.blue(`Not posting to Slack${dryRun ? ' in a dry run' : ': SLACK_ACCESS_TOKEN or SLACK_CHANNEL is unset'}.`));
    return;
  }

  const lines = unclosed.map(
    (issue) => `• <https://gravitee.atlassian.net/browse/${issue.key}|${issue.key}> · ${issue.type} · *${issue.status}* — ${issue.summary}`,
  );
  // The list is in the log above, so a failed post — refused, unreachable or garbled — must not hide it behind a red job.
  try {
    const response = await fetch('https://slack.com/api/chat.postMessage', {
      method: 'POST',
      headers: { Authorization: `Bearer ${process.env.SLACK_ACCESS_TOKEN}`, 'Content-Type': 'application/json; charset=utf-8' },
      body: JSON.stringify({
        channel,
        text: `:warning: APIM ${releasingVersion} shipped ${unclosed.length} ticket(s) that are not closed in Jira:\n${lines.join('\n')}`,
        unfurl_links: false,
      }),
    }).then((answer) => answer.json());
    if (!response.ok) {
      echo(chalk.yellow(`⚠️  Could not post to Slack: ${response.error}`));
    }
  } catch (postError) {
    echo(chalk.yellow(`⚠️  Could not post to Slack: ${postError.message}`));
  }
}
