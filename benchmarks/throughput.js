import http from 'k6/http'
import { check } from 'k6'

export const options = {
    stages: [
        {duration: '10s', target: 50},
        {duration: '30s', target: 50},
        {duration: '5s', target: 0},
    ],
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
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
        'allowed is true': (r) => r.json('allowed') === true,
    });
}