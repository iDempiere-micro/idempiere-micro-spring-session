package company.bigger.test.session

import company.bigger.dto.UserLoginModel
import company.bigger.test.support.BaseTest
import company.bigger.test.support.randomString
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserServiceTests : BaseTest() {
    companion object {
        const val GardenAdmin = "GardenAdmin"
        const val GardenUser = "GardenUser"
        const val System = "System"
    }

    @Test
    fun `GardenUser can login (service)`() {
        val result = userService.login(UserLoginModel(GardenUser, GardenUser))
        assertNotNull(result?.token)
    }
    @Test
    fun `GardenUser can not login with wrong password (service)`() {
        val result = userService.login(UserLoginModel(GardenUser, randomString(20)))
        assertNull(result?.token)
    }

    @Test
    fun `Joe Sales cannot login as he does not have a password (service)`() {
        val result = userService.login(UserLoginModel("Joe Sales", ""))
        assertNull(result?.token)
    }

    @Test
    fun `System can not login as he does not have the business partner associated (service)`() {
        val result = userService.login(UserLoginModel(System, System))
        assertNull(result?.token)
    }

    @Test
    fun `GardenAdmin can not login after too much unsuccessful logins (service)`() {
        val okResult = userService.login(UserLoginModel(GardenAdmin, GardenAdmin))
        assertNotNull(okResult?.token)
        val badPassword = randomString(20)
        for (i in 1..20) {
            val result = userService.login(UserLoginModel(GardenAdmin, badPassword))
            assertNull(result?.token)
        }
        val result = userService.login(UserLoginModel(GardenAdmin, GardenAdmin))
        assertNull(result?.token)
    }
}