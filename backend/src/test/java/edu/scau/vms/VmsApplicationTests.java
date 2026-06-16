package edu.scau.vms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring 上下文加载冒烟测试。
 * 注意：本测试需要本地 MySQL 可用且 application-dev.yml 配置正确，
 * 否则数据源初始化会失败。CI 中可通过 -Dspring.profiles.active=test 切换 H2 内存库。
 */
@SpringBootTest
class VmsApplicationTests {

    @Test
    void contextLoads() {
        // 上下文能起即视为通过；不写断言避免与 DB 状态耦合
    }
}
