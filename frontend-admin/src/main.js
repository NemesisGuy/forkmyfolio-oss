import {createApp} from 'vue';
import App from './App.vue';
import router from './router';
import './services/initAuth'; // 👈 ensures setAuthService runs before anything else
// Import Bootstrap CSS and JS
import 'bootstrap/dist/css/bootstrap.min.css';
import * as bootstrap from 'bootstrap';
window.bootstrap = bootstrap;

// src/main.js
import '@/assets/common.css';
import '@/assets/glass.css';


// The async logic is now handled by the top-level await in `initAuth.js`.
// We can now create and mount the app directly.
const app = createApp(App);

app.use(router);

app.mount('#app');
