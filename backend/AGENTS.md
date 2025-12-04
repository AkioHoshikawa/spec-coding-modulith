# Backend Architecture Guide for AI Coding Agents

## Project Overview

This is a **Spring Boot 3.5.6** application implementing a **Modular Monolith** architecture using **Spring Modulith 1.4.1**. The application serves as an API server for a Fashion E-Commerce platform handling orders, inventory, promotions, shipments, and returns with high concurrency and data consistency requirements.

**Key Technologies:**
- Java 17
- Spring Boot 3.5.6
- Spring Modulith 1.4.1 (for modular architecture)
- Spring WebFlux (reactive programming)
- Spring Validation (bean validation)
- Spring Data JPA (database persistence)
- Gradle (build tool)

**Project Coordinates:**
- Group: `com.example.modulith`
- Artifact: `spec-coding-modulith-backend`
- Version: `0.0.1-SNAPSHOT`
- Base Package: `com.example.modulith.poc`

---

## Architecture Principles

### 1. Modular Monolith with Spring Modulith

This application follows **Spring Modulith** principles where the codebase is organized into loosely coupled, highly cohesive modules. Each module represents a bounded context from Domain-Driven Design (DDD).

**Key Principles:**
- **Module Isolation**: Modules communicate through well-defined APIs (events or service interfaces)
- **Event-Driven Communication**: Modules primarily communicate via application events (Spring's `ApplicationEventPublisher`)
- **Package-Based Modules**: Each module is represented by a top-level package under `com.example.modulith.poc.model`
- **No Direct Dependencies**: Modules should NOT directly depend on each other's internal classes

### 2. Layered Architecture within Application

The application uses a hybrid architecture pattern:

```
com.example.modulith.poc/
├── PocApplication.java          # Spring Boot entry point
├── channel/                     # Entry points (Web Controllers, Message Listeners, etc.)
│   └── web/
│       └── controller/          # REST API Controllers
├── core/                        # Shared infrastructure and utilities
│   ├── controller/              # Base controller utilities
│   └── event/                   # Event infrastructure
├── event/                       # Domain event definitions (contracts between modules)
│   ├── order/
│   ├── inventory/
│   ├── promotion/
│   └── ...
└── model/                       # Business domain modules (bounded contexts)
    ├── order/                   # Order module
    ├── inventory/               # Inventory module
    ├── promotion/               # Promotion module
    ├── shipment/                # Shipment module
    └── ...
```

---

## Package Structure and Responsibilities

### Root Package: `com.example.modulith.poc`

#### `PocApplication.java`
- Main Spring Boot application class
- Contains `@SpringBootApplication` annotation
- Entry point for the entire application

### Channel Layer: `channel/`

**Purpose**: Entry points for external interactions (HTTP, messaging, scheduled tasks, etc.)

#### `channel.web.controller/`
Controllers that handle HTTP requests. Controllers coordinate business logic by:
1. Publishing domain events
2. Waiting for completion events
3. Returning responses to clients

**Naming Convention**: `{Domain}Controller.java`

**Example**: `OrderController.java`
```java
@RestController
@RequestMapping("order")
public class OrderController extends EventCoordinatingController {
    @PostMapping("create")
    public Mono<OrderCreateComplete> order() {
        var orderCreate = new OrderCreate(...);
        return super.<OrderCreateComplete>sendEvent(orderCreate).asMono();
    }
}
```

**Guidelines for Controllers:**
- Extend `EventCoordinatingController` for event-driven operations
- Use reactive types (`Mono`, `Flux`) for responses
- Focus on orchestration, NOT business logic
- Publish domain events and wait for completion events
- Handle HTTP concerns (validation, error responses, status codes)

### Core Layer: `core/`

**Purpose**: Shared infrastructure, utilities, and cross-cutting concerns

#### `core.controller/`

**`EventCoordinatingController.java`**
Base class for controllers that need to coordinate asynchronous event-driven operations.

Features:
- Maintains a response map to correlate requests with events
- Provides `sendEvent()` method to publish events and create response sinks
- Provides `emitResponse()` method for event listeners to complete requests

**Usage Pattern:**
```java
// In controller
protected <T extends EventBase> Sinks.One<T> sendEvent(EventBase event) {
    // 1. Create response sink
    // 2. Register in response map with transaction ID
    // 3. Publish event
    // 4. Return sink
}

// In event listener (inside module)
protected <T extends EventBase> void emitResponse(T event) {
    // 1. Retrieve sink from response map
    // 2. Emit response
    // 3. Clean up
}
```

#### `core.event/`

**`EventBase.java`**
Base class for all domain events. Contains:
- `EventHeader header`: Metadata for event tracking

**`EventHeader.java`**
Event metadata record containing:
- `boolean error`: Error flag
- `String txId`: Transaction ID (correlation ID for request/response)
- `String userId`: User who triggered the event
- `OffsetDateTime createdDate`: Event creation timestamp

**Constructors:**
```java
new EventHeader(userId)                           // Auto-generates txId
new EventHeader(txId, userId)                     // Custom txId
new EventHeader(error, userId)                    // With error flag
new EventHeader(error, txId, userId)              // Full control
```

**`EventMapper.java`**
Utility for mapping between event objects using field name matching.

### Event Layer: `event/`

**Purpose**: Define domain event contracts that modules use to communicate

**Structure**: Organized by domain
```
event/
├── order/
│   ├── OrderCreate.java
│   ├── OrderCreateComplete.java
│   ├── OrderCancel.java
│   └── ...
├── inventory/
│   ├── ItemAllocate.java
│   ├── ItemAllocateComplete.java
│   └── ...
├── promotion/
└── shipment/
```

**Event Naming Convention:**
- Command events: `{Domain}{Action}` (e.g., `OrderCreate`, `ItemAllocate`)
- Completion events: `{Domain}{Action}Complete` (e.g., `OrderCreateComplete`, `ItemAllocateComplete`)
- Notification events: `{Domain}{Event}` (e.g., `OrderCancelled`, `PaymentFailed`)

**Event Design Guidelines:**

1. **Extend EventBase:**
```java
public final class OrderCreate extends EventBase {
    private final String itemId;
    private final Integer amount;
    
    public OrderCreate(EventHeader header, String itemId, Integer amount) {
        super(header);
        this.itemId = itemId;
        this.amount = amount;
    }
    // Getters only - immutable
}
```

2. **Use Records for Simple Events:**
```java
public record ItemAllocateComplete(
    EventHeader header,
    String allocationId
) {}
```

3. **Include Validation:**
```java
public final class OrderCreate extends EventBase {
    private final @NotBlank String itemId;
    private final @Min(1) Integer amount;
    // ...
}
```

4. **Make Events Immutable:**
- Final fields
- No setters
- Final class (for classes) or record

### Model Layer: `model/`

**Purpose**: Business domain modules implementing bounded contexts

**Structure**: Each module is a separate package with internal organization
```
model/{domain}/
├── entity/              # Domain entities and value objects
├── repository/          # Data access interfaces (Spring Data JPA repositories)
├── service/             # Business logic services
├── eventlistener/       # Event handlers (listeners)
└── dto/                 # Data transfer objects (optional)
```

**Module Isolation Rules:**
- ✅ Modules can publish domain events
- ✅ Modules can listen to domain events
- ✅ Modules can expose service interfaces (public API)
- ❌ Modules MUST NOT directly import classes from other modules (except via events/interfaces)
- ❌ Modules MUST NOT share entities or repositories

#### Example: `model.order/`

**`entity/OrderEntity.java`**
```java
public record OrderEntity(
    EventHeader header,
    String itemId,
    Integer amount,
    OffsetDateTime orderedDate
) {}
```

**`repository/OrderRepository.java`**
```java
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    // Spring Data JPA provides basic CRUD operations automatically
    // Add custom query methods as needed:
    // List<OrderEntity> findByUserId(String userId);
}
```

**`service/OrderService.java`**
```java
public interface OrderService {
    String createOrder(OrderEntity order);
}
```

**`eventlistener/OrderListener.java`**
```java
@Component
public class OrderListener {
    private final ApplicationEventPublisher publisher;
    
    @Autowired
    public OrderListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }
    
    @EventListener
    public void onOrderCreate(OrderCreate event) {
        // 1. Execute business logic
        // 2. Publish next event in the saga
        var itemAllocate = new ItemAllocate(event.getHeader(), ...);
        publisher.publishEvent(itemAllocate);
    }
}
```

**Module Communication Pattern (Saga/Choreography):**
```
OrderController -> OrderCreate event
    -> OrderListener handles -> ItemAllocate event
        -> InventoryListener handles -> ItemAllocateComplete event
            -> OrderListener handles -> OrderCreateComplete event
                -> OrderController receives response
```

---

## Implementation Guidelines

### When to Create a New Module

Create a new module under `model/` when:
1. **Bounded Context**: Represents a distinct business domain (Order, Inventory, Promotion, Shipment, Payment, User, etc.)
2. **Independent Lifecycle**: Can be developed, tested, and deployed independently
3. **Clear Boundaries**: Has well-defined responsibilities and doesn't overlap with other modules

**Module Naming**: `model.{domain}` (lowercase, singular form preferred)

Examples:
- `model.order` - Order management
- `model.inventory` - Inventory and stock management
- `model.promotion` - Promotions, coupons, discounts
- `model.shipment` - Shipping and logistics
- `model.payment` - Payment processing
- `model.user` - User account management
- `model.product` - Product catalog

### How to Create a New Module

1. **Create package structure:**
```
model/{domain}/
├── entity/
├── repository/
├── service/
└── eventlistener/
```

2. **Define domain events in `event/{domain}/`:**
```java
// event/payment/PaymentProcess.java
public record PaymentProcess(
    EventHeader header,
    String orderId,
    BigDecimal amount
) {}

// event/payment/PaymentComplete.java
public record PaymentComplete(
    EventHeader header,
    String paymentId,
    boolean success
) {}
```

3. **Create repository (if database access is needed):**
```java
// model/payment/repository/PaymentRepository.java
@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    List<PaymentEntity> findByOrderId(String orderId);
    Optional<PaymentEntity> findByTransactionId(String transactionId);
}
```

4. **Implement event listener:**
```java
// model/payment/eventlistener/PaymentListener.java
@Component
public class PaymentListener {
    private final ApplicationEventPublisher publisher;
    private final PaymentRepository paymentRepository;
    
    @Autowired
    public PaymentListener(
        ApplicationEventPublisher publisher,
        PaymentRepository paymentRepository
    ) {
        this.publisher = publisher;
        this.paymentRepository = paymentRepository;
    }
    
    @EventListener
    @Transactional
    public void onPaymentProcess(PaymentProcess event) {
        // Business logic with database access
        PaymentEntity payment = new PaymentEntity(...);
        paymentRepository.save(payment);
        
        publisher.publishEvent(new PaymentComplete(...));
    }
}
```

5. **Create service (if needed):**
```java
// model/payment/service/PaymentService.java
public interface PaymentService {
    String processPayment(String orderId, BigDecimal amount);
    void refund(String paymentId);
}

// model/payment/service/PaymentServiceImpl.java
@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    
    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    @Override
    @Transactional
    public String processPayment(String orderId, BigDecimal amount) {
        PaymentEntity payment = new PaymentEntity(orderId, amount);
        PaymentEntity saved = paymentRepository.save(payment);
        return saved.getPaymentId();
    }
    
    @Override
    @Transactional
    public void refund(String paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);
    }
}
```

### How to Add a New API Endpoint

1. **Create controller in `channel.web.controller/`:**
```java
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController extends EventCoordinatingController {
    
    @Autowired
    public PaymentController(ApplicationEventPublisher events) {
        super(events);
    }
    
    @PostMapping("/{orderId}/process")
    public Mono<PaymentComplete> processPayment(
        @PathVariable String orderId,
        @RequestBody PaymentRequest request
    ) {
        var event = new PaymentProcess(
            new EventHeader(request.getUserId()),
            orderId,
            request.getAmount()
        );
        return super.<PaymentComplete>sendEvent(event).asMono();
    }
}
```

2. **Define request/response DTOs:**
```java
// channel/web/dto/PaymentRequest.java
public record PaymentRequest(
    @NotBlank String userId,
    @NotNull @Positive BigDecimal amount,
    String paymentMethod
) {}
```

### Event-Driven Transaction Pattern (Saga)

For operations spanning multiple modules, use the choreography-based saga pattern:

```java
// Step 1: Controller publishes initial event
OrderController -> OrderCreate

// Step 2: OrderListener handles and publishes next event
OrderListener -> ItemAllocate

// Step 3: InventoryListener handles and publishes completion
InventoryListener -> ItemAllocateComplete

// Step 4: OrderListener handles and publishes final completion
OrderListener -> OrderCreateComplete

// Step 5: Controller receives and returns to client
OrderController <- OrderCreateComplete
```

**Error Handling in Saga:**
- If any step fails, publish compensating events
- Example: `ItemAllocateFailed` -> `OrderCancelled` -> rollback

### Database Implementation

The application uses **Spring Data JPA** for database persistence. The dependency is already configured in `build.gradle`:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

**Repository Pattern:**

Each module can have its own repository for data access. Repositories are interfaces that extend Spring Data JPA interfaces.

#### Creating a Repository

1. **Create repository interface in `model/{domain}/repository/`:**
```java
// model/order/repository/OrderRepository.java
package com.example.modulith.poc.model.order.repository;

import com.example.modulith.poc.model.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    // Spring Data JPA provides CRUD operations automatically:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    
    // Add custom query methods as needed:
    List<OrderEntity> findByUserId(String userId);
    List<OrderEntity> findByItemId(String itemId);
    
    // Complex queries using @Query annotation:
    @Query("SELECT o FROM OrderEntity o WHERE o.orderedDate >= :startDate")
    List<OrderEntity> findRecentOrders(@Param("startDate") OffsetDateTime startDate);
}
```

2. **Repository Interface Options:**

| Interface | Purpose | Key Methods |
|-----------|---------|-------------|
| `JpaRepository<T, ID>` | Full JPA features | `save()`, `findById()`, `findAll()`, `delete()`, `flush()`, `saveAndFlush()` |
| `CrudRepository<T, ID>` | Basic CRUD | `save()`, `findById()`, `findAll()`, `delete()`, `count()` |
| `PagingAndSortingRepository<T, ID>` | With pagination | `findAll(Pageable)`, `findAll(Sort)` |

**Recommended**: Use `JpaRepository` for full JPA capabilities.

3. **Entity Requirements:**

For JPA entities, add JPA annotations (currently, entities are simple records and may need to be converted to classes for JPA):

```java
// Note: Records can be used with JPA in Java 16+, but may need adjustment
// For complex entities, consider using classes:

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String orderId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String itemId;
    
    @Column(nullable = false)
    private Integer amount;
    
    @Column(name = "ordered_date", nullable = false)
    private OffsetDateTime orderedDate;
    
    // Constructors, getters, setters
}
```

4. **Using Repositories in Services:**
```java
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    
    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    @Transactional
    public String createOrder(OrderEntity order) {
        OrderEntity saved = orderRepository.save(order);
        return saved.orderId();
    }
    
    public Optional<OrderEntity> findById(String orderId) {
        return orderRepository.findById(orderId);
    }
}
```

5. **Transaction Management:**

Use `@Transactional` annotation on service methods that modify data:

```java
@Transactional  // Ensures atomicity
public void processOrder(String orderId) {
    OrderEntity order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    
    // Multiple database operations in one transaction
    order.setStatus("PROCESSING");
    orderRepository.save(order);
    
    // If exception occurs, all changes are rolled back
}

@Transactional(readOnly = true)  // Optimize read-only queries
public List<OrderEntity> findUserOrders(String userId) {
    return orderRepository.findByUserId(userId);
}
```

#### Repository Best Practices

1. **Keep repositories in their module's package** - Don't share repositories across modules
2. **Use method naming conventions** - Spring Data JPA generates queries from method names:
   - `findBy{Field}` - Find by single field
   - `findBy{Field}And{Field2}` - Multiple conditions
   - `findBy{Field}OrderBy{Field2}Desc` - With sorting
   - `countBy{Field}` - Count matching records
   - `deleteBy{Field}` - Delete matching records

3. **Add @Repository annotation** - Although optional with Spring Data JPA, it's recommended for clarity

4. **Define custom queries when needed**:
```java
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    @Query("SELECT o FROM OrderEntity o WHERE o.userId = :userId AND o.orderedDate >= :startDate")
    List<OrderEntity> findUserOrdersSince(
        @Param("userId") String userId,
        @Param("startDate") OffsetDateTime startDate
    );
    
    // Native SQL query
    @Query(value = "SELECT * FROM orders WHERE status = ?1 LIMIT ?2", nativeQuery = true)
    List<OrderEntity> findByStatusLimit(String status, int limit);
}
```

#### Database Configuration

Configure database connection in `application.properties`:

```properties
# DataSource Configuration (example for PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce
spring.datasource.username=dbuser
spring.datasource.password=dbpass
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# For H2 in-memory database (development/testing)
# spring.datasource.url=jdbc:h2:mem:testdb
# spring.datasource.driver-class-name=org.h2.Driver
# spring.h2.console.enabled=true
```

**Important**: Database schemas are defined in `doc/data/schema/*.sql`. Use these as the source of truth for table structure.

### Validation Guidelines

Use Bean Validation (Jakarta Validation) for input validation:

```java
// On events
public final class OrderCreate extends EventBase {
    private final @NotBlank String itemId;
    private final @Min(1) @Max(9999) Integer amount;
    private final @NotNull OffsetDateTime orderedDate;
}

// On DTOs
public record CreateOrderRequest(
    @NotBlank(message = "Item ID is required")
    String itemId,
    
    @NotNull
    @Min(value = 1, message = "Amount must be at least 1")
    Integer amount
) {}

// In controller
@PostMapping
public Mono<OrderCreateComplete> create(@Valid @RequestBody CreateOrderRequest request) {
    // Validation happens automatically
}
```

### Error Handling Guidelines

1. **Use EventHeader.error flag:**
```java
var errorHeader = new EventHeader(true, txId, userId);
var errorEvent = new OrderCreateComplete(errorHeader, null);
```

2. **Create exception handlers:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InventoryInsufficientException.class)
    public ResponseEntity<ErrorResponse> handleInventoryInsufficient(
        InventoryInsufficientException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("INVENTORY_INSUFFICIENT", ex.getMessage()));
    }
}
```

3. **Define domain exceptions:**
```java
// model/inventory/exception/InventoryInsufficientException.java
public class InventoryInsufficientException extends RuntimeException {
    private final String sku;
    private final int requested;
    private final int available;
    
