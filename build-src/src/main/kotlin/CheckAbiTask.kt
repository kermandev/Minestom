import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.lang.classfile.*
import java.lang.constant.ConstantDescs
import java.lang.reflect.AccessFlag
import java.util.jar.JarFile
import kotlin.jvm.optionals.getOrNull

/**
 * Simple dependency free ABI checker for Java code.
 */
abstract class CheckAbiTask : DefaultTask() {

    @get:InputFile
    @get:Optional
    abstract val oldJar: RegularFileProperty

    @get:InputFile
    abstract val newJar: RegularFileProperty

    @get:InputFiles
    abstract val sourceDirectories: ConfigurableFileCollection

    @get:Input
    abstract val rootProjectDir: Property<File>

    @get:Input
    @get:Optional
    abstract val ci: Property<Boolean>

    @TaskAction
    fun run() {
        if (!oldJar.isPresent) {
            logger.lifecycle("Skipping ABI check: Baseline JAR is not specified or could not be resolved.")
            return
        }
        val oldFile = oldJar.get().asFile
        if (!oldFile.exists()) {
            logger.lifecycle("Skipping ABI check: Baseline JAR file does not exist: ${oldFile.absolutePath}")
            return
        }
        val newFile = newJar.get().asFile

        val oldApis = extractPublicApi(oldFile)
        val newApis = extractPublicApi(newFile)

        var violations = 0

        // Check if any old public API is missing or modified in the new build
        for ((className, oldClass) in oldApis) {
            val newClass = newApis[className]
            if (newClass == null) {
                logViolation("Class removed, made package-private or considered internal: $className", null, oldClass, oldClass.line)
                violations++
                continue
            }

            // Check class level changes
            if (oldClass.`interface` != newClass.`interface`) {
                logViolation("Class type changed (class <-> interface): $className", newClass, oldClass, newClass.line)
                violations++
            }
            if (oldClass.enum != newClass.enum) {
                logViolation("Class type changed (enum status modified): $className", newClass, oldClass, newClass.line)
                violations++
            }
            if (oldClass.annotation != newClass.annotation) {
                logViolation("Class type changed (annotation status modified): $className", newClass, oldClass, newClass.line)
                violations++
            }
            if (!oldClass.final && newClass.final) {
                logViolation("Class made final: $className (breaks subclasses)", newClass, oldClass, newClass.line)
                violations++
            }
            if (!oldClass.abstract && newClass.abstract) {
                logViolation("Class made abstract: $className (breaks instantiation)", newClass, oldClass, newClass.line)
                violations++
            }
            if (!oldClass.protected && newClass.protected) {
                logViolation("Class visibility narrowed to protected: $className", newClass, oldClass, newClass.line)
                violations++
            }
            for (supertype in oldClass.supertypes) {
                if (!isSubtypeOf(className, supertype, newApis)) {
                    logViolation("Class no longer implements/extends $supertype: $className", newClass, oldClass, newClass.line)
                    violations++
                }
            }

            // Check methods
            for ((methodKey, oldMethod) in oldClass.methods) {
                val newMethod = newClass.methods[methodKey] ?: lookupMethod(className, methodKey, newApis)
                if (newMethod == null) {
                    if (hasUnscannedSupertype(className, newApis)) {
                        logWarning("Cannot verify binary compatibility because the class hierarchy contains external/unscanned supertypes: Method removed -> $className#$methodKey", newClass, oldClass, newClass.line)
                        continue
                    }
                    logViolation("Public/protected method removed or signature changed: $className#$methodKey", newClass, oldClass, newClass.line)
                    violations++
                    continue
                }

                if (!oldMethod.protected && newMethod.protected) {
                    logViolation("Method visibility narrowed to protected: $className#$methodKey", newClass, oldClass, newMethod.line)
                    violations++
                }
                if (oldMethod.static != newMethod.static) {
                    logViolation("Method static status changed: $className#$methodKey", newClass, oldClass, newMethod.line)
                    violations++
                }
                if (!oldMethod.final && newMethod.final) {
                    logViolation("Method made final: $className#$methodKey (breaks subclasses)", newClass, oldClass, newMethod.line)
                    violations++
                }
                if (!oldMethod.abstract && newMethod.abstract) {
                    logViolation("Method made abstract: $className#$methodKey (breaks implementing classes/subclasses)", newClass, oldClass, newMethod.line)
                    violations++
                }
            }

            // Check fields
            for ((fieldKey, oldField) in oldClass.fields) {
                val newField = newClass.fields[fieldKey] ?: lookupField(className, fieldKey, newApis)
                if (newField == null) {
                    if (hasUnscannedSupertype(className, newApis)) {
                        logWarning("Cannot verify binary compatibility because the class hierarchy contains external/unscanned supertypes: Field removed or type changed -> $className#$fieldKey", newClass, oldClass, newClass.line)
                        continue
                    }
                    logViolation("Public/protected field removed or type changed: $className#$fieldKey", newClass, oldClass, newClass.line)
                    violations++
                    continue
                }

                if (!oldField.protected && newField.protected) {
                    logViolation("Field visibility narrowed to protected: $className#$fieldKey", newClass, oldClass, newField.line)
                    violations++
                }
                if (oldField.static != newField.static) {
                    logViolation("Field static status changed: $className#$fieldKey", newClass, oldClass, newField.line)
                    violations++
                }
                if (!oldField.final && newField.final) {
                    logViolation("Field made final: $className#$fieldKey", newClass, oldClass, newField.line)
                    violations++
                }
            }
        }

        // Check for newly added abstract methods in interfaces and non final classes
        for ((className, newClass) in newApis) {
            val oldClass = oldApis[className] ?: continue // New classes are always compatible

            // Determine if we need to check this class/interface
            val checkInterface = newClass.`interface` && !newClass.nonExtendable
            val checkClass = !newClass.`interface` && !newClass.final && !newClass.nonExtendable

            if (!checkInterface && !checkClass) continue

            for ((methodKey, newMethod) in newClass.methods) {
                // If the new method is abstract, not internal, and did not exist in the baseline version
                if (!newMethod.abstract) continue
                val oldMethod = lookupMethod(className, methodKey, oldApis)
                if (oldMethod != null) {
                    if (!oldMethod.abstract) {
                        logViolation("Inherited concrete method made abstract: $className#$methodKey (breaks subclasses)", newClass, oldClass, newMethod.line)
                        violations++
                    }
                    continue
                }

                if (newClass.`interface`) {
                    logViolation("New abstract method added to interface: $className#$methodKey (breaks implementing classes)", newClass, oldClass, newMethod.line)
                } else {
                    logViolation("New abstract method added to extendable class: $className#$methodKey (breaks subclasses)", newClass, oldClass, newMethod.line)
                }
                violations++
            }
        }

        if (violations > 0) {
            throw GradleException("ABI Check failed with $violations binary compatibility violations.")
        }
    }

