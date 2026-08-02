# H2 Db

```gradle
runtimeOnly 'com.h2database:h2'
// Source: https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-h2console
implementation 'org.springframework.boot:spring-boot-h2console:4.0.0-M1'
annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
```

```properties
# To persist data in a file
spring.datasource.url=jdbc:h2:file:C:/Users/bsara/h2db/springAI
#spring.datasource.url=jdbc:h2:file:./h2DBdata/springAi
#spring.datasource.url=jdbc:h2:file:~/chatmemory;AUTO_SERVER=true
# Set the same url in browser url - jdbc:h2:file:/resources/data
spring.datasource.username=sa
spring.datasource.password=sa

## We have sql file here
## spring-ai-model-chat-memory-repository-jdbc-2.0.0.jar!\org\springframework\ai\chat\memory\repository\jdbc\schema-h2.sql
## spring-ai-model-chat-memory-repository-jdbc-2.0.0.jar!\org\springframework\ai\chat\memory\repository\jdbc\schema-postgresql.sql
#Old Driver
#spring.datasource.driverClassName=org.h2.Driver
spring.datasource.driver-class-name=org.h2.Driver
#spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

#spring.jpa.open-in-view=false

# H2 console http://localhost:8080/h2-console
# We need this dependency to use the bellow properties
# implementation 'org.springframework.boot:spring-boot-h2console:4.0.0-M1'
# Enable the H2 web console (disabled by default for security)
spring.h2.console.enabled=true

# URL path to access the console in the browser
spring.h2.console.path=/h2-console
# Console path console http://localhost:8080/h2 We are changing the default path
# spring.h2.console.path=/h2
spring.h2.console.settings.trace=false
# Allow access when running behind a reverse proxy or from non-localhost
spring.h2.console.settings.web-allow-others=false
spring.ai.chat.memory.repository.jdbc.initialize-schema=always
```
http://localhost:8080/h2-console
jdbc url - jdbc:h2:file:C:/Users/bsara/h2db/springAI

# compose.yml

- localhsot:6333/dashboard

