package company.bigger.test.session

import company.bigger.test.support.BaseTest
import company.bigger.web.controller.SessionController
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertNotNull

class SessionControllerTests : BaseTest() {
    @Autowired
    private lateinit var sessionController: SessionController

    @Test
    fun `GardenUser can login (controller)`() {
        val result = sessionController.login("GardenUser", "GardenUser")
        assertNotNull(result?.token)
    }
}