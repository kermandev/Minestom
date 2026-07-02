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
    options.compilerArgs.addAll(listOf("-Xlint:all"))
    options.errorprone {
        error("StatementSwitchToExpressionSwitch", "UnnecessarilyFullyQualified", "UnnecessaryParentheses", "DefaultCharset", "UnusedVariable", "EffectivelyPrivate", "BooleanLiteral", "UnusedMethod", "VariableNameSameAsType", "AttemptedNegativeZero", "EqualsHashCode")
        disable("StringSplitter", "ClassInitializationDeadlock", "BadImport", "AvoidCommonTypeNames", "ArrayRecordComponent", "LabelledBreakTarget", "SameNameButDifferent")
    }
}
