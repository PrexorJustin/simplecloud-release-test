import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.sonatype.central.portal.publisher)
    `maven-publish`
}

fun determineVersion(): String {
    val baseVersion = "0.0.30"
    val releaseType = project.findProperty("releaseType")?.toString() ?: "snapshot"
    val commitHash = System.getenv("COMMIT_HASH") ?: "local"

    return when (releaseType) {
        "release" -> baseVersion
        "rc" -> "$baseVersion-rc.$commitHash"
        "snapshot" -> "$baseVersion-SNAPSHOT.$commitHash"
        else -> "$baseVersion-SNAPSHOT.local"
    }
}

fun determineRepositoryUrl(): String {
    val baseUrl = "http://172.26.96.250"
    return when (project.findProperty("releaseType")?.toString() ?: "snapshot") {
        "release" -> "$baseUrl/releases"
        "rc" -> "$baseUrl/rc"
        else -> "$baseUrl/snapshots"
    }
}

allprojects {
    group = "app.simplecloud.controller"
    version = determineVersion()

    repositories {
        mavenCentral()
        maven("https://buf.build/gen/maven")
        maven("https://repo.simplecloud.app/snapshots")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "net.thebugmc.gradle.sonatype-central-portal-publisher")
    apply(plugin = "maven-publish")

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlin.jvm)
    }

    publishing {
        repositories {
            maven {
                name = "simplecloud"
                url = uri(determineRepositoryUrl())
                credentials {
                    username = System.getenv("SIMPLECLOUD_USERNAME")
                        ?: (project.findProperty("simplecloudUsername") as? String)
                    password = System.getenv("SIMPLECLOUD_PASSWORD")
                        ?: (project.findProperty("simplecloudPassword") as? String)
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }

        publications {
            // Not publish controller-runtime
            if (project.name == "controller-runtime") {
                return@publications
            }

            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        }
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")
    }

    tasks.test {
        useJUnitPlatform()
    }

    centralPortal {
        name = project.name

        username = project.findProperty("sonatypeUsername") as? String
        password = project.findProperty("sonatypePassword") as? String

        pom {
            name.set("SimpleCloud controller")
            description.set("The heart of SimpleCloud v3")
            url.set("https://github.com/theSimpleCloud/simplecloud-controller")

            developers {
                developer {
                    id.set("fllipeis")
                    email.set("p.eistrach@gmail.com")
                }
                developer {
                    id.set("dayyeeet")
                    email.set("david@cappell.net")
                }
            }
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                url.set("https://github.com/theSimpleCloud/simplecloud-controller.git")
                connection.set("git:git@github.com:theSimpleCloud/simplecloud-controller.git")
            }
        }
    }

    signing {
        val releaseType = project.findProperty("releaseType")?.toString() ?: "snapshot"
        if (releaseType != "release") {
            return@signing
        }

        sign(publishing.publications)
        useGpgCmd()
    }
}
