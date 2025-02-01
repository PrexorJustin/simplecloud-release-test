package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.runtime.MetricsEventNames
import app.simplecloud.controller.runtime.YamlDirectoryRepository
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.droplet.api.time.ProtobufTimestamp
import app.simplecloud.pubsub.PubSubClient
import build.buf.gen.simplecloud.metrics.v1.metric
import build.buf.gen.simplecloud.metrics.v1.metricMeta
import java.nio.file.Path
import java.time.LocalDateTime

class GroupRepository(
    path: Path,
    private val pubSubClient: PubSubClient
) : YamlDirectoryRepository<Group, String>(path, Group::class.java, WatcherEvents(pubSubClient)) {
    override fun getFileName(identifier: String): String {
        return "$identifier.yml"
    }

    override suspend fun find(identifier: String): Group? {
        return entities.values.find { it.name == identifier }
    }

    override fun save(element: Group) {
        save(getFileName(element.name), element)
    }

    override suspend fun getAll(): List<Group> {
        return entities.values.toList()
    }

    private class WatcherEvents(
        private val pubsubClient: PubSubClient
    ) : YamlDirectoryRepository.WatcherEvents<Group> {

        override fun onCreate(entity: Group) {
            pubsubClient.publish(MetricsEventNames.RECORD_METRIC, metric {
                metricType = "ACTIVITY_LOG"
                metricValue = 1L
                time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
                meta.addAll(
                    listOf(
                        metricMeta {
                            dataName = "displayName"
                            dataValue = entity.name
                        },
                        metricMeta {
                            dataName = "status"
                            dataValue = "CREATED"
                        },
                        metricMeta {
                            dataName = "resourceType"
                            dataValue = "GROUP"
                        },
                        metricMeta {
                            dataName = "groupName"
                            dataValue = entity.name
                        },
                        metricMeta {
                            dataName = "by"
                            dataValue = "FILE_WATCHER"
                        }
                    )
                )
            })
        }

        override fun onDelete(entity: Group) {
            pubsubClient.publish(MetricsEventNames.RECORD_METRIC, metric {
                metricType = "ACTIVITY_LOG"
                metricValue = 1L
                time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
                meta.addAll(
                    listOf(
                        metricMeta {
                            dataName = "displayName"
                            dataValue = entity.name
                        },
                        metricMeta {
                            dataName = "status"
                            dataValue = "DELETED"
                        },
                        metricMeta {
                            dataName = "resourceType"
                            dataValue = "GROUP"
                        },
                        metricMeta {
                            dataName = "groupName"
                            dataValue = entity.name
                        },
                        metricMeta {
                            dataName = "by"
                            dataValue = "FILE_WATCHER"
                        }
                    )
                )
            })
        }

        override fun onModify(entity: Group) {
            pubsubClient.publish(MetricsEventNames.RECORD_METRIC, metric {
                metricType = "ACTIVITY_LOG"
                metricValue = 1L
                time = ProtobufTimestamp.fromLocalDateTime(LocalDateTime.now())
                meta.addAll(
                    listOf(
                        metricMeta {
                            dataName = "displayName"
                            dataValue = entity.name
                        },
                        metricMeta {
                            dataName = "status"
                            dataValue = "EDITED"
                        },
                        metricMeta {
                            dataName = "resourceType"
                            dataValue = "GROUP"
                        },
                        metricMeta {
                            dataName = "groupName"
                            dataValue = entity.name
                        },
                        metricMeta {
                            dataName = "by"
                            dataValue = "FILE_WATCHER"
                        }
                    )
                )
            })
        }

    }
}