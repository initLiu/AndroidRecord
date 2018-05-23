Android Gradle Plugin定义在base-gradle_2.3.0-build-system\gradle\src\main\resources\META-INF\gradle-plugins目录下，在该目录下可以看到我们在build.gradle文件中经常引用的插件
android-library.properties和com.android.application.properties。  
首先从application插件说起。  
# 1.Application
打开com.android.application.properties文件后，可以看到文件中只有一行代码
```xml
implementation-class=com.android.build.gradle.AppPlugin
```
这里定义了application插件的实现类为```com.android.build.gradle.AppPlugin```
```java
/**
 * Gradle plugin class for 'application' projects.
 */
public class AppPlugin extends BasePlugin implements Plugin<Project> {
}

/**
 * Base class for all Android plugins
 */
public abstract class BasePlugin implements ToolingRegistryProvider {
}
```
插件生命周期始于apply()方法
```java
/**
 * Base class for all Android plugins
 */
public abstract class BasePlugin implements ToolingRegistryProvider {
    ...
    protected void apply(@NonNull Project project) {
        ...
        //初始化projject
        threadRecorder.record(
                ExecutionType.BASE_PLUGIN_PROJECT_CONFIGURE,
                project.getPath(),
                null,
                this::configureProject);
                /创建Android Extension
        //1.1初始化配置，buildType,productFlavor等
        threadRecorder.record(
                ExecutionType.BASE_PLUGIN_PROJECT_BASE_EXTENSION_CREATION,
                project.getPath(),
                null,
                this::configureExtension);

        //1.2创建task
        threadRecorder.record(
                ExecutionType.BASE_PLUGIN_PROJECT_TASKS_CREATION,
                project.getPath(),
                null,
                this::createTasks);
    }
}
```
## 1.1初始化配置，buildType,productFlavor等
```java
//BasePlugin.java
private void configureExtension() {
    //创建buildTypeContainer，用来创建relase、debug等type，对应gradle文件中的
    //buildTypes {
    //    debug {...}
    //    release {...}
    // }
    final NamedDomainObjectContainer<BuildType> buildTypeContainer =
            project.container(
                    BuildType.class,
                    new BuildTypeFactory(instantiator, project, project.getLogger()));

    //创建productFlavorContainer，用来创建不同的产品风味，对应gradle文件中的
    //productFlavors {
    //    demo {
    //      // Assigns this product flavor to the "mode" flavor dimension.
    //      dimension "mode"
    //      ...
    //    }
    //
    //    full {
    //      dimension "mode"
    //      ...
    //     }
    // }
    final NamedDomainObjectContainer<ProductFlavor> productFlavorContainer =
            project.container(
                    ProductFlavor.class,
                    new ProductFlavorFactory(
                            instantiator, project, project.getLogger(), extraModelInfo));

    //signingConfigContainer，用来创建签名
    final NamedDomainObjectContainer<SigningConfig> signingConfigContainer =
            project.container(SigningConfig.class, new SigningConfigFactory(instantiator));

    //1.1.1创建AppExtension，对应gradle文件中的
    // android{
    //    ....
    //    ...
    // }
    extension =
            createExtension(
                    project,
                    instantiator,
                    androidBuilder,
                    sdkHandler,
                    buildTypeContainer,
                    productFlavorContainer,
                    signingConfigContainer,
                    extraModelInfo);

    // create the default mapping configuration.
    project.getConfigurations()
            .create("default" + VariantDependencies.CONFIGURATION_MAPPING)
            .setDescription("Configuration for default mapping artifacts.");
    project.getConfigurations().create("default" + VariantDependencies.CONFIGURATION_METADATA)
            .setDescription("Metadata for the produced APKs.");

    dependencyManager = new DependencyManager(
            project,
            extraModelInfo,
            sdkHandler);

    ndkHandler = new NdkHandler(
            project.getRootDir(),
            null, /* compileSkdVersion, this will be set in afterEvaluate */
            "gcc",
            "" /*toolchainVersion*/);

    //创建ApplicationTaskManager
    taskManager =
            createTaskManager(
                    project,
                    androidBuilder,
                    dataBindingBuilder,
                    extension,
                    sdkHandler,
                    ndkHandler,
                    dependencyManager,
                    registry,
                    threadRecorder);

    //管理构建变体的(根据buildtype和productflavor的组合可以形成多个构变体)
    variantFactory = createVariantFactory(instantiator, androidBuilder, extension);

    variantManager =
            new VariantManager(
                    project,
                    androidBuilder,
                    extension,
                    variantFactory,
                    taskManager,
                    instantiator,
                    threadRecorder);

    // Register a builder for the custom tooling model
    ModelBuilder modelBuilder = new ModelBuilder(
            androidBuilder,
            variantManager,
            taskManager,
            extension,
            extraModelInfo,
            ndkHandler,
            new NativeLibraryFactoryImpl(ndkHandler),
            getProjectType(),
            AndroidProject.GENERATION_ORIGINAL);
    registry.register(modelBuilder);

    // Register a builder for the native tooling model
    NativeModelBuilder nativeModelBuilder = new NativeModelBuilder(variantManager);
    registry.register(nativeModelBuilder);

    // map the whenObjectAdded callbacks on the containers.
    signingConfigContainer.whenObjectAdded(variantManager::addSigningConfig);

    //当向buildTypes{}中添加数据是，会触发whenObjectAdded，然后将buildType添加到variantManager中
    buildTypeContainer.whenObjectAdded(
            buildType -> {
                SigningConfig signingConfig =
                        signingConfigContainer.findByName(BuilderConstants.DEBUG);
                buildType.init(signingConfig);
                variantManager.addBuildType(buildType);
            });
    //当向productFlavors{}中添加数据是，会触发whenObjectAdded，然后将ProductFlavor添加到variantManager中
    productFlavorContainer.whenObjectAdded(variantManager::addProductFlavor);

    // map whenObjectRemoved on the containers to throw an exception.
    signingConfigContainer.whenObjectRemoved(
            new UnsupportedAction("Removing signingConfigs is not supported."));
    buildTypeContainer.whenObjectRemoved(
            new UnsupportedAction("Removing build types is not supported."));
    productFlavorContainer.whenObjectRemoved(
            new UnsupportedAction("Removing product flavors is not supported."));

    // create default Objects, signingConfig first as its used by the BuildTypes.
    variantFactory.createDefaultComponents(
            buildTypeContainer, productFlavorContainer, signingConfigContainer);
}

/**
 * Class to create, manage variants.
 */
public class VariantManager implements VariantModel {
    ...
    /**
     * Adds new BuildType, creating a BuildTypeData, and the associated source set,
     * and adding it to the map.
     * @param buildType the build type.
     */
    public void addBuildType(@NonNull CoreBuildType buildType) {
        String name = buildType.getName();
        ...
        buildTypes.put(name, buildTypeData);
    }

    /**
     * Adds a new ProductFlavor, creating a ProductFlavorData and associated source sets,
     * and adding it to the map.
     *
     * @param productFlavor the product flavor
     */
    public void addProductFlavor(@NonNull CoreProductFlavor productFlavor) {
        String name = productFlavor.getName();
        ...
        ProductFlavorData<CoreProductFlavor> productFlavorData =
                new ProductFlavorData<>(
                        productFlavor,
                        mainSourceSet,
                        androidTestSourceSet,
                        unitTestSourceSet,
                        project);

        productFlavors.put(productFlavor.getName(), productFlavorData);
    }
}
```
### 1.1.1创建AppExtension
```java
//AppPlugin.java
protected BaseExtension createExtension(
    @NonNull Project project,
    @NonNull Instantiator instantiator,
    @NonNull AndroidBuilder androidBuilder,
    @NonNull SdkHandler sdkHandler,
    @NonNull NamedDomainObjectContainer<BuildType> buildTypeContainer,
    @NonNull NamedDomainObjectContainer<ProductFlavor> productFlavorContainer,
    @NonNull NamedDomainObjectContainer<SigningConfig> signingConfigContainer,
    @NonNull ExtraModelInfo extraModelInfo) {
return project.getExtensions()
        .create(
                "android",
                AppExtension.class,
                project,
                instantiator,
                androidBuilder,
                sdkHandler,
                buildTypeContainer,
                productFlavorContainer,
                signingConfigContainer,
                extraModelInfo);
}

/**
 * {@code android} extension for {@code com.android.application} projects.
 */
public class AppExtension extends TestedExtension {
    public AppExtension(
            @NonNull Project project,
            @NonNull Instantiator instantiator,
            @NonNull AndroidBuilder androidBuilder,
            @NonNull SdkHandler sdkHandler,
            @NonNull NamedDomainObjectContainer<BuildType> buildTypes,
            @NonNull NamedDomainObjectContainer<ProductFlavor> productFlavors,
            @NonNull NamedDomainObjectContainer<SigningConfig> signingConfigs,
            @NonNull ExtraModelInfo extraModelInfo) {
        super(
                project,
                instantiator,
                androidBuilder,
                sdkHandler,
                buildTypes,
                productFlavors,
                signingConfigs,
                extraModelInfo,
                false);
    }
    ...
}

public abstract class BaseExtension implements AndroidConfig {
    BaseExtension(
            @NonNull final Project project,
            @NonNull Instantiator instantiator,
            @NonNull AndroidBuilder androidBuilder,
            @NonNull SdkHandler sdkHandler,
            @NonNull NamedDomainObjectContainer<BuildType> buildTypes,
            @NonNull NamedDomainObjectContainer<ProductFlavor> productFlavors,
            @NonNull NamedDomainObjectContainer<SigningConfig> signingConfigs,
            @NonNull ExtraModelInfo extraModelInfo,
            final boolean publishPackage) {
        ...
        //创建configuration
        //就是在dependencies里面引用aar或jar时使用的compile/apk/provided等
        sourceSetsContainer.whenObjectAdded(
                new Action<AndroidSourceSet>() {
                    @Override
                    public void execute(AndroidSourceSet sourceSet) {
                        ConfigurationContainer configurations = project.getConfigurations();
                        
                        //sourceSet--->com.android.build.gradle.internal.api.DefaultAndroidSourceSet
                        createConfiguration(
                                configurations,
                                sourceSet.getCompileConfigurationName(),//compile
                                "Classpath for compiling the " + sourceSet.getName() + " sources.");

                        String packageConfigDescription;
                        if (publishPackage) {
                            packageConfigDescription =
                                    "Classpath only used when publishing '"
                                            + sourceSet.getName()
                                            + "'.";
                        } else {
                            packageConfigDescription =
                                    "Classpath packaged with the compiled '"
                                            + sourceSet.getName()
                                            + "' classes.";
                        }
                        createConfiguration(
                                configurations,
                                sourceSet.getPackageConfigurationName(),//publish
                                packageConfigDescription);

                        createConfiguration(
                                configurations,
                                sourceSet.getProvidedConfigurationName(),//provided
                                "Classpath for only compiling the "
                                        + sourceSet.getName()
                                        + " sources.");

                        createConfiguration(
                                configurations,
                                sourceSet.getWearAppConfigurationName(),//wearApp
                                "Link to a wear app to embed for object '"
                                        + sourceSet.getName()
                                        + "'.");

                        createConfiguration(
                                configurations,
                                sourceSet.getAnnotationProcessorConfigurationName(),//annotationProcessor
                                "Classpath for the annotation processor for '"
                                        + sourceSet.getName()
                                        + "'.");

                        createConfiguration(
                                configurations,
                                sourceSet.getJackPluginConfigurationName(),//jackPlugin
                                String.format(
                                        "Classpath for the '%s' Jack plugins.",
                                        sourceSet.getName()));

                        sourceSet.setRoot(String.format("src/%s", sourceSet.getName()));
                    }
                });
    }
}
```


