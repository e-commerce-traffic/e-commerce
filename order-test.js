import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    scenarios: {
        order_ramp_test: {
            executor: 'ramping-arrival-rate',
            startRate: 100,            // 초기 요청률
            timeUnit: '1s',            // 1초 단위
            preAllocatedVUs: 50,       // 사전 할당 VU
            maxVUs: 1000,              // 최대 VU
            stages: [
                { duration: '10s', target: 100 },
                { duration: '20s', target: 300 },
                { duration: '30s', target: 500 },
                { duration: '30s', target: 800 },
                { duration: '30s', target: 500 },
                { duration: '20s', target: 300 },
                { duration: '10s', target: 0 }
            ]
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<1000'],
        'http_req_failed': ['rate<0.1'],  // 재고 소진으로 인한 실패 허용
        'checks': ['rate>0.9'],
    },
};

// 테스트 시작 전 초기 재고 확인
export function setup() {
    const stockRes = http.get('http://localhost:8080/stock/1');
    const initialStock = stockRes.status === 200 ? parseInt(stockRes.body) : 0;
    console.log(`Initial stock for vendorItemId=1: ${initialStock}`);
    return { initialStock };
}

export default function (data) {
    // 각 가상 사용자마다 고유한 멱등성 키 생성
    const idempotencyKey = uuidv4();

    // 다양한 사용자 시뮬레이션
    const userId = Math.floor(Math.random() * 10000) + 1;

    // 각 주문은 1~5개 사이의 수량을 주문 (재고 300개를 고려하여 적절히 조정)
    const itemCount = Math.floor(Math.random() * 5) + 1;

    const payload = JSON.stringify({
        userId: userId,
        orderItem: [
            {
                vendorItemId: 1,
                itemCount: itemCount
            }
        ]
    });

    const headers = {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey
    };

    // 주문 API 호출
    const res = http.post('http://localhost:8080/orders', payload, { headers });

    // 응답 확인 - 재고 부족 에러도 허용
    check(res, {
        'status is success or handled inventory error': (r) =>
            r.status === 200 || r.status === 400 || r.status === 409,
        'response time < 500ms': (r) => r.timings.duration < 500,
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    });

    // 트래픽 분산을 위한 매우 짧은 지연
    sleep(0.01);
}

// 테스트 완료 후 잔여 재고 확인
export function teardown(data) {
    const stockRes = http.get('http://localhost:8080/stock/1');
    if (stockRes.status === 200) {
        const finalStock = parseInt(stockRes.body);
        console.log(`Final stock for vendorItemId=1: ${finalStock}`);
        console.log(`Estimated items ordered: ${data.initialStock - finalStock}`);
        console.log(`Remaining stock: ${finalStock}`);
    }
}