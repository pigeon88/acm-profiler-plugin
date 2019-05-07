package com.studio.plugin.acm;

import org.gradle.api.tasks.Input;

import java.util.HashSet;

import groovy.transform.CompileStatic;

@CompileStatic
public class AcmExtension {

    @Input
    boolean enable = true;

    @Input
    int executeTimeout;

    @Input
    String includePackage;

    @Input
    HashSet<String> excludePackage = new HashSet<>();
}