    public InventoryInsufficientException(String sku, int requested, int available) {
        super(String.format("Insufficient inventory for SKU %s: requested=%d, available=%d",
            sku, requested, available));
        this.sku = sku;
        this.requested = requested;
        this.available = available;
    }
}
```

---

## Testing Guidelines

### Unit Testing

Test individual components in isolation:

```java
@SpringBootTest
class OrderServiceTest {
    @Autowired
    private OrderService orderService;
    
    @Test
    void shouldCreateOrder() {
        var order = new OrderEntity(...);
        String orderId = orderService.createOrder(order);
        assertNotNull(orderId);
    }
}
```

### Module Integration Testing

Use Spring Modulith testing support:

```java
@SpringBootTest
@ModuleTest
class OrderModuleTest {
    @Autowired
    private ApplicationEventPublisher events;
    
    @Test
    void shouldPublishItemAllocateWhenOrderCreated() {
        var orderCreate = new OrderCreate(...);
        events.publishEvent(orderCreate);
        
        // Verify ItemAllocate event was published
    }
}
```

### Event Testing

Test event listeners:

```java
@SpringBootTest
class OrderListenerTest {
    @Autowired
    private OrderListener listener;
    
    @Autowired
    private ApplicationEventPublisher publisher;
    
    @Test
    void shouldHandleOrderCreate() {
        var event = new OrderCreate(...);
        listener.onOrderCreate(event);
        
        // Verify behavior
    }
}
```

---

## Coding Standards

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Package | lowercase, singular | `model.order`, `event.inventory` |
| Class | PascalCase | `OrderController`, `InventoryListener` |
| Interface | PascalCase | `OrderService`, `InventoryRepository` |
| Record | PascalCase | `OrderEntity`, `ItemAllocate` |
| Event | `{Domain}{Action}` | `OrderCreate`, `PaymentComplete` |
| Controller | `{Domain}Controller` | `OrderController` |
| Service | `{Domain}Service` | `OrderService` |
| Listener | `{Domain}Listener` | `OrderListener` |
| Repository | `{Domain}Repository` | `OrderRepository` |

### Code Organization

1. **Group imports:**
   - Java standard library
   - Third-party libraries
   - Spring framework
   - Project imports

2. **Class member order:**
   - Static fields
   - Instance fields
   - Constructors
   - Public methods
   - Protected methods
   - Private methods

3. **Use records for immutable data:**
```java
// Prefer
public record OrderEntity(String orderId, String userId, OffsetDateTime createdAt) {}