    private fun logViolation(message: String, newClass: ClassApi?, oldClass: ClassApi, line: Int) {
        logger.error(message)
        val sourceFile = newClass?.sourceFile ?: oldClass.sourceFile
        if (ci.getOrElse(false) && sourceFile != null) {
            println("::error file=$sourceFile,line=$line::$message")
        }
    }

    private fun logWarning(message: String, newClass: ClassApi?, oldClass: ClassApi, line: Int) {
        logger.warn(message)
        val sourceFile = newClass?.sourceFile ?: oldClass.sourceFile
        if (ci.getOrElse(false) && sourceFile != null) {
            println("::warning file=$sourceFile,line=$line::$message")
        }
    }

    private data class ElementApi(val descriptor: String, val abstract: Boolean, val static: Boolean, val final: Boolean, val protected: Boolean, val line: Int)

    private data class ClassApi(val `interface`: Boolean, // Annoying reserved keyword ugh
                                val enum: Boolean, val annotation: Boolean, val final: Boolean, val abstract: Boolean, val nonExtendable: Boolean, val protected: Boolean, val supertypes: Set<String>, val methods: Map<String, ElementApi>, val fields: Map<String, ElementApi>, val outerClassName: String?, val sourceFile: String?, val line: Int)

    private fun extractPublicApi(jarFile: File): Map<String, ClassApi> {
        val apis = mutableMapOf<String, ClassApi>()
        val classFileParser = ClassFile.of()

        JarFile(jarFile).use { jar ->
            for (entry in jar.entries().asSequence()) {
                if (!entry.name.endsWith(".class")) continue

                jar.getInputStream(entry).use { stream ->
                    val bytes = stream.readAllBytes()
                    val classModel: ClassModel = classFileParser.parse(bytes)

                    val classFlags = classModel.flags().flags()
                    // Skip non-public / non-protected classes
                    if (hasPrivateAccess(classFlags)) return@use

                    val className = classModel.thisClass().asInternalName()
                    // Omit internal classes
                    if (isInternal(classModel)) return@use
                    val isClassInterface = AccessFlag.INTERFACE in classFlags
                    val isClassEnum = AccessFlag.ENUM in classFlags
                    val isClassAnnotation = AccessFlag.ANNOTATION in classFlags
                    val isClassFinal = AccessFlag.FINAL in classFlags
                    val isClassAbstract = AccessFlag.ABSTRACT in classFlags
                    val isClassNonExtendable = isNonExtendable(classModel)
                    val isClassProtected = AccessFlag.PROTECTED in classFlags

                    val supertypes = mutableSetOf<String>()
                    classModel.superclass().ifPresent { supertypes.add(it.asInternalName()) }
                    classModel.interfaces().forEach { supertypes.add(it.asInternalName()) }

                    // Determine the outer class if we are nested
                    val outerClassName = classModel.findAttribute(Attributes.innerClasses()).getOrNull()?.classes()?.firstOrNull { it.innerClass().name().stringValue() == className }?.outerClass()?.getOrNull()?.name()?.stringValue()

                    // Reconstruct source file path
                    val sourceFileName = classModel.findAttribute(Attributes.sourceFile()).map { it.sourceFile().stringValue() }.orElse(null)
                    val fullSourcePath = if (sourceFileName != null) {
                        val packagePath = className.substringBeforeLast('/', "")
                        val path = if (packagePath.isEmpty()) sourceFileName else "$packagePath/$sourceFileName"

                        // Find where the file exists locally across all configured source directories
                        val resolvedFile = sourceDirectories.files.asSequence().map { File(it, path) }.firstOrNull { it.exists() }

                        // Fallback to the first source directory if not found locally
                        val fileToUse = resolvedFile ?: sourceDirectories.files.firstOrNull()?.let { File(it, path) }

                        fileToUse?.relativeTo(rootProjectDir.get())?.path
                    } else null

                    // Find class declaration line (minimum line of any method)
                    val classLine = classModel.methods().asSequence().mapNotNull { it.code().orElse(null) }.mapNotNull { it.findAttribute(Attributes.lineNumberTable()).orElse(null) }.flatMap { it.lineNumbers() }.minOfOrNull { it.lineNumber() }
                            ?: 1

                    val methods = mutableMapOf<String, ElementApi>()
                    val fields = mutableMapOf<String, ElementApi>()

                    // Inspect methods
                    for (method: MethodModel in classModel.methods()) {
                        val flags = method.flags().flags()
                        if (hasPrivateAccess(flags)) continue
                        if (isInternal(method)) continue

                        val methodName = method.methodName().stringValue()
                        if (methodName == ConstantDescs.CLASS_INIT_NAME) continue
                        val descriptor = method.methodType().stringValue()
                        val isMethodAbstract = AccessFlag.ABSTRACT in flags
                        val isMethodStatic = AccessFlag.STATIC in flags
                        val isMethodFinal = AccessFlag.FINAL in flags
                        val isMethodProtected = AccessFlag.PROTECTED in flags

                        val code = method.code().orElse(null)
                        val lineNumbers = code?.findAttribute(Attributes.lineNumberTable())?.orElse(null)
                        val methodLine = lineNumbers?.lineNumbers()?.firstOrNull()?.lineNumber() ?: classLine

                        methods["$methodName$descriptor"] = ElementApi(descriptor, isMethodAbstract, isMethodStatic, isMethodFinal, isMethodProtected, methodLine)
                    }

                    // Inspect fields
                    for (field: FieldModel in classModel.fields()) {
                        val flags = field.flags().flags()
                        if (hasPrivateAccess(flags)) continue
                        if (isInternal(field)) continue

                        val fieldName = field.fieldName().stringValue()
                        val descriptor = field.fieldType().stringValue()
                        val isFieldStatic = AccessFlag.STATIC in flags
                        val isFieldFinal = AccessFlag.FINAL in flags
                        val isFieldProtected = AccessFlag.PROTECTED in flags
                        fields["$fieldName:$descriptor"] = ElementApi(descriptor, false, isFieldStatic, isFieldFinal, isFieldProtected, classLine)
                    }

                    apis[className] = ClassApi(
                            isClassInterface, isClassEnum, isClassAnnotation, isClassFinal, isClassAbstract, isClassNonExtendable, isClassProtected, supertypes, methods, fields, outerClassName, fullSourcePath, classLine,
                    )
                }
            }
        }
        // If the parent class doesn't we omit them, has to be done here as we can read in any order
        return apis.filterValues { hasPublicEnclosure(it, apis) }
    }

