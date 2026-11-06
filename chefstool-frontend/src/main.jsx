// Vendors
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

// Utils
import App from './App.jsx'

// Hooks
import { TemaProvider } from '@/lib/tema'

// Styles
import './index.css'

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {})
  })
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <TemaProvider>
        <App />
      </TemaProvider>
    </BrowserRouter>
  </StrictMode>,
)
