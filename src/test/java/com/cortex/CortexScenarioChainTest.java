package com.cortex;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cortex.auth.JwtService;
import com.cortex.llm.LlmClient;
import com.cortex.llm.LlmRequest;
import com.cortex.llm.LlmResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:cortex-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.sql.init.mode=always",
    "cortex.llm.default-config.api-key=test-mock-key",
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CortexScenarioChainTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private LlmClient llmClient;

    private String baseUrl;
    private String adminToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        adminToken = jwtService.generateToken("admin", "ROOT", "default-org");
        reset(llmClient);

        // 通用Fallback Mock：任何未特化覆盖的LLM调用返回合法JSON或Mock对象
        lenient().when(llmClient.chatSimple(anyString())).thenReturn("{}");
        lenient().when(llmClient.chatSimple(anyString(), anyString())).thenReturn("{}");
        lenient().when(llmClient.chatSimple(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("{}");
        lenient().when(llmClient.chat(any(LlmRequest.class))).thenReturn(makeLlmResponse("Mock response"));
        lenient().when(llmClient.chat(anyString(), anyString(), any(LlmRequest.class))).thenReturn(makeLlmResponse("Mock response"));
    }

    // ==================== 辅助方法 ====================

    private LlmResponse makeLlmResponse(String content) {
        LlmResponse resp = new LlmResponse();
        LlmResponse.Choice choice = new LlmResponse.Choice();
        LlmResponse.Message msg = new LlmResponse.Message();
        msg.setRole("assistant");
        msg.setContent(content);
        choice.setMessage(msg);
        resp.setChoices(new LlmResponse.Choice[]{choice});
        return resp;
    }

    @SuppressWarnings("unchecked")
    private JSONObject parseBody(HttpEntity<String> resp) {
        return JSON.parseObject(resp.getBody());
    }

    private void assertOk(JSONObject body) {
        assertNotNull(body, "Response body is null");
        assertEquals(200, body.getIntValue("code"), "API returned error response: " + body);
    }

    private ResponseEntity<String> doGet(String path) {
        return rest.getForEntity(baseUrl + path, String.class);
    }

    private ResponseEntity<String> doGetWithToken(String path, String token) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) h.setBearerAuth(token);
        return rest.exchange(baseUrl + path, HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    private ResponseEntity<String> doPost(String path, Object body) {
        return doPostWithToken(path, body, null);
    }

    private ResponseEntity<String> doPostWithToken(String path, Object body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return rest.exchange(baseUrl + path, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
    }

    private ResponseEntity<String> doPut(String path, Object body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return rest.exchange(baseUrl + path, HttpMethod.PUT, new HttpEntity<>(body, h), String.class);
    }

    private ResponseEntity<String> doDelete(String path, String token) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) h.setBearerAuth(token);
        return rest.exchange(baseUrl + path, HttpMethod.DELETE, new HttpEntity<>(h), String.class);
    }

    private ResponseEntity<String> doPostWithApiKey(String path, Object body, String apiKey) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null) h.set("X-API-Key", apiKey);
        return rest.exchange(baseUrl + path, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
    }

    private ResponseEntity<String> doGetWithApiKey(String path, String apiKey) {
        HttpHeaders h = new HttpHeaders();
        if (apiKey != null) h.set("X-API-Key", apiKey);
        return rest.exchange(baseUrl + path, HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    // ==================================================================
    //  场景一：用户学习全链路（注册→知识树→场景→对话→题库→卡片→报告→登出）
    // ==================================================================
    @Test
    @Order(1)
    void scenario1_userLearningJourney() {
        // ---------- ① 注册 ----------
        JSONObject reg = parseBody(doPost("/api/auth/register", Map.of(
            "username", "learner1", "password", "pass123", "email", "l1@test.com")));
        assertOk(reg);
        String userId = reg.getJSONObject("data").getString("userId");
        String token = reg.getJSONObject("data").getString("token");
        assertNotNull(userId);
        assertNotNull(token);

        // ---------- ② 知识树 ----------
        JSONObject tree = parseBody(doGet("/api/lab/knowledge-tree"));
        assertOk(tree);
        assertFalse(tree.getJSONArray("data").isEmpty());

        // ---------- ③ 从树节点生成陷阱代码（java-basics-datatype已有映射场景，不走LLM） ----------
        JSONObject gen = parseBody(doPostWithToken("/api/lab/knowledge-tree/generate",
            Map.of("nodeId", "java-basics-datatype"), token));
        assertOk(gen);
        assertNotNull(gen.getJSONObject("data").getString("trapCode"));

        // ---------- ④ 启动学习会话 ----------
        long scenarioId = gen.getJSONObject("data").getLongValue("id");
        JSONObject sess = parseBody(doPostWithToken("/api/lab/session/start",
            Map.of("scenarioId", String.valueOf(scenarioId), "userId", userId), token));
        assertOk(sess);
        String sessionId = sess.getJSONObject("data").getString("sessionId");

        // ---------- ⑤ AI苏格拉底对话 ----------
        when(llmClient.chatSimple(contains("苏格拉底"), anyString()))
            .thenReturn("{\"reply\":\"运行代码观察输出，你发现了什么？\",\"hintLevel\":\"OBSERVE\",\"enlightened\":false,\"suggestions\":[\"观察输出\"]}");
        JSONObject chat = parseBody(doPostWithToken("/api/lab/chat",
            Map.of("sessionId", sessionId, "message", "我运行了代码"), token));
        assertOk(chat);
        assertNotNull(chat.getJSONObject("data").getString("reply"));

        // ---------- ⑥ 列出题目 ----------
        JSONObject qList = parseBody(doGetWithToken("/api/lab/questions?userId=" + userId, token));
        assertOk(qList);
        assertFalse(qList.getJSONArray("data").isEmpty());
        long qId = qList.getJSONArray("data").getJSONObject(0).getLongValue("id");

        // ---------- ⑦ 查看题目详情 ----------
        JSONObject qDetail = parseBody(doGetWithToken("/api/lab/questions/" + qId + "?userId=" + userId, token));
        assertOk(qDetail);
        assertEquals(qId, qDetail.getJSONObject("data").getLongValue("id"));

        // ---------- ⑧ 生成知识卡片 ----------
        when(llmClient.chatSimple(contains("知识卡片")))
            .thenReturn("{\"title\":\"缓存池\",\"keyPoints\":\"-128~127|缓存复用\",\"detailExplanation\":\"Integer缓存\",\"codeSnippet\":\"Integer a=100;\",\"commonPitfalls\":\"==比较引用\"}");
        JSONObject card = parseBody(doPostWithToken("/api/lab/questions/" + qId + "/card/generate", Map.of(), token));
        assertOk(card);
        assertNotNull(card.getJSONObject("data").getString("keyPoints"));

        // ---------- ⑨ 标记掌握 ----------
        JSONObject mastered = parseBody(doPostWithToken("/api/lab/questions/" + qId + "/mastered",
            Map.of("userId", userId, "mastered", true), token));
        assertOk(mastered);

        // ---------- ⑩ 获取待复习列表 ----------
        JSONObject reviewDue = parseBody(doGetWithToken("/api/lab/questions/review/due?userId=" + userId, token));
        assertOk(reviewDue);

        // ---------- ⑪ 提交复习结果 ----------
        JSONObject review = parseBody(doPostWithToken("/api/lab/questions/review",
            Map.of("questionId", qId, "userId", userId, "stillMastered", true), token));
        assertOk(review);

        // ---------- ⑫ 获取学习报告 ----------
        JSONObject report = parseBody(doGetWithToken("/api/auth/report?userId=" + userId + "&type=WEEKLY", token));
        assertOk(report);

        // ---------- ⑭ 标记知识树节点掌握 ----------
        JSONObject treeMaster = parseBody(doPostWithToken("/api/lab/knowledge-tree/master",
            Map.of("nodeId", "java-basics-datatype", "userId", userId, "mastered", "true"), token));
        assertOk(treeMaster);
        JSONObject masteredNodes = parseBody(doGetWithToken("/api/lab/knowledge-tree/mastered?userId=" + userId, token));
        assertOk(masteredNodes);
        assertTrue(masteredNodes.getJSONArray("data").contains("java-basics-datatype"));

        // ---------- ⑮ 导出能力图谱 ----------
        // export返回ResponseEntity<byte[]>文件下载，非ApiResponse
        ResponseEntity<String> exportResp = doGet("/api/export/capability?userId=" + userId);
        assertEquals(200, exportResp.getStatusCodeValue());
        assertNotNull(exportResp.getBody());
        JSONObject capData = JSON.parseObject(exportResp.getBody());
        assertNotNull(capData.getString("exportTime"));

        // ---------- ⑯ 登出 ----------
        JSONObject logout = parseBody(doPostWithToken("/api/auth/logout", Map.of(), token));
        assertOk(logout);
    }

    // ==================================================================
    //  场景二：AI助手对话 & 社区陷阱众包
    // ==================================================================
    @Test
    @Order(2)
    void scenario2_assistantAndCommunity() {
        // 登录
        JSONObject login = parseBody(doPost("/api/auth/login", Map.of("username", "admin", "password", "admin123")));
        assertOk(login);
        String token = login.getJSONObject("data").getString("token");
        String userId = login.getJSONObject("data").getString("userId");

        // ---------- AI助手流程 ----------

        // ① 获取AI配置
        JSONObject cfg = parseBody(doGet("/api/lab/assistant/config"));
        assertOk(cfg);
        assertNotNull(cfg.getJSONObject("data").getString("vendor"));

        // ② 更新AI配置
        JSONObject updCfg = parseBody(doPut("/api/lab/assistant/config",
            Map.of("temperature", "0.8"), token));
        assertOk(updCfg);
        assertEquals("0.8", updCfg.getJSONObject("data").getString("temperature"));

        // ③ AI对话（触发RAG+进化）
        when(llmClient.chatSimple(anyString(), anyString(), anyString(), contains("小C"), anyString()))
            .thenReturn("{\"reply\":\"Integer缓存池范围是-128到127。\",\"action\":null,\"suggestions\":[\"试试其他值\"]}");
        // RAG提取关键词
        when(llmClient.chatSimple(contains("提取")))
            .thenReturn("Integer,缓存,比较");
        // 进化洞察 + 知识索引
        when(llmClient.chatSimple(contains("洞察")))
            .thenReturn("缓存池概念很重要||==比较引用地址||注意自动装箱范围");
        when(llmClient.chatSimple(contains("知识点")))
            .thenReturn("Integer缓存池机制");

        JSONObject aiChat = parseBody(doPostWithToken("/api/lab/assistant/chat",
            Map.of("message", "解释Integer缓存", "userId", userId), token));
        assertOk(aiChat);
        String convId = aiChat.getJSONObject("data").getString("conversationId");

        // ④ 列出对话列表
        JSONObject convs = parseBody(doGetWithToken("/api/lab/assistant/conversations?userId=" + userId, token));
        assertOk(convs);
        assertFalse(convs.getJSONArray("data").isEmpty());

        // ⑤ 查看对话消息
        JSONObject msgs = parseBody(doGetWithToken("/api/lab/assistant/conversations/" + convId + "/messages", token));
        assertOk(msgs);
        assertFalse(msgs.getJSONArray("data").isEmpty());

        // ⑥ 提交反馈
        JSONObject feedback = parseBody(doPostWithToken("/api/lab/assistant/feedback",
            Map.of("conversationId", convId, "rating", 5, "userId", userId, "comment", "很棒"), token));
        assertOk(feedback);

        // ⑦ 删除对话
        JSONObject delConv = parseBody(doDelete("/api/lab/assistant/conversations/" + convId, token));
        assertOk(delConv);

        // ---------- 社区陷阱流程 ----------

        // ⑧ 提交社区陷阱
        JSONObject submit = parseBody(doPostWithToken("/api/lab/community/submit",
            Map.of("title", "测测试试-浮点数精度陷阱",
                   "knowledgePoint", "浮点数精度",
                   "category", "Java核心",
                   "trapCode", "public class FloatTrap {\n    public static void main(String[] args) {\n        double a = 0.1 + 0.2;\n        System.out.println(a);\n    }\n}",
                   "expectedPitfall", "0.1+0.2不等于0.3",
                   "correctExplanation", "浮点数二进制精度问题",
                   "submitter", userId),
            token));
        assertOk(submit);
        Long trapId = submit.getJSONObject("data").getLong("id");

        // ⑨ 列出社区陷阱
        JSONObject traps = parseBody(doGetWithToken("/api/lab/community/traps?status=all&category=all", token));
        assertOk(traps);
        assertFalse(traps.getJSONArray("data").isEmpty());

        // ⑩ 查看陷阱详情
        JSONObject trapDetail = parseBody(doGetWithToken("/api/lab/community/traps/" + trapId, token));
        assertOk(trapDetail);
        assertEquals("PENDING", trapDetail.getJSONObject("data").getString("status"));

        // ⑪ 投票
        JSONObject vote = parseBody(doPostWithToken("/api/lab/community/traps/" + trapId + "/vote", Map.of(), token));
        assertOk(vote);

        // ⑫ 审核通过
        JSONObject approve = parseBody(doPostWithToken("/api/lab/community/traps/" + trapId + "/approve", Map.of(), token));
        assertOk(approve);
        assertEquals("APPROVED", approve.getJSONObject("data").getString("status"));

        // ⑬ 导入题库
        JSONObject integrate = parseBody(doPostWithToken("/api/lab/community/traps/" + trapId + "/integrate", Map.of(), token));
        assertOk(integrate);

        // ⑭ 搜索题目（含刚导入的）
        JSONObject search = parseBody(doGetWithToken("/api/lab/questions/search?keyword=浮点数", token));
        assertOk(search);

        // ⑮ 添加讨论
        Long searchQId = search.getJSONArray("data").getJSONObject(0).getLongValue("id");
        JSONObject disc = parseBody(doPostWithToken("/api/lab/questions/" + searchQId + "/discussions",
            Map.of("content", "这个陷阱很有启发性", "userId", userId), token));
        assertOk(disc);
        Long discId = disc.getJSONObject("data").getLong("id");

        // ⑯ 获取讨论列表
        JSONObject discs = parseBody(doGetWithToken("/api/lab/questions/" + searchQId + "/discussions", token));
        assertOk(discs);
        assertFalse(discs.getJSONArray("data").isEmpty());

        // ⑰ 删除评论
        JSONObject delDisc = parseBody(doDelete("/api/lab/discussions/" + discId, token));
        assertOk(delDisc);
    }

    // ==================================================================
    //  场景三：多智能体任务协作
    // ==================================================================
    @Test
    @Order(3)
    void scenario3_multiAgentTask() {
        // 注册专用用户（避免依赖其他测试的登录状态）
        JSONObject reg = parseBody(doPost("/api/auth/register", Map.of(
            "username", "agentUser", "password", "pass123", "email", "agent@test.com")));
        assertOk(reg);
        String token = reg.getJSONObject("data").getString("token");
        String userId = reg.getJSONObject("data").getString("userId");

        // ---------- ① 列出Agent ----------
        JSONObject agents = parseBody(doGet("/api/agents"));
        assertOk(agents);
        assertFalse(agents.getJSONArray("data").isEmpty());

        // ---------- ② 获取用户画像（在core包） ----------
        JSONObject profile = parseBody(doGet("/api/profile/" + userId));
        assertOk(profile);

        // ---------- ③ 添加性格标签 ----------
        JSONObject tag = parseBody(doPostWithToken("/api/profile/" + userId + "/tags",
            Map.of("tag", "细心"), token));
        assertOk(tag);

        // ---------- ④ 添加工作习惯 ----------
        JSONObject habit = parseBody(doPostWithToken("/api/profile/" + userId + "/habits",
            Map.of("habit", "先读文档"), token));
        assertOk(habit);

        // ---------- ⑤ 添加错误记录 ----------
        JSONObject mistake = parseBody(doPostWithToken("/api/profile/" + userId + "/mistakes",
            Map.of("keyword", "Integer缓存", "description", "误用==比较", "taskId", "test-001"), token));
        assertOk(mistake);

        // ---------- ⑥ 创建任务（LLM分解） ----------
        when(llmClient.chatSimple(contains("任务分解")))
            .thenReturn("{\"title\":\"分析Java代码\",\"nodes\":[{\"nodeId\":\"node_1\",\"title\":\"分析代码\",\"description\":\"分析Integer缓存\",\"capability\":\"code-analysis\",\"dependencies\":[]},{\"nodeId\":\"node_2\",\"title\":\"生成报告\",\"description\":\"生成分析报告\",\"capability\":\"report\",\"dependencies\":[\"node_1\"]}]}");

        String requirement = "分析一段Java代码，找出其中的Integer缓存陷阱，并生成改进报告";
        JSONObject createTask = parseBody(doPostWithToken("/api/task/create",
            Map.of("userId", userId, "requirement", requirement), token));
        assertOk(createTask);
        String taskId = createTask.getJSONObject("data").getString("taskId");

        // ---------- ⑦ 执行任务（Agent执行） ----------
        JSONObject execTask = parseBody(doPostWithToken("/api/task/execute",
            Map.of("taskId", taskId, "userId", userId), token));
        assertOk(execTask);

        // ---------- ⑧ 获取任务结果 ----------
        JSONObject taskResult = parseBody(doGetWithToken("/api/task/" + taskId, token));
        assertOk(taskResult);
        assertEquals("COMPLETED", taskResult.getJSONObject("data").getString("status"));
        assertNotNull(taskResult.getJSONObject("data").getString("result"));
    }

    // ==================================================================
    //  场景四：代码沙箱 & Spring项目 & 个人设置
    // ==================================================================
    @Test
    @Order(4)
    void scenario4_sandboxAndProfile() {
        // 登录
        JSONObject login = parseBody(doPost("/api/auth/login", Map.of("username", "admin", "password", "admin123")));
        assertOk(login);
        String token = login.getJSONObject("data").getString("token");
        String userId = login.getJSONObject("data").getString("userId");

        // ---------- ① 代码沙箱执行 ----------
        JSONObject exec = parseBody(doPostWithToken("/api/lab/execute",
            Map.of("code", "public class Hello {\n    public static void main(String[] args) {\n        System.out.println(\"Hello Cortex!\");\n    }\n}"), token));
        assertOk(exec);
        JSONObject execData = exec.getJSONObject("data");
        if (execData.getBooleanValue("success")) {
            assertTrue(execData.getString("stdout").contains("Hello Cortex!"));
        }

        // ---------- ② 生成Spring项目（spring-ioc是project类型节点） ----------
        JSONObject proj = parseBody(doPostWithToken("/api/lab/knowledge-tree/project",
            Map.of("nodeId", "spring-ioc"), token));
        assertOk(proj);

        // ---------- ③ 导入题目 ----------
        JSONObject imp = parseBody(doPostWithToken("/api/lab/questions/import",
            Map.of("title", "自定义测试题",
                   "description", "测试导入功能",
                   "trapCode", "public class Test { public static void main(String[] args) { } }",
                   "expectedPitfall", "无输出",
                   "correctExplanation", "没有打印语句"), token));
        assertOk(imp);
        Long qId = imp.getJSONObject("data").getLong("id");

        // ---------- ④ 批量导入 ----------
        String batchText = "## 题目\n批量测试题\n\n## 代码\npublic class Batch {}\n\n## 预期\n编译通过\n\n## 解释\n正确代码\n";
        JSONObject batchImp = parseBody(doPostWithToken("/api/lab/questions/batch-import",
            Map.of("text", batchText, "category", "批量测试"), token));
        assertOk(batchImp);

        // ---------- ⑤ 列出知识卡片 ----------
        JSONObject cards = parseBody(doGetWithToken("/api/lab/cards", token));
        assertOk(cards);

        // ---------- ⑥ 获取卡片详情 ----------
        // 先新生成一张卡片
        when(llmClient.chatSimple(contains("知识整理专家")))
            .thenReturn("{\"title\":\"测试卡\",\"keyPoints\":\"点1|点2\",\"detailExplanation\":\"一句话\",\"codeSnippet\":\"int a=1;\",\"commonPitfalls\":\"误区1\"}");
        JSONObject newCard = parseBody(doPostWithToken("/api/lab/questions/" + qId + "/card/generate", Map.of(), token));
        assertOk(newCard);
        Long cardId = newCard.getJSONObject("data").getLong("id");

        // 获取卡片
        JSONObject getCard = parseBody(doGetWithToken("/api/lab/questions/" + qId + "/card", token));
        assertOk(getCard);

        // ---------- ⑦ 更新卡片 ----------
        JSONObject updCard = parseBody(doPut("/api/lab/cards/" + cardId,
            Map.of("title", "更新标题", "keyPoints", "新要点"), token));
        assertOk(updCard);
        assertEquals("更新标题", updCard.getJSONObject("data").getString("title"));

        // ---------- ⑧ 删除卡片 ----------
        JSONObject delCard = parseBody(doDelete("/api/lab/cards/" + cardId, token));
        assertOk(delCard);

        // ---------- ⑨ 删除题目 ----------
        JSONObject delQ = parseBody(doDelete("/api/lab/questions/" + qId, token));
        assertOk(delQ);

        // ---------- ⑩ 记录答题 ----------
        JSONObject recAns = parseBody(doPostWithToken("/api/auth/record-answer",
            Map.of("userId", userId, "correct", true), token));
        assertOk(recAns);

        // ---------- ⑪ 导出学习数据 ----------
        ResponseEntity<String> exportResp = doGetWithToken("/api/auth/export?userId=" + userId, token);
        assertEquals(200, exportResp.getStatusCodeValue());

        // ---------- ⑫ 获取通知配置 ----------
        JSONObject notifCfg = parseBody(doGetWithToken("/api/auth/notification/config?userId=" + userId, token));
        assertOk(notifCfg);

        // ---------- ⑬ 更新通知配置 ----------
        JSONObject updNotif = parseBody(doPut("/api/auth/notification/config",
            Map.of("userId", userId, "emailNotifications", true, "reviewReminder", true), token));
        assertOk(updNotif);
    }

    // ==================================================================
    //  场景五：面试评估全流程
    // ==================================================================
    @Test
    @Order(5)
    void scenario5_interviewJourney() {
        // 登录
        JSONObject login = parseBody(doPost("/api/auth/login", Map.of("username", "admin", "password", "admin123")));
        assertOk(login);
        String token = login.getJSONObject("data").getString("token");

        // ---------- ① 创建面试配置 ----------
        JSONObject cfg = parseBody(doPostWithToken("/api/interview/config/create",
            Map.of("title", "Java基础面试", "timeLimitMinutes", 30, "passingScore", 60, "questionCount", 2), token));
        assertOk(cfg);
        String configId = cfg.getJSONObject("data").getString("configId");

        // ---------- ② 列出面试配置 ----------
        JSONObject cfgs = parseBody(doGetWithToken("/api/interview/configs", token));
        assertOk(cfgs);

        // ---------- ③ 获取配置详情 ----------
        JSONObject cfgDetail = parseBody(doGetWithToken("/api/interview/config/" + configId, token));
        assertOk(cfgDetail);

        // ---------- ④ 开始面试 ----------
        JSONObject start = parseBody(doPostWithToken("/api/interview/start",
            Map.of("configId", configId), token));
        assertOk(start);
        String sessionId = start.getJSONObject("data").getString("sessionId");

        // ---------- ⑤ 获取当前题目 ----------
        JSONObject q1 = parseBody(doGetWithToken("/api/interview/session/" + sessionId, token));
        assertOk(q1);
        assertNotNull(q1.getJSONObject("data").getString("title"));

        // ---------- ⑥ 提交答案（AI评分） ----------
        when(llmClient.chatSimple(eq(""), eq(""), eq("deepseek-chat"), anyString(), eq("请评分")))
            .thenReturn("{\"score\":85,\"comment\":\"代码正确\",\"isCorrect\":true}");

        JSONObject ans1 = parseBody(doPostWithToken("/api/interview/session/" + sessionId + "/submit",
            Map.of("code", "public class Test {\n    public static void main(String[] args) {\n        System.out.println(\"test\");\n    }\n}", "timeSpentSeconds", 120), token));
        assertOk(ans1);

        // ---------- ⑦ 下一题 ----------
        JSONObject next = parseBody(doPostWithToken("/api/interview/session/" + sessionId + "/next", Map.of(), token));
        assertOk(next);

        // ---------- ⑧ 提交第二题 ----------
        JSONObject ans2 = parseBody(doPostWithToken("/api/interview/session/" + sessionId + "/submit",
            Map.of("code", "public class Test2 { public static void main(String[] args) {} }", "timeSpentSeconds", 60), token));
        assertOk(ans2);

        // ---------- ⑨ 结束面试（AI生成报告） ----------
        when(llmClient.chatSimple(eq(""), eq(""), eq("deepseek-chat"), anyString(), eq("请分析")))
            .thenReturn("{\"strengthAreas\":\"Java基础扎实\",\"weakAreas\":\"需加强并发\",\"summary\":\"表现良好\",\"suggestions\":\"继续加油\"}");

        JSONObject finish = parseBody(doPostWithToken("/api/interview/session/" + sessionId + "/finish", Map.of(), token));
        assertOk(finish);

        // ---------- ⑩ 获取报告 ----------
        JSONObject report = parseBody(doGetWithToken("/api/interview/session/" + sessionId + "/report", token));
        assertOk(report);
        assertNotNull(report.getJSONObject("data").getJSONObject("report"));

        // ---------- ⑪ 获取面试历史 ----------
        JSONObject history = parseBody(doGetWithToken("/api/interview/history", token));
        assertOk(history);
        assertFalse(history.getJSONArray("data").isEmpty());
    }

    // ==================================================================
    //  场景六：企业管理员（JWT认证）
    // ==================================================================
    @Test
    @Order(6)
    void scenario6_enterpriseAdmin() {
        // ---------- ① 获取组织信息（JWT） ----------
        JSONObject org = parseBody(doGetWithToken("/api/admin/org/info", adminToken));
        assertOk(org);
        assertEquals("default-org", org.getJSONObject("data").getString("orgId"));

        // ---------- ② 更新组织 ----------
        JSONObject updOrg = parseBody(doPut("/api/admin/org/update",
            Map.of("orgName", "测试组织", "description", "测试描述"), adminToken));
        assertOk(updOrg);

        // ---------- ③ 列出成员 ----------
        JSONObject members = parseBody(doGetWithToken("/api/admin/org/members", adminToken));
        assertOk(members);
        assertFalse(members.getJSONArray("data").isEmpty());

        // ---------- ④ 添加成员 ----------
        JSONObject addMember = parseBody(doPostWithToken("/api/admin/org/member/add",
            Map.of("username", "newuser", "password", "pass123", "displayName", "新用户", "email", "new@test.com"), adminToken));
        assertOk(addMember);

        // ---------- ⑤ 更新成员角色 ----------
        // 先获取刚添加成员的userId
        JSONObject membersAfter = parseBody(doGetWithToken("/api/admin/org/members", adminToken));
        String newUserId = membersAfter.getJSONArray("data").stream()
            .map(o -> (JSONObject) o)
            .filter(m -> "newuser".equals(m.getString("username")))
            .findFirst().get().getString("userId");
        JSONObject updRole = parseBody(doPut("/api/admin/org/member/role",
            Map.of("userId", newUserId, "role", "MEMBER"), adminToken));
        assertOk(updRole);

        // ---------- ⑥ 统计概览 ----------
        JSONObject stats = parseBody(doGetWithToken("/api/admin/stats/overview", adminToken));
        assertOk(stats);

        // ---------- ⑦ 生成培训报告 ----------
        JSONObject genRep = parseBody(doPostWithToken("/api/admin/report/generate",
            Map.of("reportType", "WEEKLY"), adminToken));
        assertOk(genRep);
        String reportId = genRep.getJSONObject("data").getString("reportId");

        // ---------- ⑧ 列出报告 ----------
        JSONObject reports = parseBody(doGetWithToken("/api/admin/reports", adminToken));
        assertOk(reports);
        assertFalse(reports.getJSONArray("data").isEmpty());

        // ---------- ⑨ 获取报告详情 ----------
        JSONObject repDetail = parseBody(doGetWithToken("/api/admin/report/" + reportId, adminToken));
        assertOk(repDetail);

        // ---------- ⑩ 创建API密钥 ----------
        JSONObject ak = parseBody(doPostWithToken("/api/admin/apikey/create",
            Map.of("keyName", "测试密钥"), adminToken));
        assertOk(ak);
        String apiKey = ak.getJSONObject("data").getString("apiKey");

        // ---------- ⑪ 列出API密钥 ----------
        JSONObject aks = parseBody(doGetWithToken("/api/admin/apikeys", adminToken));
        assertOk(aks);

        // ---------- ⑫ 使用API Key调用外部接口 ----------
        // V1接口需要X-API-Key头认证
        JSONObject cand = parseBody(doPostWithApiKey("/api/v1/candidates/create",
            Map.of("name", "张三", "email", "zhangsan@test.com", "position", "Java开发"), apiKey));
        assertOk(cand);
        String candidateId = cand.getJSONObject("data").getString("candidateId");

        // 创建面试配置用于V1接口
        Map<String, Object> v1Config = Map.of(
            "title", "V1面试", "questionCount", 1, "timeLimitMinutes", 30, "passingScore", 60);
        JSONObject v1Cfg = parseBody(doPostWithToken("/api/interview/config/create", v1Config, adminToken));
        assertOk(v1Cfg);
        String v1ConfigId = v1Cfg.getJSONObject("data").getString("configId");

        // 使用X-API-Key调用V1接口
        JSONObject v1Interview = parseBody(doPostWithApiKey("/api/v1/interviews/start",
            Map.of("candidateId", candidateId, "configId", v1ConfigId), apiKey));
        assertOk(v1Interview);
        String v1SessionId = v1Interview.getJSONObject("data").getString("sessionId");

        assertEquals("INVITED", v1Interview.getJSONObject("data").getString("status"));

        // 获取面试结果（X-API-Key）
        JSONObject v1Result = parseBody(doGetWithApiKey("/api/v1/interviews/" + v1SessionId + "/result", apiKey));
        assertOk(v1Result);

        // ---------- ⑬ 删除成员 ----------
        JSONObject rmMember = parseBody(doPostWithToken("/api/admin/org/member/remove",
            Map.of("userId", newUserId), adminToken));
        assertOk(rmMember);

        // ---------- ⑭ 删除报告 ----------
        JSONObject delRep = parseBody(doDelete("/api/admin/report/" + reportId, adminToken));
        assertOk(delRep);

        // ---------- ⑮ 删除API密钥 ----------
        JSONObject delAk = parseBody(doDelete("/api/admin/apikey/" + apiKey, adminToken));
        assertOk(delAk);
    }
}
