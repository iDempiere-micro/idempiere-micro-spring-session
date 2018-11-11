package company.bigger

import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * The main iDempiere-Micro Spring Boot application.
 *
 */
@Configuration
@EnableCaching
@SpringBootApplication
open class Application : WebMvcConfigurer {

    /**
     * Unlimited CORS allowed
     */
    override fun addCorsMappings(registry: CorsRegistry?) {
        registry!!.addMapping("*")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600)
    }
}

/**
 * Main application entry point. We try to get the ROLLBAR_TOKEN and setup Rollbar for error handling.
 */
fun main(args: Array<String>) {
    val token = System.getenv("ROLLBAR_TOKEN") ?: ""
    if (token.isEmpty()) {
        println("Rollbar not setup.")
    } else {
        val rollbar = Rollbar.init(ConfigBuilder.withAccessToken("602b880210304119b6435c4129061714").build())
        rollbar.log("idempiere-micro-spring started")
    }
    SpringApplication.run(Application::class.java, *args)
}
