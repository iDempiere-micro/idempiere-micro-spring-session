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

/**
 * Base Unit test running without the web environment
 */
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = [company.bigger.Application::class])
abstract class BaseTest {
    companion object {
        private var setUpIsDone = false
    }

    @Autowired
    protected lateinit var ini: Ini

    @Autowired
    protected lateinit var userService: UserService

    /**
     * At the beginning of the tests setup the Hikari Connection Pool to connect to the Ini-provided PgSQL
     */
    @Before
    open fun prepare() {
        if (!setUpIsDone) {
            HikariCP.default(ini.url, ini.username, ini.password)
            setUpIsDone = true
        }
    }
}
