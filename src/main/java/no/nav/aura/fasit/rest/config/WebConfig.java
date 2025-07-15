package no.nav.aura.fasit.rest.config;

import java.util.List;

import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import no.nav.aura.fasit.rest.spring.RestParameterNameResolver;
import no.nav.aura.fasit.rest.spring.StringToEnumConverterFactory;


@Configuration
public class WebConfig implements WebMvcConfigurer {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(WebConfig.class);
	
    private final RestParameterNameResolver restParameterNameResolver;

    public WebConfig(RestParameterNameResolver restParameterNameResolver) {
        this.restParameterNameResolver = restParameterNameResolver;
    }
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map the root and /api directly to the swagger-ui index.html
    	log.info("Adding ViewControllers for Swagger UI");
        registry.addViewController("/api").setViewName("forward:/swagger-ui/index.html");
        registry.addViewController("/api/").setViewName("forward:/swagger-ui/index.html");
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Explicitly map the swagger-ui resources to prevent redirection
        registry.addResourceHandler("/api/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
                .resourceChain(false);
    }
    
    // this is needed to convert String to Enum in @RequestParam
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToEnumConverterFactory());
    }
    
    
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(restParameterNameResolver);
    }
    
}
