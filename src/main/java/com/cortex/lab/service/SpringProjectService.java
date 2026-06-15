package com.cortex.lab.service;

import com.cortex.lab.dto.ProjectFileDTO;
import com.cortex.lab.dto.ProjectInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SpringProjectService {
    // Spring Boot 项目生成器：根据知识节点生成可编译的 Maven 项目

    private static final String SPRING_BOOT_VERSION = "3.2.5";

    // ======================== 项目分发 ========================

    public ProjectInfoDTO generateProject(String nodeId) {
        String projectName = "spring-boot-demo";
        String description;
        List<ProjectFileDTO> files;

        switch (nodeId) {
            case "spring-ioc":
                description = "Spring IoC 容器演示：依赖注入与控制反转";
                files = buildIocProject(projectName);
                break;
            case "spring-aop":
                description = "Spring AOP 面向切面编程演示";
                files = buildAopProject(projectName);
                break;
            case "spring-mvc":
                description = "Spring MVC 请求处理流程演示";
                files = buildMvcProject(projectName);
                break;
            case "spring-security":
                description = "Spring Security 认证授权：JWT + RBAC 权限控制";
                files = buildSecurityProject(projectName);
                break;
            case "spring-jpa":
                description = "Spring Data JPA：实体映射、关联查询、Repository";
                files = buildJpaProject(projectName);
                break;
            case "spring-mybatis-plus":
                description = "MyBatis-Plus：自动映射、Wrapper、分页插件";
                files = buildMybatisPlusProject(projectName);
                break;
            case "spring-cache-redis":
                description = "Spring Cache + Redis：缓存注解、分布式锁";
                files = buildCacheRedisProject(projectName);
                break;
            case "spring-websocket":
                description = "WebSocket 实时通信：Stomp + 广播";
                files = buildWebsocketProject(projectName);
                break;
            case "spring-rabbitmq":
                description = "RabbitMQ 集成：消息确认、死信队列";
                files = buildRabbitmqProject(projectName);
                break;
            default:
                description = "Spring Boot 基础项目";
                files = buildIocProject(projectName);
        }

        return new ProjectInfoDTO(nodeId, projectName, description, "", files);
    }

    // ======================== POM 工厂 ========================

    private String basePom(String projectName, String... extraDeps) {
        StringBuilder deps = new StringBuilder();
        deps.append("        <dependency>\n")
            .append("            <groupId>org.springframework.boot</groupId>\n")
            .append("            <artifactId>spring-boot-starter-web</artifactId>\n")
            .append("        </dependency>\n");
        for (String dep : extraDeps) {
            deps.append(dep);
        }
        deps.append("        <dependency>\n")
            .append("            <groupId>org.springframework.boot</groupId>\n")
            .append("            <artifactId>spring-boot-starter-test</artifactId>\n")
            .append("            <scope>test</scope>\n")
            .append("        </dependency>\n");

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <parent>\n" +
            "        <groupId>org.springframework.boot</groupId>\n" +
            "        <artifactId>spring-boot-starter-parent</artifactId>\n" +
            "        <version>" + SPRING_BOOT_VERSION + "</version>\n" +
            "        <relativePath/>\n" +
            "    </parent>\n" +
            "    <groupId>com.example</groupId>\n" +
            "    <artifactId>" + projectName + "</artifactId>\n" +
            "    <version>1.0.0</version>\n" +
            "    <properties>\n" +
            "        <java.version>17</java.version>\n" +
            "    </properties>\n" +
            "    <dependencies>\n" + deps + "    </dependencies>\n" +
            "    <build>\n" +
            "        <plugins>\n" +
            "            <plugin>\n" +
            "                <groupId>org.springframework.boot</groupId>\n" +
            "                <artifactId>spring-boot-maven-plugin</artifactId>\n" +
            "            </plugin>\n" +
            "        </plugins>\n" +
            "    </build>\n" +
            "</project>\n";
    }

    private static String dep(String groupId, String artifactId) {
        return "        <dependency>\n" +
            "            <groupId>" + groupId + "</groupId>\n" +
            "            <artifactId>" + artifactId + "</artifactId>\n" +
            "        </dependency>\n";
    }

    private static String dep(String groupId, String artifactId, String version) {
        return "        <dependency>\n" +
            "            <groupId>" + groupId + "</groupId>\n" +
            "            <artifactId>" + artifactId + "</artifactId>\n" +
            "            <version>" + version + "</version>\n" +
            "        </dependency>\n";
    }

    private String applicationProperties(String... lines) {
        StringBuilder sb = new StringBuilder("server.port=8081\nspring.application.name=spring-boot-demo\n");
        for (String line : lines) sb.append(line).append("\n");
        return sb.toString();
    }

    private ProjectFileDTO appClass() {
        return new ProjectFileDTO("src/main/java/com/example/DemoApplication.java",
            "package com.example;\n\n" +
            "import org.springframework.boot.SpringApplication;\n" +
            "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
            "@SpringBootApplication\n" +
            "public class DemoApplication {\n" +
            "    public static void main(String[] args) {\n" +
            "        SpringApplication.run(DemoApplication.class, args);\n" +
            "    }\n" +
            "}\n");
    }

    // ======================== IoC 项目 ========================

    private List<ProjectFileDTO> buildIocProject(String projectName) {
        List<ProjectFileDTO> files = new ArrayList<>();
        files.add(new ProjectFileDTO("pom.xml", basePom(projectName)));
        files.add(new ProjectFileDTO("src/main/resources/application.properties", applicationProperties()));
        files.add(appClass());
        files.add(new ProjectFileDTO("src/main/java/com/example/service/GreetingService.java",
            "package com.example.service;\n\n" +
            "import org.springframework.stereotype.Service;\n\n" +
            "@Service\n" +
            "public class GreetingService {\n" +
            "    public String greet(String name) {\n" +
            "        return \"你好, \" + name + \"! 欢迎来到 Spring Boot 世界。\";\n" +
            "    }\n" +
            "}\n"));
        files.add(new ProjectFileDTO("src/main/java/com/example/controller/HelloController.java",
            "package com.example.controller;\n\n" +
            "import com.example.service.GreetingService;\n" +
            "import org.springframework.beans.factory.annotation.Autowired;\n" +
            "import org.springframework.web.bind.annotation.*;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api\")\n" +
            "public class HelloController {\n\n" +
            "    @Autowired\n" +
            "    private GreetingService greetingService;\n\n" +
            "    @GetMapping(\"/hello\")\n" +
            "    public String hello(@RequestParam(defaultValue = \"Spring\") String name) {\n" +
            "        return greetingService.greet(name);\n" +
            "    }\n" +
            "}\n"));
        files.add(new ProjectFileDTO("src/main/java/com/example/controller/UserController.java",
            "package com.example.controller;\n\n" +
            "import com.example.entity.User;\n" +
            "import jakarta.annotation.PostConstruct;\n" +
            "import org.springframework.web.bind.annotation.*;\n" +
            "import java.util.*;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/users\")\n" +
            "public class UserController {\n\n" +
            "    private final Map<Long, User> users = new HashMap<>();\n" +
            "    private long nextId = 1;\n\n" +
            "    @PostConstruct\n" +
            "    public void init() {\n" +
            "        users.put(nextId, new User(nextId++, \"张三\", \"zhangsan@example.com\"));\n" +
            "        users.put(nextId, new User(nextId++, \"李四\", \"lisi@example.com\"));\n" +
            "    }\n\n" +
            "    @GetMapping\n" +
            "    public List<User> list() { return new ArrayList<>(users.values()); }\n\n" +
            "    @GetMapping(\"/{id}\")\n" +
            "    public User get(@PathVariable Long id) { return users.get(id); }\n" +
            "}\n"));
        files.add(new ProjectFileDTO("src/main/java/com/example/entity/User.java",
            "package com.example.entity;\n\n" +
            "public class User {\n" +
            "    private Long id; private String name; private String email;\n" +
            "    public User() {}\n" +
            "    public User(Long id, String name, String email) { this.id = id; this.name = name; this.email = email; }\n" +
            "    public Long getId() { return id; } public void setId(Long id) { this.id = id; }\n" +
            "    public String getName() { return name; } public void setName(String name) { this.name = name; }\n" +
            "    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }\n" +
            "}\n"));
        return files;
    }

    // ======================== AOP 项目 ========================

    private List<ProjectFileDTO> buildAopProject(String projectName) {
        List<ProjectFileDTO> files = new ArrayList<>(buildIocProject(projectName));
        String aopPom = basePom(projectName, dep("org.springframework.boot", "spring-boot-starter-aop"));
        files.set(0, new ProjectFileDTO("pom.xml", aopPom));
        files.add(new ProjectFileDTO("src/main/java/com/example/aspect/LoggingAspect.java",
            "package com.example.aspect;\n\n" +
            "import org.aspectj.lang.JoinPoint;\n" +
            "import org.aspectj.lang.annotation.*;\n" +
            "import org.slf4j.Logger;\n" +
            "import org.slf4j.LoggerFactory;\n" +
            "import org.springframework.stereotype.Component;\n" +
            "import java.util.Arrays;\n\n" +
            "@Aspect\n" +
            "@Component\n" +
            "public class LoggingAspect {\n" +
            "    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);\n" +
            "    @Pointcut(\"execution(* com.example.controller..*(..))\")\n" +
            "    public void controllerMethods() {}\n" +
            "    @Before(\"controllerMethods()\")\n" +
            "    public void beforeController(JoinPoint jp) {\n" +
            "        log.info(\"[AOP] 调用: {} 参数: {}\", jp.getSignature().toShortString(), Arrays.toString(jp.getArgs()));\n" +
            "    }\n" +
            "    @AfterReturning(pointcut = \"controllerMethods()\", returning = \"result\")\n" +
            "    public void afterReturning(JoinPoint jp, Object result) {\n" +
            "        log.info(\"[AOP] 返回: {} 结果: {}\", jp.getSignature().toShortString(), result);\n" +
            "    }\n" +
            "}\n"));
        return files;
    }

    // ======================== MVC 项目 ========================

    private List<ProjectFileDTO> buildMvcProject(String projectName) {
        List<ProjectFileDTO> files = new ArrayList<>(buildIocProject(projectName));
        // 替换 HelloController 为更全面的 MVC 演示
        files.set(4, new ProjectFileDTO("src/main/java/com/example/controller/HelloController.java",
            "package com.example.controller;\n\n" +
            "import jakarta.servlet.http.HttpServletRequest;\n" +
            "import org.springframework.web.bind.annotation.*;\n" +
            "import java.util.Map;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api\")\n" +
            "public class HelloController {\n\n" +
            "    @GetMapping(\"/hello\")\n" +
            "    public String hello(@RequestParam(defaultValue = \"Spring\") String name) {\n" +
            "        return \"你好, \" + name + \"!\";\n" +
            "    }\n\n" +
            "    @PostMapping(\"/echo\")\n" +
            "    public String echo(@RequestBody String body) { return \"你说了: \" + body; }\n\n" +
            "    @GetMapping(\"/info\")\n" +
            "    public Map<String, Object> info(HttpServletRequest request) {\n" +
            "        return Map.of(\"method\", request.getMethod(), \"path\", request.getRequestURI());\n" +
            "    }\n" +
            "}\n"));
        return files;
    }

    // ======================== Spring Security 项目 ========================

    private List<ProjectFileDTO> buildSecurityProject(String projectName) {
        List<ProjectFileDTO> files = new ArrayList<>();
        files.add(new ProjectFileDTO("pom.xml", basePom(projectName,
            dep("org.springframework.boot", "spring-boot-starter-security"),
            dep("io.jsonwebtoken", "jjwt-api", "0.12.5"),
            dep("io.jsonwebtoken", "jjwt-impl", "0.12.5"),
            dep("io.jsonwebtoken", "jjwt-jackson", "0.12.5"))));
        files.add(new ProjectFileDTO("src/main/resources/application.properties",
            applicationProperties("jwt.secret=MySecretKeyForJWTTokenGeneration2024SecurityDemo",
                "jwt.expiration=86400000")));
        files.add(appClass());

        files.add(new ProjectFileDTO("src/main/java/com/example/config/SecurityConfig.java",
            "package com.example.config;\n\n" +
            "import com.example.security.JwtAuthFilter;\n" +
            "import org.springframework.context.annotation.Bean;\n" +
            "import org.springframework.context.annotation.Configuration;\n" +
            "import org.springframework.security.config.annotation.web.builders.HttpSecurity;\n" +
            "import org.springframework.security.config.http.SessionCreationPolicy;\n" +
            "import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;\n" +
            "import org.springframework.security.crypto.password.PasswordEncoder;\n" +
            "import org.springframework.security.web.SecurityFilterChain;\n" +
            "import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;\n\n" +
            "@Configuration\n" +
            "public class SecurityConfig {\n\n" +
            "    private final JwtAuthFilter jwtAuthFilter;\n" +
            "    public SecurityConfig(JwtAuthFilter jwtAuthFilter) { this.jwtAuthFilter = jwtAuthFilter; }\n\n" +
            "    @Bean\n" +
            "    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {\n" +
            "        http.csrf(csrf -> csrf.disable())\n" +
            "            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))\n" +
            "            .authorizeHttpRequests(auth -> auth\n" +
            "                .requestMatchers(\"/api/auth/**\").permitAll()\n" +
            "                .requestMatchers(\"/api/admin/**\").hasRole(\"ADMIN\")\n" +
            "                .anyRequest().authenticated())\n" +
            "            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);\n" +
            "        return http.build();\n" +
            "    }\n\n" +
            "    @Bean\n" +
            "    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/security/JwtTokenProvider.java",
            "package com.example.security;\n\n" +
            "import io.jsonwebtoken.*;\n" +
            "import io.jsonwebtoken.security.Keys;\n" +
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "import org.springframework.stereotype.Component;\n" +
            "import javax.crypto.SecretKey;\n" +
            "import java.nio.charset.StandardCharsets;\n" +
            "import java.util.Date;\n\n" +
            "@Component\n" +
            "public class JwtTokenProvider {\n\n" +
            "    private final SecretKey key;\n" +
            "    private final long expiration;\n\n" +
            "    public JwtTokenProvider(@Value(\"${jwt.secret}\") String secret,\n" +
            "                           @Value(\"${jwt.expiration}\") long expiration) {\n" +
            "        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));\n" +
            "        this.expiration = expiration;\n" +
            "    }\n\n" +
            "    public String generateToken(String username, String role) {\n" +
            "        return Jwts.builder()\n" +
            "            .subject(username)\n" +
            "            .claim(\"role\", role)\n" +
            "            .issuedAt(new Date())\n" +
            "            .expiration(new Date(System.currentTimeMillis() + expiration))\n" +
            "            .signWith(key)\n" +
            "            .compact();\n" +
            "    }\n\n" +
            "    public String getUsernameFromToken(String token) {\n" +
            "        return Jwts.parser().verifyWith(key).build()\n" +
            "            .parseSignedClaims(token).getPayload().getSubject();\n" +
            "    }\n\n" +
            "    public boolean validateToken(String token) {\n" +
            "        try { Jwts.parser().verifyWith(key).build().parseSignedClaims(token); return true; }\n" +
            "        catch (JwtException | IllegalArgumentException e) { return false; }\n" +
            "    }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/security/JwtAuthFilter.java",
            "package com.example.security;\n\n" +
            "import jakarta.servlet.FilterChain;\n" +
            "import jakarta.servlet.ServletException;\n" +
            "import jakarta.servlet.http.HttpServletRequest;\n" +
            "import jakarta.servlet.http.HttpServletResponse;\n" +
            "import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;\n" +
            "import org.springframework.security.core.authority.SimpleGrantedAuthority;\n" +
            "import org.springframework.security.core.context.SecurityContextHolder;\n" +
            "import org.springframework.stereotype.Component;\n" +
            "import org.springframework.web.filter.OncePerRequestFilter;\n" +
            "import java.io.IOException;\n" +
            "import java.util.List;\n\n" +
            "@Component\n" +
            "public class JwtAuthFilter extends OncePerRequestFilter {\n\n" +
            "    private final JwtTokenProvider jwtTokenProvider;\n" +
            "    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) { this.jwtTokenProvider = jwtTokenProvider; }\n\n" +
            "    @Override\n" +
            "    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,\n" +
            "                                    FilterChain chain) throws ServletException, IOException {\n" +
            "        String header = request.getHeader(\"Authorization\");\n" +
            "        if (header != null && header.startsWith(\"Bearer \")) {\n" +
            "            String token = header.substring(7);\n" +
            "            if (jwtTokenProvider.validateToken(token)) {\n" +
            "                String username = jwtTokenProvider.getUsernameFromToken(token);\n" +
            "                var auth = new UsernamePasswordAuthenticationToken(username, null, List.of());\n" +
            "                SecurityContextHolder.getContext().setAuthentication(auth);\n" +
            "            }\n" +
            "        }\n" +
            "        chain.doFilter(request, response);\n" +
            "    }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/controller/AuthController.java",
            "package com.example.controller;\n\n" +
            "import com.example.security.JwtTokenProvider;\n" +
            "import org.springframework.web.bind.annotation.*;\n" +
            "import java.util.Map;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/auth\")\n" +
            "public class AuthController {\n\n" +
            "    private final JwtTokenProvider jwtTokenProvider;\n" +
            "    public AuthController(JwtTokenProvider jwtTokenProvider) { this.jwtTokenProvider = jwtTokenProvider; }\n\n" +
            "    @PostMapping(\"/login\")\n" +
            "    public Map<String, String> login(@RequestBody Map<String, String> body) {\n" +
            "        String username = body.get(\"username\");\n" +
            "        String token = jwtTokenProvider.generateToken(username, \"USER\");\n" +
            "        return Map.of(\"token\", token, \"username\", username);\n" +
            "    }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/controller/AdminController.java",
            "package com.example.controller;\n\n" +
            "import org.springframework.web.bind.annotation.*;\n" +
            "import java.util.Map;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/admin\")\n" +
            "public class AdminController {\n\n" +
            "    @GetMapping(\"/dashboard\")\n" +
            "    public Map<String, String> dashboard() {\n" +
            "        return Map.of(\"message\", \"欢迎管理员！只有 ADMIN 角色能访问\");\n" +
            "    }\n" +
            "}\n"));

        return files;
    }

    // ======================== Spring Data JPA 项目 ========================

    private List<ProjectFileDTO> buildJpaProject(String projectName) {
        List<ProjectFileDTO> files = new ArrayList<>();
        files.add(new ProjectFileDTO("pom.xml", basePom(projectName,
            dep("org.springframework.boot", "spring-boot-starter-data-jpa"),
            dep("com.h2database", "h2"))));
        files.add(new ProjectFileDTO("src/main/resources/application.properties",
            applicationProperties("spring.datasource.url=jdbc:h2:mem:testdb",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.h2.console.enabled=true",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=true")));
        files.add(appClass());

        files.add(new ProjectFileDTO("src/main/java/com/example/entity/User.java",
            "package com.example.entity;\n\n" +
            "import jakarta.persistence.*;\n\n" +
            "@Entity\n" +
            "@Table(name = \"users\")\n" +
            "public class User {\n" +
            "    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)\n" +
            "    private Long id;\n" +
            "    @Column(nullable = false) private String name;\n" +
            "    @Column(unique = true) private String email;\n\n" +
            "    public User() {}\n" +
            "    public User(String name, String email) { this.name = name; this.email = email; }\n" +
            "    public Long getId() { return id; } public void setId(Long id) { this.id = id; }\n" +
            "    public String getName() { return name; } public void setName(String name) { this.name = name; }\n" +
            "    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/entity/Order.java",
            "package com.example.entity;\n\n" +
            "import jakarta.persistence.*;\n" +
            "import java.math.BigDecimal;\n" +
            "import java.time.LocalDateTime;\n\n" +
            "@Entity\n" +
            "@Table(name = \"orders\")\n" +
            "public class Order {\n" +
            "    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)\n" +
            "    private Long id;\n" +
            "    @ManyToOne(fetch = FetchType.LAZY)\n" +
            "    @JoinColumn(name = \"user_id\")\n" +
            "    private User user;\n" +
            "    private BigDecimal amount;\n" +
            "    private LocalDateTime createTime;\n" +
            "    public Order() {}\n" +
            "    public Order(User user, BigDecimal amount) { this.user = user; this.amount = amount; this.createTime = LocalDateTime.now(); }\n" +
            "    public Long getId() { return id; } public void setId(Long id) { this.id = id; }\n" +
            "    public User getUser() { return user; } public void setUser(User user) { this.user = user; }\n" +
            "    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal amount) { this.amount = amount; }\n" +
            "    public LocalDateTime getCreateTime() { return createTime; }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/repository/UserRepository.java",
            "package com.example.repository;\n\n" +
            "import com.example.entity.User;\n" +
            "import org.springframework.data.jpa.repository.JpaRepository;\n" +
            "import java.util.List;\n\n" +
            "public interface UserRepository extends JpaRepository<User, Long> {\n" +
            "    List<User> findByNameContaining(String keyword);\n" +
            "    boolean existsByEmail(String email);\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/repository/OrderRepository.java",
            "package com.example.repository;\n\n" +
            "import com.example.entity.Order;\n" +
            "import org.springframework.data.jpa.repository.JpaRepository;\n" +
            "import org.springframework.data.jpa.repository.Query;\n" +
            "import java.util.List;\n\n" +
            "public interface OrderRepository extends JpaRepository<Order, Long> {\n" +
            "    @Query(\"SELECT o FROM Order o JOIN FETCH o.user WHERE o.user.id = :userId\")\n" +
            "    List<Order> findByUserIdWithUser(Long userId);\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/controller/UserController.java",
            "package com.example.controller;\n\n" +
            "import com.example.entity.User;\n" +
            "import com.example.repository.UserRepository;\n" +
            "import org.springframework.web.bind.annotation.*;\n" +
            "import java.util.List;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/users\")\n" +
            "public class UserController {\n" +
            "    private final UserRepository userRepository;\n" +
            "    public UserController(UserRepository userRepository) { this.userRepository = userRepository; }\n\n" +
            "    @PostMapping\n" +
            "    public User create(@RequestBody User user) { return userRepository.save(user); }\n\n" +
            "    @GetMapping\n" +
            "    public List<User> list() { return userRepository.findAll(); }\n\n" +
            "    @GetMapping(\"/{id}\")\n" +
            "    public User get(@PathVariable Long id) { return userRepository.findById(id).orElse(null); }\n\n" +
            "    @GetMapping(\"/search\")\n" +
            "    public List<User> search(@RequestParam String q) { return userRepository.findByNameContaining(q); }\n" +
            "}\n"));

        return files;
    }

    // ======================== MyBatis-Plus 项目 ========================

    private List<ProjectFileDTO> buildMybatisPlusProject(String projectName) {
        List<ProjectFileDTO> files = new ArrayList<>();
        files.add(new ProjectFileDTO("pom.xml", basePom(projectName,
            dep("com.baomidou", "mybatis-plus-spring-boot3-starter", "3.5.7"),
            dep("com.h2database", "h2"))));
        files.add(new ProjectFileDTO("src/main/resources/application.properties",
            applicationProperties("spring.datasource.url=jdbc:h2:mem:mpdb",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "mybatis-plus.mapper-locations=classpath:mapper/*.xml",
                "mybatis-plus.global-config.db-config.logic-delete-field=deleted",
                "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl")));
        files.add(appClass());

        files.add(new ProjectFileDTO("src/main/java/com/example/entity/User.java",
            "package com.example.entity;\n\n" +
            "import com.baomidou.mybatisplus.annotation.*;\n\n" +
            "@TableName(\"user\")\n" +
            "public class User {\n" +
            "    @TableId(type = IdType.AUTO) private Long id;\n" +
            "    private String name;\n" +
            "    private Integer age;\n" +
            "    private String email;\n" +
            "    @TableLogic private Integer deleted;\n" +
            "    @Version private Integer version;\n" +
            "    public Long getId() { return id; } public void setId(Long id) { this.id = id; }\n" +
            "    public String getName() { return name; } public void setName(String name) { this.name = name; }\n" +
            "    public Integer getAge() { return age; } public void setAge(Integer age) { this.age = age; }\n" +
            "    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/mapper/UserMapper.java",
            "package com.example.mapper;\n\n" +
            "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
            "import com.example.entity.User;\n" +
            "import org.apache.ibatis.annotations.Mapper;\n\n" +
            "@Mapper\n" +
            "public interface UserMapper extends BaseMapper<User> {}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/service/UserService.java",
            "package com.example.service;\n\n" +
            "import com.baomidou.mybatisplus.extension.service.IService;\n" +
            "import com.example.entity.User;\n\n" +
            "public interface UserService extends IService<User> {}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/service/impl/UserServiceImpl.java",
            "package com.example.service.impl;\n\n" +
            "import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;\n" +
            "import com.example.entity.User;\n" +
            "import com.example.mapper.UserMapper;\n" +
            "import com.example.service.UserService;\n" +
            "import org.springframework.stereotype.Service;\n\n" +
            "@Service\n" +
            "public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/controller/UserController.java",
            "package com.example.controller;\n\n" +
            "import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;\n" +
            "import com.baomidou.mybatisplus.extension.plugins.pagination.Page;\n" +
            "import com.example.entity.User;\n" +
            "import com.example.service.UserService;\n" +
            "import org.springframework.web.bind.annotation.*;\n" +
            "import java.util.List;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/users\")\n" +
            "public class UserController {\n" +
            "    private final UserService userService;\n" +
            "    public UserController(UserService userService) { this.userService = userService; }\n\n" +
            "    @PostMapping\n" +
            "    public User create(@RequestBody User user) { userService.save(user); return user; }\n\n" +
            "    @GetMapping\n" +
            "    public List<User> list() { return userService.list(); }\n\n" +
            "    @GetMapping(\"/page\")\n" +
            "    public Page<User> page(@RequestParam int current, @RequestParam int size) {\n" +
            "        return userService.page(new Page<>(current, size));\n" +
            "    }\n\n" +
            "    @GetMapping(\"/search\")\n" +
            "    public List<User> search(@RequestParam String name) {\n" +
            "        return userService.list(new LambdaQueryWrapper<User>().like(User::getName, name));\n" +
            "    }\n\n" +
            "    @DeleteMapping(\"/{id}\")\n" +
            "    public void delete(@PathVariable Long id) { userService.removeById(id); }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/config/MybatisPlusConfig.java",
            "package com.example.config;\n\n" +
            "import com.baomidou.mybatisplus.annotation.DbType;\n" +
            "import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;\n" +
            "import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;\n" +
            "import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;\n" +
            "import org.springframework.context.annotation.Bean;\n" +
            "import org.springframework.context.annotation.Configuration;\n\n" +
            "@Configuration\n" +
            "public class MybatisPlusConfig {\n" +
            "    @Bean\n" +
            "    public MybatisPlusInterceptor interceptor() {\n" +
            "        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();\n" +
            "        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));\n" +
            "        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());\n" +
            "        return interceptor;\n" +
            "    }\n" +
            "}\n"));

        return files;
    }

    // ======================== Spring Cache + Redis 项目 ========================

    private List<ProjectFileDTO> buildCacheRedisProject(String projectName) {
        List<ProjectFileDTO> files = new ArrayList<>();
        files.add(new ProjectFileDTO("pom.xml", basePom(projectName,
            dep("org.springframework.boot", "spring-boot-starter-cache"),
            dep("org.springframework.boot", "spring-boot-starter-data-redis"),
            dep("org.apache.commons", "commons-pool2"))));
        files.add(new ProjectFileDTO("src/main/resources/application.properties",
            applicationProperties("spring.data.redis.host=localhost", "spring.data.redis.port=6379",
                "spring.cache.type=redis", "spring.cache.redis.time-to-live=60000",
                "spring.cache.redis.cache-null-values=false")));
        files.add(appClass());

        files.add(new ProjectFileDTO("src/main/java/com/example/config/CacheConfig.java",
            "package com.example.config;\n\n" +
            "import org.springframework.cache.annotation.EnableCaching;\n" +
            "import org.springframework.context.annotation.Bean;\n" +
            "import org.springframework.context.annotation.Configuration;\n" +
            "import org.springframework.data.redis.cache.RedisCacheConfiguration;\n" +
            "import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;\n" +
            "import org.springframework.data.redis.serializer.RedisSerializationContext;\n" +
            "import java.time.Duration;\n\n" +
            "@Configuration\n" +
            "@EnableCaching\n" +
            "public class CacheConfig {\n" +
            "    @Bean\n" +
            "    public RedisCacheConfiguration cacheConfiguration() {\n" +
            "        return RedisCacheConfiguration.defaultCacheConfig()\n" +
            "            .entryTtl(Duration.ofMinutes(10))\n" +
            "            .disableCachingNullValues()\n" +
            "            .serializeValuesWith(RedisSerializationContext.SerializationPair\n" +
            "                .fromSerializer(new GenericJackson2JsonRedisSerializer()));\n" +
            "    }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/service/UserService.java",
            "package com.example.service;\n\n" +
            "import org.springframework.cache.annotation.CacheEvict;\n" +
            "import org.springframework.cache.annotation.CachePut;\n" +
            "import org.springframework.cache.annotation.Cacheable;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "import java.util.Map;\n" +
            "import java.util.concurrent.ConcurrentHashMap;\n\n" +
            "@Service\n" +
            "public class UserService {\n\n" +
            "    private final Map<Long, String> db = new ConcurrentHashMap<>();\n\n" +
            "    @Cacheable(value = \"users\", key = \"#id\")\n" +
            "    public String getUser(Long id) {\n" +
            "        simulateSlow();\n" +
            "        return db.getOrDefault(id, \"default\");\n" +
            "    }\n\n" +
            "    @CachePut(value = \"users\", key = \"#id\")\n" +
            "    public String updateUser(Long id, String name) {\n" +
            "        db.put(id, name);\n" +
            "        return name;\n" +
            "    }\n\n" +
            "    @CacheEvict(value = \"users\", key = \"#id\")\n" +
            "    public void deleteUser(Long id) { db.remove(id); }\n\n" +
            "    @CacheEvict(value = \"users\", allEntries = true)\n" +
            "    public void clearCache() {}\n\n" +
            "    private void simulateSlow() {\n" +
            "        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}\n" +
            "    }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/controller/UserController.java",
            "package com.example.controller;\n\n" +
            "import com.example.service.UserService;\n" +
            "import org.springframework.web.bind.annotation.*;\n" +
            "import java.util.Map;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/users\")\n" +
            "public class UserController {\n\n" +
            "    private final UserService userService;\n" +
            "    public UserController(UserService userService) { this.userService = userService; }\n\n" +
            "    @GetMapping(\"/{id}\")\n" +
            "    public Map<String, Object> get(@PathVariable Long id) {\n" +
            "        long start = System.currentTimeMillis();\n" +
            "        String user = userService.getUser(id);\n" +
            "        return Map.of(\"data\", user, \"elapsedMs\", System.currentTimeMillis() - start);\n" +
            "    }\n\n" +
            "    @PutMapping(\"/{id}\")\n" +
            "    public String update(@PathVariable Long id, @RequestBody Map<String, String> body) {\n" +
            "        return userService.updateUser(id, body.get(\"name\"));\n" +
            "    }\n\n" +
            "    @DeleteMapping(\"/{id}\")\n" +
            "    public String delete(@PathVariable Long id) {\n" +
            "        userService.deleteUser(id);\n" +
            "        return \"已删除，缓存已清理\";\n" +
            "    }\n" +
            "}\n"));

        return files;
    }

    // ======================== WebSocket 项目 ========================

    private List<ProjectFileDTO> buildWebsocketProject(String projectName) {
        List<ProjectFileDTO> files = new ArrayList<>();
        files.add(new ProjectFileDTO("pom.xml", basePom(projectName,
            dep("org.springframework.boot", "spring-boot-starter-websocket"))));
        files.add(new ProjectFileDTO("src/main/resources/application.properties",
            "server.port=8081\nspring.application.name=spring-boot-demo\n"));
        files.add(appClass());

        files.add(new ProjectFileDTO("src/main/java/com/example/config/WebSocketConfig.java",
            "package com.example.config;\n\n" +
            "import org.springframework.context.annotation.Configuration;\n" +
            "import org.springframework.messaging.simp.config.MessageBrokerRegistry;\n" +
            "import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;\n" +
            "import org.springframework.web.socket.config.annotation.StompEndpointRegistry;\n" +
            "import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;\n\n" +
            "@Configuration\n" +
            "@EnableWebSocketMessageBroker\n" +
            "public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {\n\n" +
            "    @Override\n" +
            "    public void configureMessageBroker(MessageBrokerRegistry config) {\n" +
            "        config.enableSimpleBroker(\"/topic\");\n" +
            "        config.setApplicationDestinationPrefixes(\"/app\");\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public void registerStompEndpoints(StompEndpointRegistry registry) {\n" +
            "        registry.addEndpoint(\"/ws\").setAllowedOriginPatterns(\"*\").withSockJS();\n" +
            "    }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/controller/ChatController.java",
            "package com.example.controller;\n\n" +
            "import org.springframework.messaging.handler.annotation.MessageMapping;\n" +
            "import org.springframework.messaging.handler.annotation.SendTo;\n" +
            "import org.springframework.stereotype.Controller;\n" +
            "import java.util.Map;\n\n" +
            "@Controller\n" +
            "public class ChatController {\n\n" +
            "    @MessageMapping(\"/chat.send\")\n" +
            "    @SendTo(\"/topic/messages\")\n" +
            "    public Map<String, String> sendMessage(Map<String, String> message) {\n" +
            "        String user = message.getOrDefault(\"user\", \"anonymous\");\n" +
            "        String content = message.getOrDefault(\"content\", \"\");\n" +
            "        return Map.of(\"user\", user, \"content\", content, \"timestamp\", String.valueOf(System.currentTimeMillis()));\n" +
            "    }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/resources/static/index.html",
            "<!DOCTYPE html>\n<html>\n<head>\n" +
            "<meta charset=\"UTF-8\"><title>WebSocket Chat</title>\n" +
            "<script src=\"https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js\"></script>\n" +
            "<script src=\"https://cdn.jsdelivr.net/npm/stompjs@2/lib/stomp.min.js\"></script>\n" +
            "</head>\n<body>\n" +
            "<h2>WebSocket 聊天室</h2>\n" +
            "<div><input id=\"userInput\" placeholder=\"用户名\" value=\"用户\" /></div>\n" +
            "<div><input id=\"msgInput\" placeholder=\"输入消息\" /><button onclick=\"send()\">发送</button></div>\n" +
            "<div id=\"messages\" style=\"margin-top:10px;border:1px solid #ccc;height:200px;overflow:auto;padding:10px;\"></div>\n" +
            "<script>\n" +
            "var socket = new SockJS('/ws');\n" +
            "var stompClient = Stomp.over(socket);\n" +
            "stompClient.connect({}, function() {\n" +
            "  stompClient.subscribe('/topic/messages', function(msg) {\n" +
            "    var data = JSON.parse(msg.body);\n" +
            "    document.getElementById('messages').innerHTML +=\n" +
            "      '<p><b>' + data.user + '</b>: ' + data.content + ' <small>' + data.timestamp + '</small></p>';\n" +
            "  });\n" +
            "});\n" +
            "function send() {\n" +
            "  var user = document.getElementById('userInput').value;\n" +
            "  var content = document.getElementById('msgInput').value;\n" +
            "  stompClient.send('/app/chat.send', {}, JSON.stringify({user: user, content: content}));\n" +
            "}\n" +
            "</script>\n</body>\n</html>\n"));

        return files;
    }

    // ======================== RabbitMQ 项目 ========================

    private List<ProjectFileDTO> buildRabbitmqProject(String projectName) {
        List<ProjectFileDTO> files = new ArrayList<>();
        files.add(new ProjectFileDTO("pom.xml", basePom(projectName,
            dep("org.springframework.boot", "spring-boot-starter-amqp"))));
        files.add(new ProjectFileDTO("src/main/resources/application.properties",
            applicationProperties("spring.rabbitmq.host=localhost", "spring.rabbitmq.port=5672",
                "spring.rabbitmq.username=guest", "spring.rabbitmq.password=guest")));
        files.add(appClass());

        files.add(new ProjectFileDTO("src/main/java/com/example/config/RabbitConfig.java",
            "package com.example.config;\n\n" +
            "import org.springframework.amqp.core.*;\n" +
            "import org.springframework.context.annotation.Bean;\n" +
            "import org.springframework.context.annotation.Configuration;\n\n" +
            "@Configuration\n" +
            "public class RabbitConfig {\n\n" +
            "    public static final String EXCHANGE = \"demo.exchange\";\n" +
            "    public static final String QUEUE = \"demo.queue\";\n" +
            "    public static final String ROUTING_KEY = \"demo.routing.#\";\n" +
            "    public static final String DLQ = \"demo.dlq\";\n" +
            "    public static final String DLX = \"demo.dlx\";\n\n" +
            "    @Bean\n" +
            "    public DirectExchange exchange() { return new DirectExchange(EXCHANGE); }\n\n" +
            "    @Bean\n" +
            "    public Queue queue() { return QueueBuilder.durable(QUEUE)\n" +
            "        .withArgument(\"x-dead-letter-exchange\", DLX)\n" +
            "        .withArgument(\"x-dead-letter-routing-key\", \"dead\")\n" +
            "        .build(); }\n\n" +
            "    @Bean\n" +
            "    public Binding binding() { return BindingBuilder.bind(queue()).to(exchange()).with(ROUTING_KEY); }\n\n" +
            "    @Bean\n" +
            "    public DirectExchange deadLetterExchange() { return new DirectExchange(DLX); }\n\n" +
            "    @Bean\n" +
            "    public Queue deadLetterQueue() { return QueueBuilder.durable(DLQ).build(); }\n\n" +
            "    @Bean\n" +
            "    public Binding deadLetterBinding() { return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(\"dead\"); }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/service/MessageProducer.java",
            "package com.example.service;\n\n" +
            "import org.springframework.amqp.rabbit.core.RabbitTemplate;\n" +
            "import org.springframework.stereotype.Service;\n\n" +
            "@Service\n" +
            "public class MessageProducer {\n\n" +
            "    private final RabbitTemplate rabbitTemplate;\n" +
            "    public MessageProducer(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }\n\n" +
            "    public void send(String message) {\n" +
            "        rabbitTemplate.convertAndSend(\"demo.exchange\", \"demo.routing.key\", message);\n" +
            "        System.out.println(\" [x] 已发送: \" + message);\n" +
            "    }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/service/MessageConsumer.java",
            "package com.example.service;\n\n" +
            "import org.springframework.amqp.rabbit.annotation.RabbitListener;\n" +
            "import org.springframework.stereotype.Component;\n\n" +
            "@Component\n" +
            "public class MessageConsumer {\n\n" +
            "    @RabbitListener(queues = \"demo.queue\")\n" +
            "    public void receive(String message) {\n" +
            "        System.out.println(\" [✓] 已消费: \" + message);\n" +
            "    }\n\n" +
            "    @RabbitListener(queues = \"demo.dlq\")\n" +
            "    public void receiveDead(String message) {\n" +
            "        System.out.println(\"[☠] 死信: \" + message);\n" +
            "    }\n" +
            "}\n"));

        files.add(new ProjectFileDTO("src/main/java/com/example/controller/MessageController.java",
            "package com.example.controller;\n\n" +
            "import com.example.service.MessageProducer;\n" +
            "import org.springframework.web.bind.annotation.*;\n" +
            "import java.util.Map;\n\n" +
            "@RestController\n" +
            "@RequestMapping(\"/api/mq\")\n" +
            "public class MessageController {\n\n" +
            "    private final MessageProducer producer;\n" +
            "    public MessageController(MessageProducer producer) { this.producer = producer; }\n\n" +
            "    @PostMapping(\"/send\")\n" +
            "    public Map<String, String> send(@RequestBody Map<String, String> body) {\n" +
            "        String msg = body.getOrDefault(\"message\", \"hello\");\n" +
            "        producer.send(msg);\n" +
            "        return Map.of(\"status\", \"sent\", \"message\", msg);\n" +
            "    }\n" +
            "}\n"));

        return files;
    }
}