plugins {
    `java-library`
    `maven-publish`
}

group = "ca.phon"
version = (findProperty("version") as String?)?.takeIf { it != "unspecified" } ?: "25"
description = "Native dialogs for Java with fallback to Swing."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    modularity.inferModulePath = true
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "ca.phon.ui.nativedialogs.demo.NativeDialogsDemo",
            "Enable-Native-Access" to "ALL-UNNAMED"
        )
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Native Dialogs"
                description = project.description
                developers {
                    developer {
                        id = "ghedlund"
                        name = "Greg Hedlund"
                        email = "greg.hedlund@gmail.com"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ghedlund/nativedialogs")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
            }
        }
    }
}