    private fun hasPrivateAccess(flags: Set<AccessFlag>): Boolean {
        return AccessFlag.PUBLIC !in flags && AccessFlag.PROTECTED !in flags
    }

    // We don't care about internal or experimental marked methods
    private fun isInternal(element: AttributedElement): Boolean {
        return hasAnnotation(element, $$"Lorg/jetbrains/annotations/ApiStatus$Internal;", $$"Lorg/jetbrains/annotations/ApiStatus$Experimental;")
    }

    private fun isNonExtendable(element: AttributedElement): Boolean {
        if (hasAnnotation(element, $$"Lorg/jetbrains/annotations/ApiStatus$NonExtendable;")) return true
        if (element !is ClassModel) return false
        return element.findAttribute(Attributes.permittedSubclasses()).isPresent
    }

    private fun hasAnnotation(element: AttributedElement, vararg descriptors: String): Boolean {
        val visible = element.findAttribute(Attributes.runtimeVisibleAnnotations()).orElse(null)
        val invisible = element.findAttribute(Attributes.runtimeInvisibleAnnotations()).orElse(null)

        val annotations = (visible?.annotations() ?: emptyList()) + (invisible?.annotations() ?: emptyList())
        for (anno in annotations) {
            val desc = anno.classSymbol().descriptorString()
            if (desc in descriptors) {
                return true
            }
        }
        return false
    }

