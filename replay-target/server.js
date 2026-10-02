const http = require('http');

const server = http.createServer((req, res) => {
  if (req.url === '/api/test-target/health' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'UP' }));
  } else if (req.url === '/api/test-target/echo' && req.method === 'GET') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ message: 'Echo GET successful' }));
  } else if (req.url === '/api/test-target/delay' && req.method === 'GET') {
    setTimeout(() => {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ message: 'Delayed response' }));
    }, 2000);
  } else if (req.url === '/api/test-target/echo' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => {
      body += chunk.toString();
    });
    req.on('end', () => {
      res.writeHead(200, { 'Content-Type': req.headers['content-type'] || 'application/json' });
      res.end(body);
    });
  } else {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not Found' }));
  }
});

const PORT = 8080;
server.listen(PORT, () => {
  console.log(`Replay target listening on port ${PORT}`);
});
