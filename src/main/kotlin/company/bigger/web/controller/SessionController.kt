package company.bigger.web.controller

import company.bigger.dto.UserLoginModel
import company.bigger.dto.UserLoginModelResponse
import company.bigger.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Session `/session` controller for log-in.
 * This is the only REST controller we have.
 * Note that we support one token per login name now only.
 * Also we still store the token inside the microservice so they do not survive the microservice restart.
 */
@RestController
class SessionController(
    private val userService: UserService
) {

    /**
     * Login and return token if the username and password are valid.
     * Note that when a user logs in, if the user was logged from somewhere else before, the previous token is invalidated.
     */
    @GetMapping()
    @RequestMapping(value = ["/session/{username}/login/{password}"])
    fun login(@PathVariable username: String, @PathVariable password: String): UserLoginModelResponse? {
        return userService.login(UserLoginModel(username, password))
    }

    /**
     * Validate a token and return the associated user
     */
    @GetMapping()
    @RequestMapping(value = ["/session/{token}/validate"])
    fun validateToken(@PathVariable token: String): UserLoginModelResponse? {
        return userService.validateToken(token)
    }
}