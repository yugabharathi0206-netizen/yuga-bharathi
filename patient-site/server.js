/**
 * Dr. Balaji Homeo Care - Patient Portal Web Server
 * Compatible with local Node.js execution AND Vercel Serverless Function
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = __dirname;

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8',
  '.zip': 'application/zip'
};

function requestHandler(req, res) {
  let safePath = path.normalize((req.url || '/').split('?')[0]);
  if (safePath === '/' || safePath === '' || safePath === '.') {
    safePath = '/index.html';
  }

  let filePath = path.join(PUBLIC_DIR, safePath);

  fs.stat(filePath, (err, stats) => {
    // If not a static file or doesn't exist, fallback to index.html for SPA routes
    if (err || !stats.isFile()) {
      filePath = path.join(PUBLIC_DIR, 'index.html');
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    fs.readFile(filePath, (readErr, content) => {
      if (readErr) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('500 Internal Server Error');
      } else {
        const headers = {
          'Content-Type': contentType,
          'Cache-Control': ext === '.html' ? 'no-cache' : 'public, max-age=3600',
          'Access-Control-Allow-Origin': '*'
        };
        if (ext === '.zip') {
          headers['Content-Disposition'] = `attachment; filename="${path.basename(filePath)}"`;
        }
        res.writeHead(200, headers);
        res.end(content);
      }
    });
  });
}

// Export request handler for Vercel / serverless environments
module.exports = requestHandler;

// If started directly via Node.js (e.g. node server.js)
if (require.main === module) {
  const server = http.createServer(requestHandler);
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`✓ Dr. Balaji Homeo Care Patient Website running on http://0.0.0.0:${PORT}`);
  });
}
