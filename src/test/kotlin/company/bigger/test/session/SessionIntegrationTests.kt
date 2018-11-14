package company.bigger.test.session

import company.bigger.test.support.BaseIntegrationTest
import company.bigger.test.support.randomString
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Testing the session controller throught the REST interface.
 */
class SessionIntegrationTests : BaseIntegrationTest() {
    @Test
    fun contextLoads() {
    }

    /**
     * GardenUser can login
     */
    @Test
    fun `GardenUser can login`() {
        val gardenUserLogin = loginClient?.login("GardenUser", "GardenUser")
        println("$gardenUserLogin")
        assertNotNull(gardenUserLogin)
        assertTrue { gardenUserLogin?.logged == true }
    }

    /**
     * Random user can not login
     */
    @Test
    fun `Random user can not login`() {
        val gardenUserLogin = loginClient?.login(randomString(10), randomString(10))
        println("$gardenUserLogin")
        assertNotNull(gardenUserLogin)
        assertFalse { gardenUserLogin?.logged == true }
    }
}