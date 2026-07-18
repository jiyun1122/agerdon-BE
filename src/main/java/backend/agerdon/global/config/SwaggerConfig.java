package backend.agerdon.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        // 1. 사용할 보안 스키마의 키값 정의
        String securityJwtName = "JWT_Authentication";

        // 2. 기본적으로 모든 API 요구사항에 이 JWT 스키마를 연결
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityJwtName);

        Components components = new Components().addSecuritySchemes(securityJwtName,
                new SecurityScheme()
                        .name(securityJwtName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("발급받은 Access Token을 입력해 주세요. (Bearer 제외하고 토큰 값만 입력)"));

        return new OpenAPI()
                .info(new Info()
                        .title("Project API 명세서")
                        .description("애거돈 API 명세서를 위한 API 문서입니다.")
                        .version("v1.0.0"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
