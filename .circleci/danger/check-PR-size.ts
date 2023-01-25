import { danger, warn, markdown } from "danger";


const bigPRThreshold = 10;

export function checkPRSize() {
    const prSize = danger.github.pr.additions + danger.github.pr.deletions;
    if (prSize > bigPRThreshold) {
        warn(`PR size is ${prSize} lines. Consider splitting it into smaller PRs.`);
      markdown(
        "> Pull Request size seems relatively large. If Pull Request contains multiple changes, split each into separate PR will helps faster, easier review."
      );
    }
}
