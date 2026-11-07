import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://forkmyfolio-app:8080/api/v1';

export const options = {
    scenarios: {
        profile_updates: {
            executor: 'ramping-arrival-rate',
            startRate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 10,
            maxVUs: 50,
            stages: [
                { target: 10, duration: '1m' }, // Ramp to 10 updates per second
                { target: 20, duration: '2m' }, // Ramp to 20 updates per second (Moderate write load)
                { target: 0, duration: '30s' }, // Cooldown
            ],
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<1500'], // Writes are slower, allow 1.5s
        'http_req_failed': ['rate<0.01'],    // Goal: < 1% error rate
    },
};

// Test Data
const headlines = [
    "Full-Stack Java Ninja",
    "Spring Boot Specialist",
    "Cloud Architect",
    "Performance Engineering Expert",
    "DevOps Guru",
];

const summaries = [
    "Experienced developer with 10+ years in the industry.",
    "Passionate about building scalable distributed systems.",
    "Loves high-performance Java and modern JavaScript frameworks.",
    "Focused on delivering business value through clean code.",
];

export default function () {
    // 1. Login (Once per VU session ideally, but for this stress test we can login more frequently if needed)
    // To minimize login overhead, we'll login once and reuse the token if we were using a different executor, 
    // but here each iteration does the full flow to test the Auth stack too.
    
    const loginPayload = JSON.stringify({
        email: 'admin@forkmyfolio.com',
        password: __ENV.ADMIN_PASSWORD || ''
    });

    const loginParams = { headers: { 'Content-Type': 'application/json' } };
    const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, loginParams);

    if (loginRes.status !== 200) {
        return;
    }

    const token = loginRes.json('data.accessToken');
    const authHeaders = { 
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };

    // 2. Perform Profile Update
    const randomHeadline = headlines[Math.floor(Math.random() * headlines.length)];
    const randomSummary = summaries[Math.floor(Math.random() * summaries.length)];

    const updatePayload = JSON.stringify({
        headline: randomHeadline,
        summary: randomSummary,
        location: "Remote Area " + Math.floor(Math.random() * 100),
        isPublic: true
    });

    const updateRes = http.put(`${BASE_URL}/me/profile`, updatePayload, { headers: authHeaders });

    check(updateRes, {
        'profile update is 200': (r) => r.status === 200,
        'headline updated': (r) => r.json('data.headline') === randomHeadline,
    });

    sleep(0.5); 
}
