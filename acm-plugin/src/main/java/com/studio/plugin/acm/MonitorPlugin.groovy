package com.studio.plugin.acm

import com.android.build.gradle.BaseExtension
import com.studio.plugin.acm.profiler.AopEngine
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.tasks.compile.JavaCompile

class MonitorPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("studio_acm", MonitorExtension)
        GLog("******************MonitorPlugin(project=$project.name)******************")
        DefaultDomainObjectSet<ApplicationVariant> variants = getAndroidVariants(project)
        if (variants != null) {
            project.afterEvaluate {
                MonitorExtension monitorConfig = MonitorExtension.getConfig(project)
                GLog("******************MonitorPlugin(config=$monitorConfig.enable)******************")
                if (monitorConfig.enable) {
                    doJavaCompile(variants, monitorConfig)
                }
            }
        }

        //project.getExtensions().findByType(BaseExtension.class)
                //.registerTransform(new MyCustomTransform())
    }

    private DefaultDomainObjectSet<ApplicationVariant> getAndroidVariants(Project project) {
        DefaultDomainObjectSet<ApplicationVariant> variants = null
        if (project.plugins.withType(AppPlugin)) {
            variants = project.android.applicationVariants
        } else if (project.plugins.withType(LibraryPlugin)) {
            variants = project.android.libraryVariants
        }
        return variants
    }

    private void doJavaCompile(DefaultDomainObjectSet<ApplicationVariant> variants, MonitorExtension config) {
        variants.all { variant ->
            JavaCompile javaCompile = variant.javaCompile
            javaCompile.doLast {
                GLog("******************MonitorPlugin -> JavaCompile($variant.dirName)******************")
                File classDir = javaCompile.destinationDir
                AopEngine.doAspect(classDir, config.includePackage, config.excludePackage, config.className, config.executeTimeout)
                /*classDir.eachFileRecurse { file ->
                    def classPath = file.absolutePath.replace("\\", "/")
                    if (isBasicFilters(classPath)
                        if (MonitorExtension.isIncluded(classPath, config.includePackage)) {
                            if (MonitorExtension.isExcluded(classPath, config.excludePackage)) {
                                GLog("class[N]: $classPath")
                            } else {
                                GLog("class[Y]: $classPath")
                                AopEngine.doAspect(file, config.includePackage)
                            }
                        }
                    }
                }*/
            }
        }
    }

    static void GLog(String message) {
        println "[MonitorPlugin] " + message
    }
}