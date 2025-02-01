package app.simplecloud.controller.api

import app.simplecloud.controller.api.impl.coroutines.ControllerApiCoroutineImpl
import app.simplecloud.controller.api.impl.future.ControllerApiFutureImpl
import app.simplecloud.pubsub.PubSubClient

interface ControllerApi {

    interface Future {

        /**
         * @return the Controller [GroupApi.Future]
         */
        fun getGroups(): GroupApi.Future

        /**
         * @return the Controller [ServerApi.Future]
         */
        fun getServers(): ServerApi.Future

        /**
         * @return the [PubSubClient] to subscribe to Controller events and send messages
         */
        fun getPubSubClient(): PubSubClient

    }

    interface Coroutine {

        /**
         * @return the Controller [GroupApi.Coroutine]
         */
        fun getGroups(): GroupApi.Coroutine

        /**
         * @return the Controller [ServerApi.Coroutine]
         */
        fun getServers(): ServerApi.Coroutine

        /**
         * @return the [PubSubClient] to subscribe to Controller events and send messages
         */
        fun getPubSubClient(): PubSubClient

    }

    companion object {

        /**
         * Creates a new [ControllerApi.Future] instance
         * @return the created [ControllerApi.Future]
         */
        @JvmStatic
        fun createFutureApi(): Future {
            val authSecret = System.getenv("CONTROLLER_SECRET")
            return createFutureApi(authSecret)
        }

        /**
         * Creates a new [ControllerApi.Future] instance
         * @param authSecret the authentication key used by the Controller
         * @return the created [ControllerApi.Future]
         */
        @JvmStatic
        fun createFutureApi(authSecret: String): Future {
            return ControllerApiFutureImpl(authSecret)
        }

        /**
         * Creates a new [ControllerApi.Coroutine] instance
         * @return the created [ControllerApi.Coroutine]
         */
        @JvmStatic
        fun createCoroutineApi(): Coroutine {
            val authSecret = System.getenv("CONTROLLER_SECRET")
            return createCoroutineApi(authSecret)
        }

        /**
         * Creates a new [ControllerApi.Coroutine] instance
         * @param authSecret the authentication key used by the Controller
         * @return the created [ControllerApi.Coroutine]
         */
        @JvmStatic
        fun createCoroutineApi(authSecret: String): Coroutine {
            return ControllerApiCoroutineImpl(authSecret)
        }

    }

}