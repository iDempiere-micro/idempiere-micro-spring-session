package company.bigger.dto

/**
 * Login parameters we support in idempiere-micro-spring-session.
 * Please note it is much less than in the full iDempiere.
 * Basically currently the login works only for users that are assigned to a single client.
 * This is however on par with the idempiere-micro-spring full backend see `setCurrentUser` in `LoginService`.
 */
interface ILogin {
    /**
     * The login name. Can be also the user email (we decided to support both by default, no need to setup USE_EMAIL_FOR_LOGIN)
     */
    val loginName: String
    val password: String
    val clientId: Int?
}