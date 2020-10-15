const compression = require('compression');
const express = require('express');
const app = express();

app.use(compression());
app.use(express.static('dist'));

app.all("/*", function(req, res) {
  res.sendFile('index.html', { root: 'dist' });
});

app.listen(4100, function () {
  console.log('Example app listening at http://localhost:4100')
});
