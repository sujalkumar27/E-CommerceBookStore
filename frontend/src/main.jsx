// main.jsx — Entry point for the React app.
// ReactDOM.createRoot() renders the root <App /> component into the #root div
// defined in index.html.  StrictMode runs extra checks in development only
// (double-invokes effects to catch bugs); it has no effect in production.

import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
