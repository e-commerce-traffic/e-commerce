import http from 'k6/http';
import { sleep,check } from 'k6';

export const options = {
    scenarios: {
        ramp_up_test: {
            executor: 'ramping-arrival-rate',
            startRate: 500,                    // 초기 요청률
            timeUnit: '1s',       // 1초 단위로 설정
            // duration: '30s',      // 테스트 기간
            preAllocatedVUs: 100, // 사전 할당할 가상 사용자 수
            maxVUs: 3000,        // 최대 가상 사용자 수
            stages: [                          // 단계별 설정
                { duration: '10s', target: 500 },
                { duration: '20s', target: 1000 },
                { duration: '30s', target: 2000 },
                { duration: '30s', target: 5000 },
                { duration: '30s', target: 10000 },
                { duration: '30s', target: 20000 },
                { duration: '30s', target: 30000 },
                { duration: '30s', target: 0 }
            ]
        },
    },
    discardResponseBodies: true, // 응답 본문 무시하여 메모리 사용량 감소
    thresholds: {
        'http_req_duration': ['p(95)<1000'],
        'http_req_failed': ['rate<0.05'],
        'checks': ['rate>0.95'],
    },
    noConnectionReuse: false,    // 연결 재사용 (성능 향상)
};

export default function () {
    const res = http.get('http://localhost:8080/stock/1');

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    })

    // sleep 제거 - 1M TPS에서는 sleep을 사용하지 않음
}