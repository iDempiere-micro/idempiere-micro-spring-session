package company.bigger.test.support

import company.bigger.test.clients.LoginClient
import feign.Feign
import feign.gson.GsonDecoder
import feign.gson.GsonEncoder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.env.Environment
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPathProvider

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
open class BaseIntegrationTest : BaseTest() {
    companion object {
        const val USER = "GardenUser"
    }

    @Autowired
    protected lateinit var environment: Environment

    @LocalServerPort
    var randomServerPort: Int = 0

    val serverUrl get() = "http://localhost:$randomServerPort"

    private fun <T> buildClient(t: Class<T>): T {
        environment.let {
            return Feign.builder()
                    .encoder(GsonEncoder()).decoder(GsonDecoder())
                    .target(t, serverUrl)
        }
    }

    protected var loginClient: LoginClient? = null

    @Before
    fun prepare() {
        loginClient = buildClient(LoginClient::class.java)
    }

    @Test
    fun contextLoadsBase() {
    }

    protected fun getGardenUserToken(): String {
        val gardenUserLogin = loginClient?.login(USER, USER)
        gardenUserLogin!!
        val token = gardenUserLogin.token
        token!!
        return token
    }

    class DummyDispatcherServletPathProvider : DispatcherServletPathProvider {
        override fun getServletPath(): String {
            return ""
        }
    }
}