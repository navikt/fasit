package no.nav.aura.fasit.rest.config.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SpringDocConfig {

    @Bean
     OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(new Info()
                        .title("Fasit API")
                        .description("This is an interactive API reference for the fasit api.\nBelow you will see the main sections of the API. Click each section in order to see the endpoints that are available in that category and use the 'Try it out' button to make API calls.\n\nGET operations does not require any authentication, except for secrets and files.\nPUT, POST and DELETE operations requires authentication. The api uses basic authentication and authenticaties against adeo.no with your NAV user. You can also use service users. Access to fasit for personal users are handled through 'identrutina'.\n\nGenerally search operations will return an empty list if nothing is found.\n\nGET operations that returns large result sets are paged by default. Page size can be overrideen by setting the query param 'pr_page' (defaults to 100).\nWhen response is paged, the header 'total_count' shows the total number of responses and  the link headers shows link to next and last page.\n\nFor bugs, questions or feature requests create a Jira issue in project Aura or use the contact info below.")
                        .contact(new Contact().email("DGNAVIKTAURA@adeo.no")));
    }
    
    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .build();
    }
//    
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // This is crucial for serving Swagger UI at /api without redirects
//        registry.addResourceHandler("/api/**")
//                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
//                .resourceChain(false);
//        
//        // Make sure API docs are also properly served
//        registry.addResourceHandler("/api-docs/**")
//                .addResourceLocations("classpath:/META-INF/resources/");
//    }
}