package company.bigger.test.support

import company.bigger.service.UserService
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import company.bigger.util.Ini
import kotliquery.HikariCP
import org.junit.Before
import org.springframework.test.context.ContextConfiguration

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = [company.bigger.Application::class])
abstract class BaseTest {
    @Autowired
    protected lateinit var ini: Ini

    @Autowired
    protected lateinit var userService: UserService

    @Before
    open fun prepare() {
        HikariCP.default(ini.url, ini.username, ini.password)
    }
}