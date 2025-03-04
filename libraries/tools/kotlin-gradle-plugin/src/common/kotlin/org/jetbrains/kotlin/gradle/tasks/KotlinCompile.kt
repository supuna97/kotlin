/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTreeElement
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.util.GradleVersion
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.work.NormalizeLineEndings
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compilerRunner.GradleCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.IncrementalCompilationEnvironment
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.KotlinJvmCompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.compilerArgumentsConfigurationFlags
import org.jetbrains.kotlin.gradle.internal.tasks.allOutputFiles
import org.jetbrains.kotlin.gradle.logging.GradleErrorMessageCollector
import org.jetbrains.kotlin.gradle.logging.GradlePrintingMessageCollector
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.report.BuildReportMode
import org.jetbrains.kotlin.gradle.tasks.internal.KotlinJvmOptionsCompat
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.chainedDisallowChanges
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.propertyWithNewInstance
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.*
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.*
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import javax.inject.Inject
import kotlin.text.appendLine

@CacheableTask
abstract class KotlinCompile @Inject constructor(
    final override val compilerOptions: KotlinJvmCompilerOptions,
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory
) : AbstractKotlinCompile<K2JVMCompilerArguments>(objectFactory, workerExecutor),
    K2MultiplatformCompilationTask,
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") KotlinJvmCompileDsl,
    KotlinCompilationTask<KotlinJvmCompilerOptions>,
    UsesKotlinJavaToolchain {

    @get:Internal // covered by compiler options
    abstract override val moduleName: Property<String>

    final override val kotlinOptions: KotlinJvmOptions = KotlinJvmOptionsCompat(
        { this },
        compilerOptions
    )

    @Suppress("DEPRECATION")
    @Deprecated("Configure compilerOptions directly", replaceWith = ReplaceWith("compilerOptions"))
    override val parentKotlinOptions: Property<KotlinJvmOptions> = objectFactory
        .property(kotlinOptions)
        .chainedDisallowChanges()

    @get:Nested
    override val multiplatformStructure: K2MultiplatformStructure = objectFactory.newInstance()

    /** A package prefix that is used for locating Java sources in a directory structure with non-full-depth packages.
     *
     * Example: a Java source file with `package com.example.my.package` is located in directory `src/main/java/my/package`.
     * Then, for the Kotlin compilation to locate the source file, use package prefix `"com.example"` */
    @get:Input
    @get:Optional
    var javaPackagePrefix: String? = null

    @get:Input
    var usePreciseJavaTracking: Boolean = true
        set(value) {
            field = value
            logger.kotlinDebug { "Set $this.usePreciseJavaTracking=$value" }
        }

    @get:Internal // To support compile avoidance (ClasspathSnapshotProperties.classpathSnapshot will be used as input instead)
    abstract override val libraries: ConfigurableFileCollection

    @Deprecated(
        "Replaced with 'libraries' input",
        replaceWith = ReplaceWith("libraries"),
        level = DeprecationLevel.ERROR
    )
    @get:Internal
    var classpath: FileCollection
        set(value) = libraries.setFrom(value)
        get() = libraries

    @get:Input
    abstract val useKotlinAbiSnapshot: Property<Boolean>

    @get:Nested
    abstract val classpathSnapshotProperties: ClasspathSnapshotProperties

    /** Properties related to the `kotlin.incremental.useClasspathSnapshot` feature. */
    abstract class ClasspathSnapshotProperties {
        @get:Input
        abstract val useClasspathSnapshot: Property<Boolean>

        @get:Classpath
        @get:Incremental
        @get:Optional // Set if useClasspathSnapshot == true
        abstract val classpathSnapshot: ConfigurableFileCollection

        @get:Classpath
        @get:Incremental
        @get:Optional // Set if useClasspathSnapshot == false (to restore the existing classpath annotations when the feature is disabled)
        abstract val classpath: ConfigurableFileCollection

        @get:OutputDirectory
        @get:Optional // Set if useClasspathSnapshot == true
        abstract val classpathSnapshotDir: DirectoryProperty
    }

    override val incrementalProps: List<FileCollection>
        get() = listOf(
            sources,
            javaSources,
            scriptSources,
            androidLayoutResources,
            commonSourceSet,
            classpathSnapshotProperties.classpath,
            classpathSnapshotProperties.classpathSnapshot
        )

    @get:Internal
    final override val defaultKotlinJavaToolchain: Provider<DefaultKotlinJavaToolchain> = objectFactory
        .propertyWithNewInstance({ this })

    final override val kotlinJavaToolchainProvider: Provider<KotlinJavaToolchain> = defaultKotlinJavaToolchain.cast()

    @get:Internal
    internal abstract val associatedJavaCompileTaskTargetCompatibility: Property<String>

    @get:Internal
    internal abstract val associatedJavaCompileTaskName: Property<String>

    @get:Input
    internal abstract val jvmTargetValidationMode: Property<PropertiesProvider.JvmTargetValidationMode>

    @get:Internal
    internal val scriptDefinitions: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    @get:Optional
    internal val scriptExtensions: SetProperty<String> = objectFactory.setPropertyWithLazyValue {
        scriptDefinitions
            .map { definitionFile ->
                definitionFile.readLines().filter(String::isNotBlank)
            }
            .flatten()
    }

    private class ScriptFilterSpec(
        private val scriptExtensions: SetProperty<String>
    ) : Spec<FileTreeElement> {
        override fun isSatisfiedBy(element: FileTreeElement): Boolean {
            val extensions = scriptExtensions.get()
            return extensions.isNotEmpty() &&
                    (element.isDirectory || extensions.contains(element.file.extension))
        }
    }

    private val scriptSourceFiles = objectFactory.fileCollection()

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal open val scriptSources: FileCollection = scriptSourceFiles
        .asFileTree
        .matching { patternFilterable ->
            patternFilterable.include(ScriptFilterSpec(scriptExtensions))
        }

    init {
        incremental = true
    }

    override fun skipCondition(): Boolean = sources.isEmpty && scriptSources.isEmpty

    override fun createCompilerArgs(): K2JVMCompilerArguments =
        K2JVMCompilerArguments()

    /**
     * Workaround for those "nasty" plugins that are adding 'freeCompilerArgs' on task execution phase.
     * With properties api it is not possible to update property value after task configuration is finished.
     *
     * Marking it as `@Internal` as anyway on the configuration phase, when Gradle does task inputs snapshot,
     * this input will always be empty.
     */
    @get:Internal
    internal var executionTimeFreeCompilerArgs: List<String>? = null

    override fun setupCompilerArgs(args: K2JVMCompilerArguments, defaultsOnly: Boolean, ignoreClasspathResolutionErrors: Boolean) {
        compilerArgumentsContributor.contributeArguments(
            args, compilerArgumentsConfigurationFlags(
                defaultsOnly,
                ignoreClasspathResolutionErrors
            )
        )

        args.configureMultiplatform(
            compilerOptions,
            k1CommonSources = commonSourceSet.asFileTree,
            k2MultiplatformFragments = multiplatformStructure
        )

        if (reportingSettings().buildReportMode == BuildReportMode.VERBOSE) {
            args.reportPerf = true
        }

        val localExecutionTimeFreeCompilerArgs = executionTimeFreeCompilerArgs
        if (localExecutionTimeFreeCompilerArgs != null) {
            args.freeArgs = localExecutionTimeFreeCompilerArgs
        }
    }

    @get:Internal
    internal val compilerArgumentsContributor: CompilerArgumentsContributor<K2JVMCompilerArguments> by lazy {
        KotlinJvmCompilerArgumentsContributor(KotlinJvmCompilerArgumentsProvider(this))
    }

    override fun callCompilerAsync(
        args: K2JVMCompilerArguments,
        kotlinSources: Set<File>,
        inputChanges: InputChanges,
        taskOutputsBackup: TaskOutputsBackup?
    ) {
        validateKotlinAndJavaHasSameTargetCompatibility(args)

        val scriptSources = scriptSources.asFileTree.files
        val javaSources = javaSources.files
        val gradlePrintingMessageCollector = GradlePrintingMessageCollector(logger, args.allWarningsAsErrors)
        val gradleMessageCollector = GradleErrorMessageCollector(gradlePrintingMessageCollector, kotlinPluginVersion = getKotlinPluginVersion(logger))
        val outputItemCollector = OutputItemsCollectorImpl()
        val compilerRunner = compilerRunner.get()

        val icEnv = if (isIncrementalCompilationEnabled()) {
            logger.info(USING_JVM_INCREMENTAL_COMPILATION_MESSAGE)
            IncrementalCompilationEnvironment(
                changedFiles = getChangedFiles(inputChanges, incrementalProps),
                classpathChanges = getClasspathChanges(inputChanges),
                workingDir = taskBuildCacheableOutputDirectory.get().asFile,
                usePreciseJavaTracking = usePreciseJavaTracking,
                disableMultiModuleIC = disableMultiModuleIC,
                multiModuleICSettings = multiModuleICSettings,
                withAbiSnapshot = useKotlinAbiSnapshot.get(),
                preciseCompilationResultsBackup = preciseCompilationResultsBackup.get(),
                keepIncrementalCompilationCachesInMemory = keepIncrementalCompilationCachesInMemory.get(),
            )
        } else null

        @Suppress("ConvertArgumentToSet")
        val environment = GradleCompilerEnvironment(
            defaultCompilerClasspath, gradleMessageCollector, outputItemCollector,
            // In the incremental compiler, outputFiles will be cleaned on rebuild. However, because classpathSnapshotDir is not included in
            // TaskOutputsBackup, we don't want classpathSnapshotDir to be cleaned immediately on rebuild, and therefore we exclude it from
            // outputFiles here. (See TaskOutputsBackup's kdoc for more info.)
            outputFiles = allOutputFiles()
                    - (classpathSnapshotProperties.classpathSnapshotDir.orNull?.asFile?.let { setOf(it) } ?: emptySet()),
            reportingSettings = reportingSettings(),
            incrementalCompilationEnvironment = icEnv,
            kotlinScriptExtensions = scriptExtensions.get().toTypedArray()
        )
        logger.info("Kotlin source files: ${kotlinSources.joinToString()}")
        logger.info("Java source files: ${javaSources.joinToString()}")
        logger.info("Script source files: ${scriptSources.joinToString()}")
        logger.info("Script file extensions: ${scriptExtensions.get().joinToString()}")
        compilerRunner.runJvmCompilerAsync(
            (kotlinSources + scriptSources + javaSources).toList(),
            javaPackagePrefix,
            args,
            environment,
            defaultKotlinJavaToolchain.get().buildJvm.get().javaHome,
            taskOutputsBackup
        )
        compilerRunner.errorsFile?.also { gradleMessageCollector.flush(it) }
    }

    private fun validateKotlinAndJavaHasSameTargetCompatibility(
        args: K2JVMCompilerArguments,
    ) {
        associatedJavaCompileTaskTargetCompatibility.orNull?.let { targetCompatibility ->
            val normalizedJavaTarget = when (targetCompatibility) {
                "6" -> "1.6"
                "7" -> "1.7"
                "8" -> "1.8"
                "1.9" -> "9"
                else -> targetCompatibility
            }

            val jvmTarget = args.jvmTarget ?: JvmTarget.DEFAULT.toString()
            if (normalizedJavaTarget != jvmTarget) {
                val javaTaskName = associatedJavaCompileTaskName.get()

                val errorMessage = buildString {
                    append("'$javaTaskName' task (current target is $targetCompatibility) and ")
                    append("'$name' task (current target is $jvmTarget) ")
                    appendLine("jvm target compatibility should be set to the same Java version.")
                    if (GradleVersion.current().baseVersion < GradleVersion.version("8.0")) {
                        append("By default will become an error since Gradle 8.0+! ")
                        appendLine("Read more: https://kotl.in/gradle/jvm/target-validation")
                    }
                    appendLine("Consider using JVM toolchain: https://kotl.in/gradle/jvm/toolchain")
                }

                when (jvmTargetValidationMode.get()) {
                    PropertiesProvider.JvmTargetValidationMode.ERROR -> throw GradleException(errorMessage)
                    PropertiesProvider.JvmTargetValidationMode.WARNING -> logger.warn(errorMessage)
                    else -> Unit
                }
            }
        }
    }

    @get:Input
    val disableMultiModuleIC: Boolean by lazy {
        if (!isIncrementalCompilationEnabled() || !javaOutputDir.isPresent) {
            false
        } else {

            var illegalTaskOrNull: AbstractCompile? = null
            project.tasks.configureEach {
                if (it is AbstractCompile &&
                    it !is JavaCompile &&
                    it !is AbstractKotlinCompile<*> &&
                    javaOutputDir.get().asFile.isParentOf(it.destinationDirectory.get().asFile)
                ) {
                    illegalTaskOrNull = illegalTaskOrNull ?: it
                }
            }
            if (illegalTaskOrNull != null) {
                val illegalTask = illegalTaskOrNull!!
                logger.info(
                    "Kotlin inter-project IC is disabled: " +
                            "unknown task '$illegalTask' destination dir ${illegalTask.destinationDirectory.get().asFile} " +
                            "intersects with java destination dir $javaOutputDir"
                )
            }
            illegalTaskOrNull != null
        }
    }

    private val javaSourceFiles = objectFactory.fileCollection()

    private fun javaFilesPatternFilter(patternFilterable: PatternFilterable) {
        patternFilterable.include(
            "java".fileExtensionCasePermutations().map { "**/*.$it" }
        )
    }

    @get:Incremental
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    open val javaSources: FileCollection = objectFactory.fileCollection()
        .from(
            javaSourceFiles
                .asFileTree
                .matching(::javaFilesPatternFilter)
        )

    @get:Internal
    internal val androidLayoutResourceFiles = objectFactory.fileCollection()

    /**
     * This input is used by android-extensions plugin
     */
    @get:Incremental
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal open val androidLayoutResources: FileCollection = androidLayoutResourceFiles
        .asFileTree
        .matching { patternFilterable ->
            patternFilterable.include("xml".fileExtensionCasePermutations().map { "**/*.$it" })
        }

    // override setSource to track Java and script sources as well
    override fun source(vararg sources: Any) {
        javaSourceFiles.from(sources)
        scriptSourceFiles.from(sources)
        super.setSource(sources)
    }

    // override source to track Java and script sources as well
    override fun setSource(vararg sources: Any) {
        javaSourceFiles.from(*sources)
        scriptSourceFiles.from(*sources)
        super.setSource(*sources)
    }

    private fun getClasspathChanges(inputChanges: InputChanges): ClasspathChanges = when {
        !classpathSnapshotProperties.useClasspathSnapshot.get() -> ClasspathSnapshotDisabled
        else -> {
            val classpathSnapshotFiles = ClasspathSnapshotFiles(
                classpathSnapshotProperties.classpathSnapshot.files.toList(),
                classpathSnapshotProperties.classpathSnapshotDir.get().asFile
            )
            when {
                !inputChanges.isIncremental -> NotAvailableForNonIncrementalRun(classpathSnapshotFiles)
                // When `inputChanges.isIncremental == true`, we want to compile incrementally. However, if the incremental state (e.g.
                // lookup caches or previous classpath snapshot) is missing, we need to compile non-incrementally. There are a few cases:
                //   1. Previous compilation happened using Kotlin daemon and with IC enabled (the usual case) => Incremental state
                //      including classpath snapshot must have been produced (we have that guarantee in `IncrementalCompilerRunner`).
                //   2. Previous compilation happened using Kotlin daemon but with IC disabled (e.g., by setting `kotlin.incremental=false`)
                //      => This run will have `inputChanges.isIncremental = false` as `isIncrementalCompilationEnabled` is a task input.
                //   3. Previous compilation happened without using Kotlin daemon (set by the user or caused by a fallback).
                //   4. Previous compilation was skipped because there were no sources to compile (see `AbstractKotlinCompile.executeImpl`).
                // In case 3 and 4, it is possible that `inputChanges.isIncremental == true` in this run and incremental state is missing.
                !classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.exists() -> {
                    NotAvailableDueToMissingClasspathSnapshot(classpathSnapshotFiles)
                }

                inputChanges.getFileChanges(classpathSnapshotProperties.classpathSnapshot).none() -> NoChanges(classpathSnapshotFiles)
                else -> ToBeComputedByIncrementalCompiler(classpathSnapshotFiles)
            }
        }
    }
}