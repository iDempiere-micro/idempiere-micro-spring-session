package company.bigger.test.clients

import company.bigger.test.clients.response.UserLoginModelResponse
import feign.Param
import feign.RequestLine

interface LoginClient {
    @RequestLine("GET /session/{username}/login/{password}")
    fun login(@Param("username") username: String, @Param("password") password: String): UserLoginModelResponse
}
