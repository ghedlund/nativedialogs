plugins {
    `java-library`
    `maven-publish`
}

group = "ca.phon"
version = "23"
description = "Native dialogs for Java with fallback to Swing."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    modularity.inferModulePath = true
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "ca.phon.ui.nativedialogs.demo.NativeDialogsDemo")
    }
}

// JNI header generation — mirrors the old Ant/Maven javah task
tasks.register<Exec>("generateJniHeaders") {
    dependsOn(tasks.compileJava)
    description = "Generate JNI headers for NativeDialogs"
    group = "build"

    val headerOutputDir = file("target/generated-sources/cpp/include")
    val tempClassesDir = layout.buildDirectory.dir("tmp/jni-classes")

    doFirst {
        headerOutputDir.mkdirs()
        tempClassesDir.get().asFile.mkdirs()
    }

    val javaToolchain = project.extensions.getByType<JavaToolchainService>()
    val compiler = javaToolchain.compilerFor {
        languageVersion = JavaLanguageVersion.of(21)
    }

    executable = compiler.get().executablePath.asFile.parentFile.resolve("javac").absolutePath
    args(
        "-h", headerOutputDir.absolutePath,
        "-classpath", tasks.compileJava.get().destinationDirectory.get().asFile.absolutePath,
        "-d", tempClassesDir.get().asFile.absolutePath,
        "src/main/java/ca/phon/ui/nativedialogs/NativeDialogs.java"
    )
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
