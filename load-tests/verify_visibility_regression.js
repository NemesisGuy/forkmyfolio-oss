import http from 'k6/http';
import { check, sleep } from 'k6';

// This script verifies the visibility toggle and cache eviction flow.
// It requires an admin user with email 'admin@forkmyfolio.com' and the admin
// password supplied via the ADMIN_PASSWORD environment variable.

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8099/api/v1';

export const options = {
    vus: 1,
    iterations: 1,
};

export default function () {
    // 1. Login
    console.log('Logging in...');
    const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        email: 'admin@forkmyfolio.com',
        password: __ENV.ADMIN_PASSWORD || ''
    }), { headers: { 'Content-Type': 'application/json' } });

    if (loginRes.status !== 200) {
        console.error('Login failed! status: ' + loginRes.status + ' body: ' + loginRes.body);
        return;
    }
    const token = loginRes.json('data.accessToken');
    const authHeaders = { 
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };

    // 2. Set Visibility to PRIVATE (false)
    console.log('Setting portfolio to PRIVATE...');
    const privRes = http.put(`${BASE_URL}/me/profile/visibility`, JSON.stringify({
        isPublic: false
    }), { headers: authHeaders });
    
    check(privRes, { 'Set Private Success (200)': (r) => r.status === 200 });
    console.log('Private status response: ' + privRes.body);

    // 3. Verify Public Access (Should be 403)
    console.log('Verifying public access (expecting 403 Content)...');
    const publicPrivRes = http.get(`${BASE_URL}/portfolios/admin-user`);
    check(publicPrivRes, { 'Public Access Denied (403)': (r) => r.status === 403 });

    // 4. Set Visibility to PUBLIC (true)
    console.log('Setting portfolio to PUBLIC...');
    const pubRes = http.put(`${BASE_URL}/me/profile/visibility`, JSON.stringify({
        isPublic: true
    }), { headers: authHeaders });

    check(pubRes, { 'Set Public Success (200)': (r) => r.status === 200 });
    console.log('Public status response: ' + pubRes.body);

    // 5. Verify Public Access (Should be 200) - THIS TESTS CACHE EVICTION
    console.log('Verifying public access (expecting 200 Content)...');
    const publicPubRes = http.get(`${BASE_URL}/portfolios/admin-user`);
    check(publicPubRes, { 'Public Access Granted (200)': (r) => r.status === 200 });

    if (publicPubRes.status !== 200) {
        console.error('REGRESSION DETECTED: Could not access portfolio after setting to public!');
    } else {
        console.log('SUCCESS: Visibility toggle and cache eviction working correctly.');
    }
}
