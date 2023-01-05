/**
 * Retry fetch each second until the isSuccessCb is true. throw an error if the isSuccessCb is false after the max retry.
 * @param url fetch url
 * @param options fetch options
 * @param maxRetry Max retry each 1 second
 * @param isSuccessCb Callback to check if the fetch is a success
 * @returns {Promise<Response>}
 */
export const fetchRetry = (url, options = {}, maxRetry, isSuccessCb = (res) => res.ok) =>
  fetch(url, options).then((res) => {
    if (isSuccessCb(res)) {
      return res.json();
    }
    if (maxRetry > 0) {
      // wait 1s before retry
      return new Promise((resolve) => setTimeout(resolve, 1000)).then(() => fetchRetry(url, options, maxRetry - 1, isSuccessCb));
    }
    throw new Error('Fetch retry failed', res);
  });

export const waitWorkflowSuccessPromise = (workflowId) =>
  fetchRetry(
    `https://circleci.com/api/v2/workflow/${workflowId}`,
    {
      method: 'get',
      headers: {
        'Content-Type': 'application/json',
        'Circle-Token': process.env.CIRCLECI_TOKEN,
      },
    },
    1200, // 20 minutes max
    (res) => res.success === true,
  );
