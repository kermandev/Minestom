import net.ltgt.gradle.errorprone.errorprone

plugins {
    java

    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

dependencies {
    errorprone(libs.errorProneCore)
}

spotless {
    java {
        importOrder("", "javax|java", "\\#")
        removeUnusedImports()
        forbidWildcardImports()
        forbidModuleImports()
    }
}

tasks.withType<JavaCompile> {
    options.errorprone {
        error("StatementSwitchToExpressionSwitch")
        error("UnnecessarilyFullyQualified")
        disable(
            "StringSplitter",
            "ClassInitializationDeadlock",
            "BadImport",
            "AvoidCommonTypeNames",
            "ArrayRecordComponent"
        )
    }
}
