import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    api(project(":controller-shared"))
}

tasks.named("shadowJar", ShadowJar::class) {
    mergeServiceFiles()
    relocate("com", "app.simplecloud.external.com")
    relocate("google", "app.simplecloud.external.google")
    relocate("io", "app.simplecloud.external.io")
    relocate("org", "app.simplecloud.external.org")
    relocate("javax", "app.simplecloud.external.javax")
    relocate("android", "app.simplecloud.external.android")
    relocate("build.buf.gen.simplecloud", "app.simplecloud.buf")
    archiveFileName.set("${project.name}.jar")
}