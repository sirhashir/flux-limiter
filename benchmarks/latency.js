import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(50)<2', 'p(95)<5', 'p(99)<10'],
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