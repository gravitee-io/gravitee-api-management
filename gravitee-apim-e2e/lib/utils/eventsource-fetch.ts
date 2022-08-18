/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 This component is highly inspired from https://github.com/Azure/fetch-event-source
 */

import { EventSourceMessage, getBytes, getLines, getMessages } from './eventsource-parse';
import fetchApi from 'node-fetch';
import { sleep } from './gateway';

export const EventStreamContentType = 'text/event-stream';

const LastEventId = 'last-event-id';

export interface FetchEventSourceInit extends RequestInit {
  /**
   * The request headers. FetchEventSource only supports the Record<string,string> format.
   */
  headers?: Record<string, string>;

  /**
   * Called when a response is received. Use this to validate that the response
   * actually matches what you expect (and throw if it doesn't.) If not provided,
   * will default to a basic validation to ensure the content-type is text/event-stream.
   */
  onopen?: (response: Response) => Promise<void>;

  /**
   * Called when a message is received. NOTE: Unlike the default browser
   * EventSource.onmessage, this callback is called for _all_ events,
   * even ones with a custom `event` field.
   */
  onmessage?: (ev: EventSourceMessage) => void;

  /**
   * Called when a response finishes. If you don't expect the server to kill
   * the connection, you can throw an exception here and retry using onerror.
   */
  onclose?: () => void;

  /**
   * Called when there is any error making the request / processing messages /
   * handling callbacks etc. Use this to control the retry strategy: if the
   * error is fatal, rethrow the error inside the callback to stop the entire
   * operation. Otherwise, you can return an interval (in milliseconds) after
   * which the request will automatically retry (with the last-event-id).
   * If this callback is not specified, or it returns undefined, fetchEventSource
   * will treat every error as retriable and will try again after 1 second.
   */
  onerror?: (err: any) => number | null | undefined | void;

  /**
   * If true, will keep the request open even if the document is hidden.
   * By default, fetchEventSource will close the request and reopen it
   * automatically when the document becomes visible again.
   */
  openWhenHidden?: boolean;

  /** The Fetch function to use. Defaults to window.fetch */
  fetch?: typeof fetch;

  timeBetweenRetries: number;

  maxRetries: number;
}

export function fetchEventSource(
  url: string,
  { headers: inputHeaders, onmessage, onclose, onerror, timeBetweenRetries, maxRetries }: FetchEventSourceInit,
) {
  return new Promise<void>((resolve, reject) => {
    // make a copy of the input headers since we may modify it below:
    const headers = { ...inputHeaders };
    let retries = maxRetries;
    if (!headers.accept) {
      headers.accept = EventStreamContentType;
    }

    let curRequestController: AbortController;

    const onopen = defaultOnOpen;

    async function create() {
      curRequestController = new AbortController();
      try {
        const response = await fetchApi(url, {
          headers,
        });

        await onopen(response);

        const messages = await getBytes(
          response.body!,
          getLines(
            getMessages(
              (id) => {
                if (id) {
                  // store the id and send it back on the next retry:
                  headers[LastEventId] = id;
                } else {
                  // don't send the last-event-id header anymore:
                  delete headers[LastEventId];
                }
              },
              () => {},
              onmessage,
            ),
          ),
        );

        onclose?.();
        // @ts-ignore
        messages.forEach((data) => onmessage({ data }));
        resolve();
      } catch (err) {
        if (!curRequestController.signal.aborted) {
          // if we haven't aborted the request ourselves:
          try {
            // check if we need to retry:
            if (retries-- > 0) {
              await sleep(timeBetweenRetries);
              create();
            }
          } catch (innerErr) {
            // we should not retry anymore:
            reject(innerErr);
          }
        }
      }
    }

    create();
  });
}

function defaultOnOpen(response: any) {
  const contentType = response.headers.get('content-type');
  if (!contentType?.startsWith(EventStreamContentType)) {
    throw new Error(`Expected content-type to be ${EventStreamContentType}, Actual: ${contentType}`);
  }
}
