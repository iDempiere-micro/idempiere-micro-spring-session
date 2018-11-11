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
 */
@RestController
class SessionController(
    private val userService: UserService
) {

    @GetMapping()
    @RequestMapping(value = ["/session/{username}/login/{password}"])
    fun login(@PathVariable username: String, @PathVariable password: String): UserLoginModelResponse? {
        return userService.login(UserLoginModel(username, password))
    }
}