// Over
public class OrderEntity {
    private final String orderId;
    private final String userId;
    // ... getters, constructors
}
```

### Reactive Programming

Use reactive types consistently:

```java
// Controller returns Mono/Flux
@GetMapping("/{id}")
public Mono<OrderResponse> getOrder(@PathVariable String id) {
    return orderService.findById(id)
        .map(this::toResponse);
}

// Service layer
public Mono<Order> findById(String id) {
    return Mono.fromCallable(() -> repository.findById(id))
        .flatMap(Mono::justOrEmpty);
}
```

---

## Business Domain Reference

This application implements a **Fashion E-Commerce platform** with the following domains:

### Functional Domains

1. **Order Management** (`model.order`)
   - Order creation and confirmation
   - Order status tracking
   - Order cancellation
   - Related events: `OrderCreate`, `OrderCreateComplete`, `OrderCancel`

2. **Inventory Management** (`model.inventory`)
   - Stock level management
   - Inventory allocation and locking
   - Concurrency control (optimistic locking)
   - Related events: `ItemAllocate`, `ItemAllocateComplete`, `ItemAllocateFailed`

3. **Promotion Management** (`model.promotion`)
   - Coupon validation
   - Discount calculation (Simple percentage discount)
   - (Simplified from complex campaigns)

4. **Shipment Management** (`model.shipment`)
   - Shipping instruction to 3PL
   - Delivery tracking
   - Shipment status updates
   - (Simplified for this phase)

5. **Return/Exchange Management** (`model.return`)
   - (Removed from scope)

6. **User Management** (`model.user`)
   - User registration and authentication
   - Profile management
   - Address book management
   - Order history

7. **Payment Processing** (`model.payment`)
   - Payment authorization
   - Payment capture
   - Refund processing
   - Payment failure handling

### Critical Business Rules

Refer to `/doc/business-rule/` for detailed business rules. Key rules include:

- **BR-001**: 在庫管理 (Inventory Management)
- **BR-002**: プロモーション (Promotion)
- **BR-003**: 注文ライフサイクル (Order Lifecycle)
- **BR-004**: カート管理 (Cart Management)

### Performance Requirements

- **Peak load**: Support thousands of concurrent requests (especially during sales)
- **Inventory locking**: Must complete within 2 seconds (P99)
- **Data consistency**: Prevent double allocation and overselling
- **Idempotency**: Handle duplicate requests gracefully

---

## External Resources

### API Specifications
- OpenAPI definitions: `/doc/api/openapi.yaml`
- API endpoints by domain: `/doc/api/paths/{domain}/`
- Data models: `/doc/api/components/schemas/`

### Database Schema
- Schema definitions: `/doc/data/schema/*.sql`
- Data model documentation: `/doc/data/model/`
- Table specifications by domain: `/doc/data/model/{domain}-tables.md`

### User Stories
- User story templates: `/doc/user-story/`
- Examples: `US-001` through `US-005`

### Project Documentation
- Project overview: `/doc/project-overview.md`
- Architecture diagrams: `/doc/architecture/`

---

## Quick Reference for Common Tasks

### Add a New Domain Module

1. Create package: `model.{domain}/`
2. Add subpackages: `entity/`, `service/`, `eventlistener/`
3. Define events in: `event.{domain}/`
4. Create listener: `{Domain}Listener.java`
5. Create service: `{Domain}Service.java`

### Add a New REST Endpoint

1. Create controller: `channel.web.controller.{Domain}Controller.java`
2. Extend `EventCoordinatingController`
3. Add mapping: `@RequestMapping("/api/v1/{domain}")`
4. Define DTOs for request/response
5. Publish events and return `Mono<T>` or `Flux<T>`

### Add Event-Driven Communication

1. Define events in `event.{domain}/`
2. Create listener in `model.{targetdomain}.eventlistener/`
3. Use `@EventListener` annotation
4. Inject `ApplicationEventPublisher` to publish subsequent events
5. Follow saga pattern for multi-step operations

### Add Database Persistence to a Module

1. Create repository interface in `model.{domain}.repository/`
2. Extend `JpaRepository<EntityType, IdType>` and add `@Repository`
3. Add custom query methods using naming conventions or `@Query`
4. Inject repository into services/listeners
5. Add JPA annotations to entities (`@Entity`, `@Table`, `@Id`, etc.)
6. Refer to `/doc/data/schema/{domain}.sql` for table structure
7. Use `@Transactional` in service methods and event listeners

---

## Important Notes for AI Agents

1. **Never break module boundaries**: Do not create direct dependencies between modules
2. **Always use events**: Inter-module communication must go through domain events
3. **Follow naming conventions**: Consistency is critical for maintainability
4. **Validate inputs**: Use Bean Validation on all external inputs
5. **Make everything immutable**: Prefer records and final fields
6. **Think reactive**: Use `Mono`/`Flux` for async operations
7. **Refer to business rules**: Check `/doc/business-rule/` before implementing logic
8. **Check API specs**: Ensure consistency with `/doc/api/` definitions
9. **Follow database schema**: Use `/doc/data/schema/` as the source of truth
10. **Test module isolation**: Use Spring Modulith testing to verify boundaries

---

## Build and Run

### Build
```bash
./gradlew build
```

### Run
```bash
./gradlew bootRun
```

### Test
```bash
./gradlew test
```

### Clean
```bash
./gradlew clean
```

---

## Configuration

### Application Properties
Location: `src/main/resources/application.properties`

Current configuration:
```properties
spring.application.name=poc
```

When adding configuration:
- Use environment-specific profiles: `application-{profile}.properties`
- Externalize secrets (use environment variables)
- Document all custom properties

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-12 | Initial architecture documentation |

---

**For Questions or Clarifications:**
Refer to the source code, business rules in `/doc/business-rule/`, and API specifications in `/doc/api/`. When in doubt, follow Spring Modulith best practices and maintain strict module boundaries.
