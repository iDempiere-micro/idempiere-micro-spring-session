package company.bigger.test.session

import company.bigger.test.support.BaseTest
import company.bigger.web.controller.SessionController
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertNotNull

/**
 * Testing the session controller (direct call).
 */
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SessionControllerTests : BaseTest() {

    @Autowired
    private lateinit var sessionController: SessionController

    /**
     * GardenUser can login (controller)
     */
    @Test
    fun `GardenUser can login (controller)`() {
        val result = sessionController.login("GardenUser", "GardenUser")
        assertNotNull(result?.token)
    }
}