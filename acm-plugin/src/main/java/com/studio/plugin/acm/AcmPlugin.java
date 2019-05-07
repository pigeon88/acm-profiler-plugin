package com.studio.plugin.acm;

import com.android.build.gradle.BaseExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AcmPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("studio_acm", AcmExtension.class);
        AcmExtension acmExtension = project.getExtensions().findByType(AcmExtension.class);

        System.out.println("******************AcmPlugin: " + project.getName() + " -> " + acmExtension.enable + "******************");
        if (acmExtension.enable) {
            project.getExtensions()
                    .findByType(BaseExtension.class)
                    .registerTransform(new AcmTransform(project, acmExtension));


            //AppExtension appExtension = (AppExtension) project.getProperties().get("android");
            //appExtension.registerTransform(new AcmTransform());
        }
    }
}
