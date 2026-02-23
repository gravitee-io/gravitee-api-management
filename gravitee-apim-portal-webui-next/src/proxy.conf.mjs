/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

const env = process.env.BACKEND_ENV;
const target = `${env ? `https://${env}` : 'http://localhost:8083'}`;
export default [
  {
    context: ['/portal/ui/bootstrap'],
    target,
    secure: false,
    changeOrigin: true,
    // Vite can access the proxyRes event via configure
    configure: function (proxy) {
      // Replace the full baseURL returned from /ui/bootstrap with just "portal"
      // This bypasses cors security in `serve:apim-master` and cloud env
      //   because the origin (localhost:4101) will match the backend
      proxy.on('proxyRes', (proxyRes, req, res) => {
        let body = Buffer.from('');
        proxyRes.on('data', function (data) {
          body = Buffer.concat([body, data]);
        });
        proxyRes.on('end', function () {
          body = body.toString();
          res.writeHead(proxyRes.statusCode);
          res.end(body.replace(`${target}/`, ''));
        });
      });
    },
    selfHandleResponse: true,
    logLevel: 'debug',
  },
  {
    // Whenever there is a request starting with "portal", then replace target
    context: ['/portal'],
    target,
    secure: false,
    changeOrigin: true,
    configure: function (proxy) {
      proxy.on('proxyReq', (proxyReq, req, res) => {
        proxyReq.setHeader('origin', target.replace('-api', '-portal'));
      });
    },
    logLevel: 'debug',
  },
];
