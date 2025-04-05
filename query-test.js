import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        galaxy_event_load_test: {
            executor: 'constant-arrival-rate',  // 일정한 요청 비율 유지
            rate: 5000,                        // 초당 요청 수 (로컬에서 가능한 만큼)
            timeUnit: '1s',                    // 1초 단위
            duration: '60s',                   // 1분간 테스트
            preAllocatedVUs: 100,              // 미리 할당된 가상 사용자
            maxVUs: 2000,                      // 최대 가상 사용자
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<300', 'p(99)<500'],
        'http_req_failed': ['rate<0.01'],
        'checks': ['rate>0.99'],
    },
    discardResponseBodies: true,
    noConnectionReuse: false,
};

export default function () {
    // ID가 1인 제품만 테스트
    const res = http.get('http://localhost:8080/stock/1');

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 300ms': (r) => r.timings.duration < 300,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });
}