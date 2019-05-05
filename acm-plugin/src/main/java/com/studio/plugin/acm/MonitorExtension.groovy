package com.studio.plugin.acm

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.Input

@CompileStatic
class MonitorExtension {

    @Input
    boolean enable = true

    @Input
    int executeTimeout

    @Input
    String includePackage

    @Input
    HashSet<String> excludePackage = []

    @Input
    String className

    static MonitorExtension getConfig(Project project) {
        MonitorExtension config = project.getExtensions().findByType(MonitorExtension.class)
        if (config == null) {
            config = new MonitorExtension()
        }
        return config
    }
}
