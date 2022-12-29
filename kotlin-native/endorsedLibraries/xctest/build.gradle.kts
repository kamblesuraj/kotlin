import org.jetbrains.kotlin.findKonanBuildTask
import org.jetbrains.kotlin.globalBuildArgs
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platformManager

val distDir: File by project
val konanHome: String by extra(distDir.absolutePath)
extra["org.jetbrains.kotlin.native.home"] = konanHome

plugins {
    konan
}

val nativeSrc = project.file("src/main/kotlin")
val konanTargetList: List<KonanTarget> by project
val endorsedLibraries: Map<Project, org.jetbrains.kotlin.EndorsedLibraryInfo> by project(":kotlin-native:endorsedLibraries").ext
val library = endorsedLibraries[project] ?: throw IllegalStateException("Library for $project is not set")

konanArtifacts {
    val appleTargets = konanTargetList.filter { it.family.isAppleFamily }
    library(args = mapOf("targets" to appleTargets), name = library.name) {
        baseDir(project.buildDir.resolve(library.name))
        val moduleName = endorsedLibraries[project]?.name.toString()
        noPack(true)
        extraOpts(project.globalBuildArgs)
        extraOpts(
                "-Werror",
                "-module-name", moduleName,
        )
        srcDir(nativeSrc)
        // Cyclic dependency
//        appleTargets.forEach {
//            project.findKonanBuildTask(library.name, it)
//                    .configure { dependsOn(":kotlin-native:${it}PlatformLibs") }
//        }
    }
}


val targetList: List<String> by project
val hostName: String by project
val cacheableTargetNames: List<String> by project

val platformXCTestLib = "org.jetbrains.kotlin.native.platform.XCTest"

targetList.forEach { targetName ->
    val copyTask = tasks.register("${targetName}${library.taskName}", Copy::class.java) {
        dependsOn(project.findKonanBuildTask(library.name, project.platformManager.hostPlatform.target))
        destinationDir = buildDir.resolve("$targetName${library.name}")
        from(buildDir.resolve(library.name))
    }
    if (targetName in cacheableTargetNames) {
        tasks.register("${targetName}${library.taskName}Cache", KonanCacheTask::class.java) {
            target = targetName
            originalKlib = project.buildDir.resolve("$targetName${library.name}")
            klibUniqName = library.name
            cacheRoot = project.buildDir.resolve("cache/$targetName").absolutePath

            cachedLibraries = mapOf(
                    distDir.resolve("klib/common/stdlib")
                            to distDir.resolve("klib/cache/${target}-g$cacheKind/stdlib-cache"),
                    distDir.resolve("klib/platform/$targetName/$platformXCTestLib")
                            to distDir.resolve("klib/cache/${target}-g$cacheKind/$platformXCTestLib-cache")
            )

            dependsOn(copyTask)
            dependsOn(":kotlin-native:${targetName}CrossDistStdlib")
            dependsOn(":kotlin-native:${targetName}StdlibCache")
        }
    }
}