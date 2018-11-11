package org.idempiere.common.util

import java.io.Serializable
import java.util.Properties

/**
 * @author [Ashley G Ramdass](mailto:agramdass@gmail.com)
 * @date Feb 25, 2007
 * @version $Revision: 0.10 $
 */
class ServerContext private constructor() : Serializable {
    companion object {
        /** generated serial version Id  */
        private const val serialVersionUID = -8274580404204046413L

        private val context = object : InheritableThreadLocal<Properties>() {
            override fun initialValue(): Properties {
                return Properties()
            }
        }

        /**
         * Get server context for current thread
         *
         * @return Properties
         */
        /**
         * Set server context for current thread
         *
         * @param ctx
         */
        var currentInstance: Properties
            get() = context.get() as Properties
            set(ctx) = context.set(ctx)

        /** dispose server context for current thread  */
        fun dispose() {
            context.remove()
        }
    }
}
