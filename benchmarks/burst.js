import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        burst: {
            executor: 'per-vu-iterations',
            vus: 1000,
            iterations: 1,
            maxDuration: '30s',
        },
    },
};

const payload = JSON.stringify({
    tenantId: 'bench',
    key: 'endpoint-1',
});

const params = {
    headers: {
        'Content-Type': 'application/json',
    },
};

export default function () {
    const res = http.post('http://nginx/api/check', payload, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}