    private fun hasPublicEnclosure(classApi: ClassApi, apis: Map<String, ClassApi>): Boolean {
        val parent = classApi.outerClassName ?: return true
        val parentClass = apis[parent] ?: return false
        return hasPublicEnclosure(parentClass, apis)
    }

    private fun lookupMethod(className: String, key: String, apis: Map<String, ClassApi>): ElementApi? = apis[className]?.let { api ->
        api.methods[key] ?: api.supertypes.firstNotNullOfOrNull { lookupMethod(it, key, apis) }
    }

    private fun lookupField(className: String, key: String, apis: Map<String, ClassApi>): ElementApi? = apis[className]?.let { api ->
        api.fields[key] ?: api.supertypes.firstNotNullOfOrNull { lookupField(it, key, apis) }
    }

    private fun isSubtypeOf(className: String, supertype: String, apis: Map<String, ClassApi>): Boolean {
        if (className == supertype) return true
        val api = apis[className] ?: return false
        if (supertype in api.supertypes) return true
        return api.supertypes.any { isSubtypeOf(it, supertype, apis) }
    }

    private fun hasUnscannedSupertype(className: String, apis: Map<String, ClassApi>): Boolean {
        // If a supertype isn't in our map, it's external/unscanned, if it's not an object cause those are noisy
        val api = apis[className] ?: return className != "java/lang/Object"
        return api.supertypes.any { it !in apis || hasUnscannedSupertype(it, apis) }
    }
}
