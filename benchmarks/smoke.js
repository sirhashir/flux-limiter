import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 5,
    duration: '5s',
};

export default function () {
    const res = http.post('http://nginx/api/check',
        JSON.stringify({ tenantId: 'bench', key: 'endpoint-1' }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, {
        'status is 200': (r) => r.status === 200,
        'allowed is true': (r) => r.json('allowed') === true,
    });
}