package com.example.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/* 
 * 
 * pom.xml 디펜던시 추가
 * -----------------------------------------------------------------
		<!-- 2022-10-23 오후 4:47 삽입 -->
	    <dependency>
	      <groupId>io.springfox</groupId>
	      <artifactId>springfox-swagger-ui</artifactId>
	      <version>2.9.2</version>
	    </dependency>

	    <dependency>
	      <groupId>io.springfox</groupId>
	      <artifactId>springfox-swagger2</artifactId>
	      <version>2.9.2</version>
	    </dependency>
 * -----------------------------------------------------------------
 * application.yml 다음의 내용 추가
 * -----------------------------------------------------------------
    # swaagger 설정 변경
    # http://localhost:####/swagger-ui.html
    pathmatch:
      matching-strategy: ant-path-matcher
 */


@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.example"))
            .paths(PathSelectors.any())
            .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("ePLATON Open API Test with Swagger")
            .description("설명 부분")
            .version("0.0.1-SNAPSHOT")
            .build();
    }
}