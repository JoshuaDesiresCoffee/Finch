plugins {
    `maven-publish`
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId    = "io.github.finch"
            artifactId = "finch-core"
            version    = project.version.toString()
        }
    }
}
