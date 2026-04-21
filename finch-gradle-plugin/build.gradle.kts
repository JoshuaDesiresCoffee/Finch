plugins {
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(project(":finch-core"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://github.com/JoshuaDesiresCoffee/Finch"
    vcsUrl = "https://github.com/JoshuaDesiresCoffee/Finch"

    plugins {
        create("finch") {
            id = "io.github.finch"
            implementationClass = "io.github.finch.gradle.FinchPlugin"
            displayName = "Finch Gradle Plugin"
            description = "Gradle plugin for Finch"
            tags = listOf("finch")
        }
    }
}
