package company.bigger

import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder
import kotliquery.HikariCP
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
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
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("*")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600)
    }
}

/**
 * The original Ini class from iDempiere to collect the configuration parameters.
 * It is still used for the connection string etc.
 * Note some local connection variables (like `jwt.issuer`) if used once-only
 * are declared in the place where needed.
 */
@Configuration
open class Ini {
    @Value("\${session.url}")
    internal lateinit var url: String
    @Value("\${session.username}")
    internal lateinit var username: String
    @Value("\${session.password}")
    internal lateinit var password: String
}

/**
 * Listener to run after successful application start.
 */
@Component
open class StartupApplicationListener(val ini: Ini) : ApplicationListener<ContextRefreshedEvent> {
    override fun onApplicationEvent(event: ContextRefreshedEvent) {

        /**
         * Connect HikariCP
         */
        HikariCP.default(ini.url, ini.username, ini.password)
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
    SpringApplication.run(arrayOf<Class<*>>(Application::class.java), args)
}
