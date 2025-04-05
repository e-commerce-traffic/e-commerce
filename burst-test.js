import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        spike: {
            executor: 'ramping-arrival-rate',
            preAllocatedVUs: 10000,
            maxVUs: 50000,
            stages: [
                { duration: '30s', target: 1000 },    // 워밍업: 중간 수준의 부하
                { duration: '10s', target: 1000000 }, // 스파이크: 갑자기 1M TPS로 증가
                { duration: '30s', target: 1000000 }, // 스파이크 유지
                { duration: '30s', target: 1000 },    // 정상화: 다시 일반 부하로 감소
            ],
        },
    },
    discardResponseBodies: true,
    noConnectionReuse: false,
    thresholds: {
        'http_req_duration': ['p(95)<2000'], // 95% 요청이 2초 이내 응답
        'http_req_failed': ['rate<0.05'],    // 에러율 5% 미만
        'checks': ['rate>0.9'],              // 90% 이상의 체크 성공
    },
};

export default function () {
    const start = new Date();
    const res = http.get('http://localhost:8080/stock/1');
    const end = new Date();
    const responseTime = end - start;

    // 응답 확인
    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
        'response time < 1s': (r) => r.timings.duration < 1000,
        'response time < 2s': (r) => r.timings.duration < 2000,
        'content is valid': (r) => r.status === 200 || r.body.includes('stock'),
    });

    // 응답 시간 구간별 로깅 (사용자 지정 메트릭)
    if (responseTime < 100) {
        console.log('Very fast response: ' + responseTime + 'ms');
    } else if (responseTime > 2000) {
        console.log('Very slow response: ' + responseTime + 'ms');
    }
}