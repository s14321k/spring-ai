# For HelpDeskTicket.java

Almost, but **`@CreatedDate` alone is not enough**.

For Spring Data JPA auditing to populate `createdAt` automatically, you need:

### 1. Enable JPA Auditing

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
}
```

### 2. Add the entity listener

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "helpdesk_ticket")
public class HelpDeskTicket {
```

Import:

```java
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
```

### 3. Annotate the field

```java
@CreatedDate
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;
```

### Complete Example

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "helpdesk_ticket")
public class HelpDeskTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String issue;

    private String status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime eta;
}
```

### Alternative (simpler)

If you only need a creation timestamp and don't care about Spring auditing, use Hibernate:

```java
@CreationTimestamp
private LocalDateTime createdAt;
```

Import:

```java
import org.hibernate.annotations.CreationTimestamp;
```

For a simple Help Desk Ticket entity, I usually prefer:

```java
@CreationTimestamp
private LocalDateTime createdAt;
```

because it requires **no `@EnableJpaAuditing`**, **no `AuditingEntityListener`**, and works automatically with Hibernate.

If you're planning to add fields like:

```java
@CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;

@CreatedBy
private String createdBy;

@LastModifiedBy
private String updatedBy;
```

then JPA Auditing is the better choice.
