# CLAUDE.md - E-Commerce Project Guidelines

## Build & Test Commands
- Build project: `./gradlew build`
- Run application: `./gradlew bootRun`
- Run all tests: `./gradlew test`
- Run specific test: `./gradlew test --tests "org.project.ecommerce.order.OrderServiceTest"`
- Run specific test method: `./gradlew test --tests "org.project.ecommerce.order.OrderServiceTest.shouldCreateOrder"`
- Clean build: `./gradlew clean build`

## Code Style Guidelines
- **Naming**: Use camelCase for variables/methods, PascalCase for classes, UPPER_SNAKE_CASE for constants
- **Architecture**: Follow DDD patterns with packages organized by domain (order, fulfillment, etc.)
- **Imports**: Group imports by: java core, 3rd party libraries, project imports
- **Error handling**: Use custom exceptions when appropriate; validate inputs early
- **Lombok**: Use @Getter, @Builder, avoid @Setter where possible to enforce immutability
- **Testing**: Use JUnit 5 with descriptive @DisplayName, AssertJ for assertions
- **Domain entities**: Use @Entity with protected no-arg constructors, static factory methods
- **Transactions**: Handle via @Transactional annotations, outbox pattern for eventual consistency
- **Documentation**: Add JavaDoc to public APIs and complex methods