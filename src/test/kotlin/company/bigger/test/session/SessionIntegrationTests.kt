package company.bigger.test.session

import company.bigger.test.support.BaseIntegrationTest
import company.bigger.test.support.randomString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Testing the session controller throught the REST interface.
 */
class SessionIntegrationTests : BaseIntegrationTest() {
    /**
     * GardenUser can login
     */
    @Test
    fun `GardenUser can login`() {
        val gardenUserLogin = loginClient?.login("GardenUser", "GardenUser")
        assertNotNull(gardenUserLogin)
        assertTrue { gardenUserLogin?.logged == true }
    }

    /**
     * Random user can not login
     */
    @Test
    fun `Random user can not login`() {
        val gardenUserLogin = loginClient?.login(randomString(10), randomString(10))
        assertNotNull(gardenUserLogin)
        assertFalse { gardenUserLogin?.logged == true }
    }

    /**
     * GardenUser can login and the token works
     */
    @Test
    fun `GardenUser can login and the token works`() {
        val gardenUserLogin = loginClient?.login("GardenUser", "GardenUser")
        val result = loginClient?.validateToken(gardenUserLogin?.token ?: "")
        assertEquals(11, result?.clientId)
    }
}