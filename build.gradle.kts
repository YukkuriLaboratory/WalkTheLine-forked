plugins {
    `maven-publish`
    alias(libs.plugins.fabricLoom)
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com/releases/")
    exclusiveContent {
        forRepository {
            maven { url = uri("https://maven.shedaniel.me/") }
        }
        filter { includeGroup("me.shedaniel.cloth") }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    exclusiveContent {
        forRepository {
            maven("https://maven.ladysnake.org/releases") {
                name = "Ladysnake Mods"
            }
        }
        filter {
            includeGroup("org.ladysnake.cardinal-components-api")
        }
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("walk-the-line") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

dependencies {
    modImplementation(libs.joml)?.let(::include)

    modImplementation(libs.fabricPermissionsApi)?.let(::include)
    modImplementation(libs.bundles.cca)?.let(::include)
    modCompileOnly(libs.iris)

    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn) { classifier("v2") })
    modImplementation(libs.fabricLoader)

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation(libs.fabricApi)

    modApi(libs.clothConfig) {
        exclude(group = "net.fabricmc.fabric-api")
    }

    modApi(libs.modmenu)

    implementation(libs.gson)
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to inputs.properties["version"]))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    inputs.property("archivesName", project.base.archivesName)

    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

tasks.remapJar {
    dependsOn(tasks.jar)
    archiveFileName.set("${project.base.archivesName}-${project.version}-${libs.versions.minecraft.get()}.jar")
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
    }
}
