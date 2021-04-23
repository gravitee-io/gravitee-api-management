const compression = require('compression');
const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const app = express();

app.use(compression());
app.use(express.static('dist/'));
app.use('/portal', createProxyMiddleware({ target: 'http://localhost:8083', changeOrigin: true }));

app.all('/*', function (req, res) {
  res.sendFile('index.html', { root: 'dist/' });
});

app.listen(4100, function () {
  console.log('Example app listening at http://localhost:4100');
});
