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
import java.util.zip.ZipFile
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

    @get:InputFiles
    @get:Classpath
    @get:Optional
    abstract val classpath: ConfigurableFileCollection

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

            fun check(condition: Boolean, message: String, line: Int = newClass?.line ?: oldClass.line) {
                if (condition) {
                    logger.error(message)
                    val sourceFile = newClass?.sourceFile ?: oldClass.sourceFile
                    if (System.getenv("CI") != null && sourceFile != null) {
                        println("::error file=$sourceFile,line=$line::$message")
                    }
                    violations++
                }
            }

            if (newClass == null) {
                check(true, "Class removed, made package-private or considered internal: $className")
                continue
            }

            // Check class level changes
            check(oldClass.`interface` != newClass.`interface`, "Class type changed (class <-> interface): $className")
            check(oldClass.enum != newClass.enum, "Class type changed (enum status modified): $className")
            check(oldClass.annotation != newClass.annotation, "Class type changed (annotation status modified): $className")
            check(!oldClass.final && newClass.final, "Class made final: $className (breaks subclasses)")
            check(!oldClass.abstract && newClass.abstract, "Class made abstract: $className (breaks instantiation)")
            check(!oldClass.protected && newClass.protected, "Class visibility narrowed to protected: $className")
            check(!oldClass.nonExtendable && newClass.nonExtendable, "Class made non-extendable (sealed or @NonExtendable): $className")

            for (supertype in oldClass.supertypes) {
                check(!isSubtypeOf(className, supertype, newApis), "Class no longer implements/extends $supertype: $className")
            }

            // Check methods
            for ((methodKey, oldMethod) in oldClass.methods) {
                val newMethod = newClass.methods[methodKey] ?: if (methodKey.startsWith(ConstantDescs.INIT_NAME)) null else lookupMethod(className, methodKey, newApis)
                if (newMethod == null) {
                    check(true, "Public/protected method removed or signature changed: $className#$methodKey")
                    continue
                }

                check(!oldMethod.protected && newMethod.protected, "Method visibility narrowed to protected: $className#$methodKey", newMethod.line)
                check(oldMethod.static != newMethod.static, "Method static status changed: $className#$methodKey", newMethod.line)
                if (!newClass.final) {
                    check(!oldMethod.final && newMethod.final, "Method made final: $className#$methodKey (breaks subclasses)", newMethod.line)
                }
                check(!oldMethod.abstract && newMethod.abstract, "Method made abstract: $className#$methodKey (breaks implementing classes/subclasses)", newMethod.line)
            }

            // Check fields
            for ((fieldKey, oldField) in oldClass.fields) {
                val newField = newClass.fields[fieldKey] ?: lookupField(className, fieldKey, newApis)
                if (newField == null) {
                    check(true, "Public/protected field removed or type changed: $className#$fieldKey")
                    continue
                }

                check(!oldField.protected && newField.protected, "Field visibility narrowed to protected: $className#$fieldKey", newField.line)
                check(oldField.static != newField.static, "Field static status changed: $className#$fieldKey", newField.line)
                check(!oldField.final && newField.final, "Field made final: $className#$fieldKey", newField.line)
                check(oldField.constantValue != newField.constantValue, "Constant field value changed: $className#$fieldKey (old: ${oldField.constantValue}, new: ${newField.constantValue})", newField.line)
            }
        }

        // Check for newly added abstract methods in interfaces and non final classes
        for ((className, newClass) in newApis) {
            val oldClass = oldApis[className] ?: continue // New classes are always compatible

            fun check(condition: Boolean, message: String, line: Int = newClass.line) {
                if (condition) {
                    logger.error(message)
                    val sourceFile = newClass.sourceFile ?: oldClass.sourceFile
                    if (System.getenv("CI") != null && sourceFile != null) {
                        println("::error file=$sourceFile,line=$line::$message")
                    }
                    violations++
                }
            }

            // Determine if we need to check this class/interface
            val checkInterface = newClass.`interface` && !newClass.nonExtendable
            val checkClass = !newClass.`interface` && !newClass.final && !newClass.nonExtendable

            if (!checkInterface && !checkClass) continue

            for ((methodKey, newMethod) in newClass.methods) {
                // If the new method is abstract, not internal, and did not exist in the baseline version
                if (!newMethod.abstract) continue
                val oldMethod = lookupMethod(className, methodKey, oldApis)
                if (oldMethod != null) {
                    check(!oldMethod.abstract, "Inherited concrete method made abstract: $className#$methodKey (breaks subclasses)", newMethod.line)
                    continue
                }

                val entityType = if (newClass.`interface`) "interface" else "extendable class"
                val breaksWho = if (newClass.`interface`) "implementing classes" else "subclasses"
                check(true, "New abstract method added to $entityType: $className#$methodKey (breaks $breaksWho)", newMethod.line)
            }
        }

        if (violations > 0) {
            throw GradleException("ABI Check failed with $violations binary compatibility violations.")
        }
    }

    private data class ElementApi(val descriptor: String, val abstract: Boolean, val static: Boolean, val final: Boolean, val protected: Boolean, val line: Int, val constantValue: String? = null)

    private data class ClassApi(val `interface`: Boolean, // Annoying reserved keyword ugh
                                val enum: Boolean, val annotation: Boolean, val final: Boolean, val abstract: Boolean, val nonExtendable: Boolean, val protected: Boolean, val supertypes: Set<String>, val methods: Map<String, ElementApi>, val fields: Map<String, ElementApi>, val outerClassName: String?, val sourceFile: String?, val line: Int)

    @Transient
    private var classIndexMap: Map<String, Pair<File, String>>? = null

    private fun getClassIndex(): Map<String, Pair<File, String>> {
        var index = classIndexMap
        if (index == null) {
            index = buildMap {
                for (file in classpath.files) {
                    if (!file.exists()) continue
                    val isJmod = file.extension == "jmod"
                    if (!isJmod && file.extension != "jar") continue
                    try {
                        ZipFile(file).use { zip ->
                            for (entry in zip.entries().asSequence()) {
                                val className = when {
                                    isJmod && entry.name.startsWith("classes/") && entry.name.endsWith(".class") ->
                                        entry.name.removePrefix("classes/").removeSuffix(".class")

                                    !isJmod && entry.name.endsWith(".class") ->
                                        entry.name.removeSuffix(".class")

                                    else -> continue
                                }
                                putIfAbsent(className, file to entry.name)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            classIndexMap = index
        }
        return index
    }

    private fun loadClassBytes(className: String): ByteArray? {
        val (archive, entryPath) = getClassIndex()[className] ?: return null
        return try {
            ZipFile(archive).use { zip ->
                zip.getInputStream(zip.getEntry(entryPath) ?: return null).readAllBytes()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveClass(className: String, apis: MutableMap<String, ClassApi>): ClassApi? {
        val existing = apis[className]
        if (existing != null) return existing

        val bytes = loadClassBytes(className) ?: return null
        val classModel = ClassFile.of().parse(bytes)
        val api = parseClassApi(classModel)
        apis[className] = api
        return api
    }

    private fun parseClassApi(classModel: ClassModel): ClassApi {
        val flags = classModel.flags()
        val isClassInterface = flags.has(AccessFlag.INTERFACE)
        val isClassEnum = flags.has(AccessFlag.ENUM)
        val isClassAnnotation = flags.has(AccessFlag.ANNOTATION)
        val isClassFinal = flags.has(AccessFlag.FINAL)
        val isClassAbstract = flags.has(AccessFlag.ABSTRACT)
        val isClassNonExtendable = isNonExtendable(classModel)
        val isClassProtected = flags.has(AccessFlag.PROTECTED)

        val supertypes = buildSet {
            classModel.superclass().getOrNull()?.let { add(it.asInternalName()) }
            classModel.interfaces().forEach { add(it.asInternalName()) }
        }

        val className = classModel.thisClass().asInternalName()
        val outerClassName = classModel.findAttribute(Attributes.innerClasses()).getOrNull()
                ?.classes()?.firstOrNull { it.innerClass().name().stringValue() == className }
                ?.outerClass()?.getOrNull()?.name()?.stringValue()

        // Reconstruct source file path
        val sourceFileName = classModel.findAttribute(Attributes.sourceFile()).map { it.sourceFile().stringValue() }.orElse(null)
        val fullSourcePath = if (sourceFileName != null) {
            val packagePath = className.substringBeforeLast('/', "")
            val path = if (packagePath.isEmpty()) sourceFileName else "$packagePath/$sourceFileName"
            val file = sourceDirectories.files.map { File(it, path) }.firstOrNull { it.exists() }
                ?: sourceDirectories.files.firstOrNull()?.let { File(it, path) }
            file?.relativeToOrNull(rootProjectDir.get())?.path
        } else null

        // Find class declaration line (minimum line of any method)
        val classLine = classModel.methods().asSequence()
                .mapNotNull { it.code().getOrNull()?.findAttribute(Attributes.lineNumberTable())?.getOrNull() }
                .flatMap { it.lineNumbers() }.minOfOrNull { it.lineNumber() } ?: 1

        val methods = classModel.methods().filter { method ->
            val methodFlags = method.flags()
            !hasPrivateAccess(methodFlags) && !isInternal(method) &&
                    method.methodName().stringValue() != ConstantDescs.CLASS_INIT_NAME
        }.associate { method ->
            val methodFlags = method.flags()
            val methodName = method.methodName().stringValue()
            val descriptor = method.methodType().stringValue()
            val methodLine = method.code().getOrNull()?.findAttribute(Attributes.lineNumberTable())?.getOrNull()
                ?.lineNumbers()?.firstOrNull()?.lineNumber() ?: classLine
            "$methodName$descriptor" to ElementApi(
                    descriptor,
                    methodFlags.has(AccessFlag.ABSTRACT),
                    methodFlags.has(AccessFlag.STATIC),
                    methodFlags.has(AccessFlag.FINAL),
                    methodFlags.has(AccessFlag.PROTECTED),
                    methodLine
            )
        }

        val fields = classModel.fields().filter { field ->
            val fieldFlags = field.flags()
            !hasPrivateAccess(fieldFlags) && !isInternal(field)
        }.associate { field ->
            val fieldFlags = field.flags()
            val fieldName = field.fieldName().stringValue()
            val descriptor = field.fieldType().stringValue()
            val constantValue = field.findAttribute(Attributes.constantValue()).getOrNull()?.constant()?.constantValue()?.toString()
            "$fieldName:$descriptor" to ElementApi(
                    descriptor,
                    false,
                    fieldFlags.has(AccessFlag.STATIC),
                    fieldFlags.has(AccessFlag.FINAL),
                    fieldFlags.has(AccessFlag.PROTECTED),
                    classLine,
                    constantValue
            )
        }

        return ClassApi(
                isClassInterface, isClassEnum, isClassAnnotation, isClassFinal, isClassAbstract,
                isClassNonExtendable, isClassProtected, supertypes, methods, fields, outerClassName, fullSourcePath, classLine
        )
    }

    private fun extractPublicApi(jarFile: File): MutableMap<String, ClassApi> {
        val classFileParser = ClassFile.of()
        val apis = ZipFile(jarFile).use { jar ->
            jar.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .mapNotNull { entry ->
                    jar.getInputStream(entry).use { stream ->
                        val classModel = classFileParser.parse(stream.readAllBytes())
                        if (hasPrivateAccess(classModel.flags()) || isInternal(classModel)) null
                        else classModel.thisClass().asInternalName() to parseClassApi(classModel)
                    }
                }.toMap()
        }
        return apis.filterValues { hasPublicEnclosure(it, apis) }.toMutableMap()
    }

    private fun hasPrivateAccess(flags: AccessFlags): Boolean {
        return !flags.has(AccessFlag.PUBLIC) && !flags.has(AccessFlag.PROTECTED)
    }

    private fun isInternal(element: AttributedElement): Boolean =
        hasAnnotation(element, $$"Lorg/jetbrains/annotations/ApiStatus$Internal;", $$"Lorg/jetbrains/annotations/ApiStatus$Experimental;")

    private fun isNonExtendable(element: AttributedElement): Boolean =
        hasAnnotation(element, $$"Lorg/jetbrains/annotations/ApiStatus$NonExtendable;") ||
                (element is ClassModel && element.findAttribute(Attributes.permittedSubclasses()).isPresent)

    private fun hasAnnotation(element: AttributedElement, vararg descriptors: String): Boolean {
        val visible = element.findAttribute(Attributes.runtimeVisibleAnnotations()).getOrNull()
        val invisible = element.findAttribute(Attributes.runtimeInvisibleAnnotations()).getOrNull()

        val annotations = (visible?.annotations() ?: emptyList()) + (invisible?.annotations() ?: emptyList())
        return annotations.any { it.classSymbol().descriptorString() in descriptors }
    }

    private fun hasPublicEnclosure(classApi: ClassApi, apis: Map<String, ClassApi>): Boolean =
        classApi.outerClassName?.let { apis[it]?.let { parent -> hasPublicEnclosure(parent, apis) } ?: false } ?: true

    private fun lookupMethod(className: String, key: String, apis: MutableMap<String, ClassApi>): ElementApi? =
        resolveClass(className, apis)?.let { api ->
            api.methods[key] ?: api.supertypes.firstNotNullOfOrNull { lookupMethod(it, key, apis) }
        }

    private fun lookupField(className: String, key: String, apis: MutableMap<String, ClassApi>): ElementApi? =
        resolveClass(className, apis)?.let { api ->
            api.fields[key] ?: api.supertypes.firstNotNullOfOrNull { lookupField(it, key, apis) }
        }

    private fun isSubtypeOf(className: String, supertype: String, apis: MutableMap<String, ClassApi>): Boolean {
        if (className == supertype) return true
        val api = resolveClass(className, apis) ?: return false
        return supertype in api.supertypes || api.supertypes.any { isSubtypeOf(it, supertype, apis) }
    }
}
