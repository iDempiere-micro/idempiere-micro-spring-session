package company.bigger.web.config
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

/**
 * Main web security configuration. Makes everything except `session` and `actuator` protected.
 * Also introduces the JWT TokenAuthenticationFilter.
 * CSRF is turned off since we are on REST and GraphQL only.
 */
@Configuration
@EnableWebSecurity
open class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {

        http
                .authorizeRequests()
                .antMatchers("/swagger-ui.html").permitAll()
                .antMatchers("/swagger-ui.html").permitAll()
                .antMatchers("/webjars/**").permitAll()
                .antMatchers("/session/**").permitAll()
                .antMatchers("/swagger-resources/**").permitAll()
                .antMatchers("/subscriptions/**").permitAll()
                .antMatchers("/vendor/**").permitAll()
                .antMatchers("/actuator/**").permitAll()

                .anyRequest().authenticated()

                // From https://github.com/bfwg/springboot-jwt-starter
                .and().csrf().disable()
    }
}