## 1.2创建task
```java
//BasePlugin.java
private void configureExtension() {
    ...
    project.afterEvaluate(
                project ->
                        threadRecorder.record(
                                ExecutionType.BASE_PLUGIN_CREATE_ANDROID_TASKS,
                                project.getPath(),
                                null,
                                () -> createAndroidTasks(false)));
}

@VisibleForTesting
final void createAndroidTasks(boolean force) {
    ...
    ndkHandler.setCompileSdkVersion(extension.getCompileSdkVersion());
    ...
    //设置android sdk的信息(compileSdkVersion BuildToolsRevision等)
    ensureTargetSetup();
    ...
    threadRecorder.record(
                ExecutionType.VARIANT_MANAGER_CREATE_ANDROID_TASKS,
                project.getPath(),
                null,
                () -> {
                    variantManager.createAndroidTasks();
                    ApiObjectFactory apiObjectFactory =
                            new ApiObjectFactory(
                                    androidBuilder, extension, variantFactory, instantiator);
                    for (BaseVariantData variantData : variantManager.getVariantDataList()) {
                        apiObjectFactory.create(variantData);
                    }
                });
}

/**
* Variant/Task creation entry point.
*
* Not used by gradle-experimental.
*/
public void createAndroidTasks() {
    final TaskFactory tasks = new TaskContainerAdaptor(project.getTasks());
    //初始化构建变体
    if (variantDataList.isEmpty()) {
        recorder.record(
                ExecutionType.VARIANT_MANAGER_CREATE_VARIANTS,
                project.getPath(),
                null /*variantName*/,
                this::populateVariantDataList);
    }
    ...
    for (final BaseVariantData<? extends BaseVariantOutputData> variantData : variantDataList) {
        recorder.record(
                ExecutionType.VARIANT_MANAGER_CREATE_TASKS_FOR_VARIANT,
                project.getPath(),
                variantData.getName(),
                () -> createTasksForVariantData(tasks, variantData));
    }
}

public void createTasksForVariantData(
    ...
    taskManager.createTasksForVariantData(tasks, variantData);
}

//com.android.build.gradle.internal.ApplicationTaskManager
@Override
    public void createTasksForVariantData(
        @NonNull final TaskFactory tasks,
        @NonNull final BaseVariantData<? extends BaseVariantOutputData> variantData) {
    assert variantData instanceof ApplicationVariantData;

    final VariantScope variantScope = variantData.getScope();
    ...
    //一下为创建各种task
    // Add a task to process the manifest(s)
    //merge aar中的manifest文件到主工程中
    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_MERGE_MANIFEST_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> createMergeAppManifestsTask(tasks, variantScope));

    // Add a task to create the res values
    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_GENERATE_RES_VALUES_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> createGenerateResValuesTask(tasks, variantScope));

    // Add a task to compile renderscript files.
    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_CREATE_RENDERSCRIPT_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> createRenderscriptTask(tasks, variantScope));

    // Add a task to merge the resource folders
    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_MERGE_RESOURCES_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            (Recorder.VoidBlock) () -> createMergeResourcesTask(tasks, variantScope));

    // Add a task to merge the asset folders
    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_MERGE_ASSETS_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> createMergeAssetsTask(tasks, variantScope));

    // Add a task to create the BuildConfig class
    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_BUILD_CONFIG_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> createBuildConfigTask(tasks, variantScope));

    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_PROCESS_RES_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> {
                // Add a task to process the Android Resources and generate source files
                //调用aapt编译资源，生成R.txt和R.java
                //根据aar中的R.txt遍历主工程的R.txt，将主工程中用到的资源保存下来（根据资源的类型和资源名进行匹配，匹配到了之后资源id使用主工程中的资源id），然后生成一个R.java文件
                createApkProcessResTask(tasks, variantScope);

                // Add a task to process the java resources
                createProcessJavaResTasks(tasks, variantScope);
            });

    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_AIDL_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> createAidlTask(tasks, variantScope));

    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_SHADER_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> createShaderTask(tasks, variantScope));

    // Add NDK tasks
    if (!isComponentModelPlugin) {
        recorder.record(
                ExecutionType.APP_TASK_MANAGER_CREATE_NDK_TASK,
                project.getPath(),
                variantScope.getFullVariantName(),
                () -> createNdkTasks(tasks, variantScope));
    } else {
        if (variantData.compileTask != null) {
            variantData.compileTask.dependsOn(getNdkBuildable(variantData));
        } else {
            variantScope.getCompileTask().dependsOn(tasks, getNdkBuildable(variantData));
        }
    }
    variantScope.setNdkBuildable(getNdkBuildable(variantData));

    // Add external native build tasks

    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_EXTERNAL_NATIVE_BUILD_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> {
                createExternalNativeBuildJsonGenerators(variantScope);
                createExternalNativeBuildTasks(tasks, variantScope);
            });

    // Add a task to merge the jni libs folders
    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_MERGE_JNILIBS_FOLDERS_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> createMergeJniLibFoldersTasks(tasks, variantScope));

    // Add a compile task
    //编译java文件
    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_COMPILE_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> {
                CoreJackOptions jackOptions =
                        variantData.getVariantConfiguration().getJackOptions();
                // create data binding merge task before the javac task so that it can
                // parse jars before any consumer
                createDataBindingMergeArtifactsTaskIfNecessary(tasks, variantScope);
                AndroidTask<? extends JavaCompile> javacTask =
                        createJavacTask(tasks, variantScope);
                if (jackOptions.isEnabled()) {
                    AndroidTask<TransformTask> jackTask =
                            createJackTask(tasks, variantScope, true /*compileJavaSource*/);
                    setJavaCompilerTask(jackTask, tasks, variantScope);
                } else {
                    // Prevent the use of java 1.8 without jack, which would otherwise cause an
                    // internal javac error.
                    if (variantScope
                            .getGlobalScope()
                            .getExtension()
                            .getCompileOptions()
                            .getTargetCompatibility()
                            .isJava8Compatible()) {
                        // Only warn for users of retrolambda and dexguard
                        if (project.getPlugins().hasPlugin("me.tatarka.retrolambda")
                                || project.getPlugins().hasPlugin("dexguard")) {
                            getLogger()
                                    .warn(
                                            "Jack is disabled, but one of the plugins you "
                                                    + "are using supports Java 8 language "
                                                    + "features.");
                        } else {
                            androidBuilder
                                    .getErrorReporter()
                                    .handleSyncError(
                                            variantScope
                                                    .getVariantConfiguration()
                                                    .getFullName(),
                                            SyncIssue
                                                    .TYPE_JACK_REQUIRED_FOR_JAVA_8_LANGUAGE_FEATURES,
                                            "Jack is required to support java 8 language "
                                                    + "features. Either enable Jack or remove "
                                                    + "sourceCompatibility "
                                                    + "JavaVersion.VERSION_1_8.");
                        }
                    }
                    addJavacClassesStream(variantScope);
                    setJavaCompilerTask(javacTask, tasks, variantScope);
                    getAndroidTasks()
                            .create(
                                    tasks,
                                    new AndroidJarTask.JarClassesConfigAction(variantScope));
                    createPostCompilationTasks(tasks, variantScope);
                }
            });

    // Add data binding tasks if enabled
    createDataBindingTasksIfNecessary(tasks, variantScope);

    createStripNativeLibraryTask(tasks, variantScope);

    if (variantData
            .getSplitHandlingPolicy()
            .equals(SplitHandlingPolicy.RELEASE_21_AND_AFTER_POLICY)) {
        if (getExtension().getBuildToolsRevision().getMajor() < 21) {
            throw new RuntimeException(
                    "Pure splits can only be used with buildtools 21 and later");
        }

        recorder.record(
                ExecutionType.APP_TASK_MANAGER_CREATE_SPLIT_TASK,
                project.getPath(),
                variantScope.getFullVariantName(),
                () -> createSplitTasks(tasks, variantScope));
    }

    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_PACKAGING_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> {
                @Nullable
                AndroidTask<BuildInfoWriterTask> fullBuildInfoGeneratorTask =
                        createInstantRunPackagingTasks(tasks, variantScope);
                createPackagingTask(
                        tasks, variantScope, true /*publishApk*/, fullBuildInfoGeneratorTask);
            });

    // create the lint tasks.
    recorder.record(
            ExecutionType.APP_TASK_MANAGER_CREATE_LINT_TASK,
            project.getPath(),
            variantScope.getFullVariantName(),
            () -> createLintTasks(tasks, variantScope));
}
```

