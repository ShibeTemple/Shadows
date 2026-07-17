@file:Suppress("UnstableApiUsage")

import io.github.klahap.dotenv.DotEnvBuilder
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    kotlin("jvm")
    alias(libs.plugins.loom)
    id("dev.kikugie.postprocess.jsonlang")
    id("me.modmuss50.mod-publish-plugin") version "2.0.0-beta.1"
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
    id("io.github.klahap.dotenv") version "1.1.3"
}

fletchingTable {
    mixins.create("main") {
        mixin("default", "${project.property("mod.id")}.mixins.json")
    }
}

sourceSets {
    main {
        java {
            if (sc.current.parsed < "1.21.5") {
                exclude("net/typho/vibrancy/mixin/GlTextureAccessor.java")
                exclude("net/typho/vibrancy/mixin/GlBufferAccessor.java")
            }
            if (sc.current.parsed < "1.21") {
                exclude("net/typho/vibrancy/mixin/sodium/DefaultFluidRendererMixin.java")
                exclude("net/typho/vibrancy/mixin/sodium/FluidRendererImplMixin.java")
            }
        }
    }
}

val env = DotEnvBuilder.dotEnv {
    addFile("$rootDir/.env")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.withType<RemapJarTask> {
    destinationDirectory.set(rootProject.file("build/libs/${project.version}"))
}

tasks.withType<RemapSourcesJarTask> {
    destinationDirectory.set(rootProject.file("build/libs/${project.version}"))
}

tasks.named<ProcessResources>("processResources") {
    val props = HashMap<String, String>().apply {
        this["minecraft"] = (project.property("deps.minecraft_range") ?: project.property("deps.minecraft")) as String
        this["java"] = when {
            sc.current.parsed >= "1.20.5" -> "21"
            sc.current.parsed >= "1.18" -> "17"
            sc.current.parsed >= "1.17" -> "16"
            else -> "8"
        }
        this["mod_id"] = project.property("mod.id") as String
        this["mod_name"] = project.property("mod.name") as String
        this["mod_version"] = project.property("mod.version") as String
        this["mod_author"] = project.property("mod.author") as String
        this["mod_description"] = project.property("mod.description") as String
        this["mod_credits"] = project.property("mod.credits") as String
        this["mod_license"] = project.property("mod.license") as String
        this["big_shot_version"] = libs.bigShot.get().version as String
    }

    inputs.properties(props)

    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml", "**/*.mixins.json")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("mod.id") as String

jsonlang {
    languageDirectories = listOf("assets/${property("mod.id")}/lang")
    prettyPrint = true
}

repositories {
    mavenLocal()
    maven("https://thedarkcolour.github.io/KotlinForForge/") { name = "KotlinForForge" }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven"
    }
    maven("https://maven.ryanhcode.dev/releases") { name = "RyanHCode Maven" }
    maven("https://mvn.devos.one/snapshots/") { name = "devos.one Snapshots" }
    maven("https://mvn.devos.one/releases/") { name = "devos.one Releases" }
    maven("https://maven.createmod.net/") { name = "CreateMod Maven" }
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven") { name = "Fuzss Mod Resources" }
    maven("https://maven.jamieswhiteshirt.com/libs-release") { name = "JamiesWhiteshirt" }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }
    maven("https://maven.quiltmc.org/repository/release/") {
        name = "Quilt Release"
    }
    ivy {
        url = uri("https://github.com/TheTypholorian/big_shot_lib/releases/download")
        patternLayout {
            artifact("[revision]/[artifact]-[revision](-[classifier]).[ext]")
        }
        metadataSources {
            artifact()
        }
    }
}

tasks.withType<Javadoc>().configureEach {
    enabled = false
}

// MC 1.20.1 bundles LWJGL 3.3.2-snapshot, but Sodium 0.5.x requires exactly 3.3.1.
// Force LWJGL 3.3.1 in the dev environment so Sodium's early driver scanner passes.
if (sc.current.parsed < "1.21") {
    configurations.all {
        resolutionStrategy.force(
            "org.lwjgl:lwjgl:3.3.1",
            "org.lwjgl:lwjgl-glfw:3.3.1",
            "org.lwjgl:lwjgl-openal:3.3.1",
            "org.lwjgl:lwjgl-opengl:3.3.1",
            "org.lwjgl:lwjgl-stb:3.3.1",
            "org.lwjgl:lwjgl-tinyfd:3.3.1",
            "org.lwjgl:lwjgl-jemalloc:3.3.1"
        )
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.layered {
        officialMojangMappings()
        if (hasProperty("deps.parchment"))
            parchment("org.parchmentmc.data:parchment-${property("deps.parchment")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:0.19.2")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")
    modImplementation(libs.flk)

    modCompileOnly("maven.modrinth:sodium:${property("deps.sodium")}")
    modLocalRuntime("maven.modrinth:sodium:${property("deps.sodium")}")
    modCompileOnly(libs.bigShot)
    if (hasProperty("deps.big_shot_runtime")) {
        modLocalRuntime(files(rootProject.file(property("deps.big_shot_runtime") as String)))
    } else {
        modLocalRuntime(libs.bigShot)
    }
    modCompileOnly("maven.modrinth:yacl:${property("deps.yacl")}")
    modLocalRuntime("maven.modrinth:yacl:${property("deps.yacl")}")
    // YACL bundles quilt-parsers as jar-in-jar, but Fabric Loader doesn't unpack JiJ
    // from classpath-added mods in dev. Add it explicitly so GsonConfigSerializer works.
    if (hasProperty("deps.yacl")) {
        runtimeOnly("org.quiltmc.parsers:gson:0.2.1")
        runtimeOnly("org.quiltmc.parsers:json:0.2.1")
    }
    val modmenuDep = if (hasProperty("deps.modmenu")) "maven.modrinth:modmenu:${property("deps.modmenu")}" else libs.modmenu.get().toString()
    modCompileOnly(modmenuDep)
    modLocalRuntime(modmenuDep)

    if (sc.current.version == "1.21") {
        include(libs.sableCompanionFabric)
        modApi(libs.sableCompanionFabric)
    }

    // Indium bridges Sodium with Fabric's Rendering API (FRAPI). Required for mods that use
    // FRAPI (Create, LED) to avoid BlockModel.<clinit> NPE under Sodium.
    if (hasProperty("deps.indium")) modLocalRuntime("maven.modrinth:indium:${property("deps.indium")}")
    // Create Fabric: use Maven coordinate so Gradle resolves all transitive deps automatically
    // (flywheel, ponder, registrate, porting_lib_*, forgeconfigapiport, milk-lib, reach-entity-attributes).
    if (hasProperty("deps.create")) modLocalRuntime("com.simibubi.create:create-fabric:${property("deps.create")}")
    // LED (Light Emitting Diode): requires JMXL >= 1.4; the only available JMXL for 1.20.1
    // (1.4+mc1.20.5) targets Java 21 (class file 65) but dev runs Java 17 (class file 61).
    // No compatible JMXL exists — LED cannot load in this dev environment.
    // Block light JSON configs for LED fixtures ship in assets/led/ and are picked up at runtime.
}

fabricApi {
    configureDataGeneration() {
        outputDirectory = file("$rootDir/src/main/generated")
        //client = true
    }
}

val mcVersion = project.property("deps.minecraft") as String
loom {
    runs {
        named("client") {
            configName = "Run Client (MC $mcVersion Fabric)"
            ideConfigGenerated(true)
            runDir("run")
            // Temurin 17.0.18 aarch64 has two JVM bugs in dev:
            //   1. C2 JIT: SIGBUS in Arena::destruct_contents compiling MixinProcessor.applyMixins.
            //      Fix: exclude that one method from JIT; C2 runs normally for everything else.
            //   2. Class verifier: SIGSEGV in Dictionary::find when verifying BehaviorBuilder
            //      (triggered by villager/entity loading). Fix: skip bytecode verification.
            vmArg("-XX:CompileCommand=exclude,org.spongepowered.asm.mixin.transformer.MixinProcessor::applyMixins")
            vmArg("-Xverify:none")
        }
        named("server") {
            configName = "Run Server (MC $mcVersion Fabric)"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "**/mods.toml")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

java {
    val javaCompat = when {
        sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
        sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
        sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
        else -> JavaVersion.VERSION_1_8
    }
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}

kotlin {
    jvmToolchain(
        when {
            sc.current.parsed >= "1.20.5" -> 21
            sc.current.parsed >= "1.18" -> 17
            sc.current.parsed >= "1.17" -> 16
            else -> 8
        }
    )
}

val additionalVersionsStr = findProperty("publish.additionalVersions") as String?
val additionalVersions: List<String> = additionalVersionsStr
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("mod.name")} ${property("mod.version")} for ${stonecutter.current.version} Fabric"
    version = "${property("mod.version")}+${stonecutter.current.version}-fabric"
    changelog = ""
    //changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("fabric")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = env["MODRINTH_TOKEN"]
        minecraftVersions.add(stonecutter.current.version)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api", "fabric-language-kotlin", "big-shot-lib", "yacl")
    }

    /*
    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = env["CURSEFORGE_TOKEN"]
        minecraftVersions.add(stonecutter.current.version)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api", "fabric-language-kotlin", "big-shot-lib", "yacl")
    }
     */
}