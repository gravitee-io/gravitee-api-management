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
const compression = require('compression');
const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const app = express();

app.use(compression());
app.use(express.static('dist/'));
app.use('/management', createProxyMiddleware({ target: 'http://localhost:8083', changeOrigin: true }));
// If you want to try webapp with an other target
// app.use('/management', createProxyMiddleware({ target: 'https://apim-master-api.cloud.gravitee.io', changeOrigin: true, secure: false }));
app.all('/*', (req, res) => {
  res.sendFile('index.html', { root: 'dist/' });
});

app.listen(3000, () => {
  console.log('Example app listening at http://localhost:3000');
});
