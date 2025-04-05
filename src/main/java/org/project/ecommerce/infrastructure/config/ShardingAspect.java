package org.project.ecommerce.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.project.ecommerce.infrastructure.route.RoutingDataSource;
import org.project.ecommerce.infrastructure.route.ShardRouter;
import org.project.ecommerce.order.domain.Order;
import org.springframework.stereotype.Component;

/**
 * 샤딩 라우팅을 위한 AOP 구현
 * - 트랜잭션 메서드 실행 전/후에 샤드 라우팅 처리
 * - 주문 관련 트랜잭션에서 적절한 샤드 선택 보장
 */
@Aspect
@Component
@Slf4j
public class ShardingAspect {

    /**
     * 오더 ID 기반 샤딩 라우팅
     * - Order 엔티티를 파라미터로 받는 메서드에 적용
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) && " +
            "args(order,..)")
    public Object routeOrderTransaction(ProceedingJoinPoint joinPoint, Order order) throws Throwable {
        try {
            // 주문 ID가 있으면 해당 ID로 샤드 설정
            if (order != null && order.getId() != null) {
                log.debug("Setting shard key based on Order ID: {}", order.getId());
                ShardRouter.setShardKey(order.getId());
            }
            return joinPoint.proceed();
        } finally {
            // 메서드 실행 후 샤드 설정 초기화
            RoutingDataSource.clearDataSource();
        }
    }

    /**
     * Long 타입 오더 ID 기반 샤딩 라우팅
     * - 주문 ID를 직접 파라미터로 받는 메서드에 적용
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) && " +
            "args(orderId,..)")
    public Object routeOrderIdTransaction(ProceedingJoinPoint joinPoint, Long orderId) throws Throwable {
        try {
            // 주문 ID가 있으면 해당 ID로 샤드 설정
            if (orderId != null) {
                log.debug("Setting shard key based on orderId parameter: {}", orderId);
                ShardRouter.setShardKey(orderId);
            }
            return joinPoint.proceed();
        } finally {
            // 메서드 실행 후 샤드 설정 초기화
            RoutingDataSource.clearDataSource();
        }
    }
}