package edu.scau.vms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI vmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VMS 志愿服务工时认证与活动管理系统 API")
                        .version("v0.5.0 - S5 收尾")
                        .description("Sprint 5 范围：文件上传 + 证书 PDF（iText 7） + 对外综测 RESTful API + DataSeeder ≥2000 条。")
                        .contact(new Contact()
                                .name("24软工4班数据库课设第七组")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录后获得的 JWT，请填入 Bearer Token")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
