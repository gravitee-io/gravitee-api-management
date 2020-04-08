const compression = require('compression');
const express = require('express');
const app = express();

app.use(compression());
app.use(express.static('dist/gravitee-portal-webui'));

app.all("/*", function(req, res) {
  res.sendFile('index.html', { root: 'dist/gravitee-portal-webui' });
});

app.listen(3000, function () {
  console.log('Example app listening at http://localhost:3000')
});
