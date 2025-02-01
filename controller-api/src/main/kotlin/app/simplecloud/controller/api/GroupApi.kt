package app.simplecloud.controller.api

import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.ServerType
import java.util.concurrent.CompletableFuture

interface GroupApi {

    interface Future {

        /**
         * @param name the name of the group.
         * @return a [CompletableFuture] with the [Group].
         */
        fun getGroupByName(name: String): CompletableFuture<Group>

        /**
         * @param name the name of the group.
         * @return the deleted [Group].
         */
        fun deleteGroup(name: String): CompletableFuture<Group>

        /**
         * @param group the [Group] to create.
         * @return the created [Group].
         */
        fun createGroup(group: Group): CompletableFuture<Group>

        /**
         * @param group the [Group] to update.
         * @return the updated [Group].
         */
        fun updateGroup(group: Group): CompletableFuture<Group>
        /**
         * @return a [CompletableFuture] with a list of all groups.
         */
        fun getAllGroups(): CompletableFuture<List<Group>>

        /**
         * @param type the [ServerType] of the group
         * @return a [CompletableFuture] with a list of all groups matching this type.
         */
        fun getGroupsByType(type: ServerType): CompletableFuture<List<Group>>

    }

    interface Coroutine {

        /**
         * @param name the name of the group.
         * @return the [Group].
         */
        suspend fun getGroupByName(name: String): Group

        /**
         * @param name the name of the group.
         * @return the deleted [Group].
         */
        suspend fun deleteGroup(name: String): Group

        /**
         * @param group the [Group] to create.
         * @return the created [Group].
         */
        suspend fun createGroup(group: Group): Group

        /**
         * @param group the [Group] to update.
         * @return the updated [Group].
         */
        suspend fun updateGroup(group: Group): Group
        /**
         * @return a list of all groups.
         */
        suspend fun getAllGroups(): List<Group>

        /**
         * @param type the [ServerType] of the group
         * @return a list of all groups matching this type.
         */
        suspend fun getGroupsByType(type: ServerType): List<Group>

    }

}