plugins {
    java
    application
}

val jvmVersion = JavaVersion.VERSION_17

allprojects {
    apply(plugin = "java")

    group = "fr.uge.chat_fusion"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    }

    java {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }

    tasks {
        test {
            useJUnitPlatform()
        }
    }
}

val mainPackage = "fr.uge.chat_fusion"

fun Project.jarConfig(mainClassFQName: String) {
    application {
        mainClass.set(mainClassFQName)
    }

    tasks.jar {
        archiveFileName.set("${rootProject.name}-${project.name}-${rootProject.version}.jar")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest {
            attributes["Main-Class"] = mainClassFQName
        }
        configurations["compileClasspath"].forEach { file ->
            from(zipTree(file.absoluteFile))
        }
    }

}

project(":client") {
    apply(plugin = "application")

    dependencies {
        implementation(project(":core"))
    }

    jarConfig("$mainPackage.client.Application")
}

project(":server") {
    apply(plugin = "application")

    dependencies {
        implementation(project(":core"))
    }

    jarConfig("$mainPackage.server.Application")
}
