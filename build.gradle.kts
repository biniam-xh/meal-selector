import java.io.File

plugins {
    id("com.android.application") version "8.0.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

fun ensureBuildJunction(moduleDir: File, moduleName: String) {
    if (!System.getProperty("os.name").contains("Windows", ignoreCase = true)) return

    val link = File(moduleDir, "build")
    val target = File(
        System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),
        "MealSelectorBuild/$moduleName"
    ).canonicalFile
    target.mkdirs()

    if (link.exists()) {
        if (isBuildJunction(link)) {
            val currentTarget = readJunctionTarget(link)?.canonicalFile
            if (currentTarget == target) return
            removeBuildJunction(link)
        } else {
            link.deleteRecursively()
        }
    }

    val process = ProcessBuilder(
        "cmd", "/c", "mklink", "/J", link.absolutePath, target.absolutePath
    ).redirectErrorStream(true).start()
    if (process.waitFor() != 0) {
        val output = process.inputStream.bufferedReader().readText()
        println("WARN: Could not create build junction for $moduleName: $output")
    }
}

fun isBuildJunction(path: File): Boolean {
    if (!path.exists()) return false
    val process = ProcessBuilder("cmd", "/c", "fsutil", "reparsepoint", "query", path.absolutePath)
        .redirectErrorStream(true)
        .start()
    return process.waitFor() == 0
}

fun readJunctionTarget(link: File): File? {
    val process = ProcessBuilder("cmd", "/c", "fsutil", "reparsepoint", "query", link.absolutePath)
        .redirectErrorStream(true)
        .start()
    if (process.waitFor() != 0) return null
    val output = process.inputStream.bufferedReader().readText()
    val match = Regex("""Print Name:\s*(.+)""").find(output) ?: return null
    return File(match.groupValues[1].trim())
}

fun removeBuildJunction(link: File) {
    ProcessBuilder("cmd", "/c", "rmdir", link.absolutePath).start().waitFor()
}

// OneDrive locks files under app/build during sync. Use a junction so Gradle and
// Android Studio both see app/build, while files are stored on local disk.
gradle.beforeProject {
    if (name == "app") {
        ensureBuildJunction(projectDir, name)
    }
}
