import { and, equal, When, Workflow } from '../../sdk/index.mjs';

export function createPullRequestWorkflow(jobs) {
  return new Workflow(
    'pull_requests',
    jobs,
    new When(and(equal('pull_requests', '<< pipeline.parameters.gio_action >>'))),
  );
}
