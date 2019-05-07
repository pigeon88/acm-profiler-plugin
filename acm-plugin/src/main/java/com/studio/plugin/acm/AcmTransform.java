package com.studio.plugin.acm;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.studio.plugin.acm.profiler.IWeaver;
import com.studio.plugin.acm.profiler.asm.ASMWeaver;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class AcmTransform extends Transform {

    protected Project project;
    protected Logger logger;
    protected IWeaver bytecodeWeaver;

    public AcmTransform(Project project, AcmExtension acmExtension) {
        this.project = project;
        this.logger = project.getLogger();
        this.bytecodeWeaver = new ASMWeaver(acmExtension.executeTimeout);
    }

    @Override
    public String getName() {
        return "studioAcm";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        //return TransformManager.SCOPE_FULL_PROJECT;
        return Sets.immutableEnumSet(
                QualifiedContent.Scope.PROJECT,
                //QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
                QualifiedContent.Scope.SUB_PROJECTS
                //QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS
                //QualifiedContent.Scope.EXTERNAL_LIBRARIES
        );
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        System.out.println(getName() + " isIncremental = " + transformInvocation.isIncremental());

        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        boolean isIncremental = transformInvocation.isIncremental();
        //如果非增量，则清空旧的输出内容
        if (!isIncremental) {
            outputProvider.deleteAll();
        }

        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                Status status = jarInput.getStatus();
                File dest = outputProvider.getContentLocation(
                        jarInput.getName(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR);
                Files.createParentDirs(dest);
                if (isIncremental) {
                    switch (status) {
                        case NOTCHANGED:
                            break;
                        case ADDED:
                        case CHANGED:
                            transformJar(jarInput.getFile(), dest, status);
                            break;
                        case REMOVED:
                            if (dest.exists()) {
                                FileUtils.forceDelete(dest);
                            }
                            break;
                    }
                } else {
                    transformJar(jarInput.getFile(), dest, status);
                }
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
                FileUtils.forceMkdir(dest);
                if (isIncremental) {
                    String srcDirPath = directoryInput.getFile().getAbsolutePath();
                    String destDirPath = dest.getAbsolutePath();
                    Map<File, Status> fileStatusMap = directoryInput.getChangedFiles();
                    for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                        Status status = changedFile.getValue();
                        File inputFile = changedFile.getKey();
                        String destFilePath = inputFile.getAbsolutePath().replace(srcDirPath, destDirPath);
                        File destFile = new File(destFilePath);
                        switch (status) {
                            case NOTCHANGED:
                                break;
                            case REMOVED:
                                if (destFile.exists()) {
                                    //FileUtils.forceDelete(destFile);
                                    destFile.delete();
                                }
                                break;
                            case ADDED:
                            case CHANGED:
                                try {
                                    FileUtils.touch(destFile);
                                } catch (IOException e) {
                                    Files.createParentDirs(destFile);
                                }
                                transformSingleFile(inputFile, destFile, srcDirPath);
                                break;
                        }
                    }
                } else {
                    transformDir(directoryInput.getFile(), dest);
                }
            }
        }
    }

    private void transformJar(final File srcJar, final File destJar, Status status) throws IOException {
        bytecodeWeaver.weaveJar(srcJar, destJar);
    }

    private void transformDir(final File inputDir, final File outputDir) throws IOException {
        final String inputDirPath = inputDir.getAbsolutePath();
        final String outputDirPath = outputDir.getAbsolutePath();
        if (inputDir.isDirectory()) {
            for (final File file : com.android.utils.FileUtils.getAllFiles(inputDir)) {
                String filePath = file.getAbsolutePath();
                File outputFile = new File(filePath.replace(inputDirPath, outputDirPath));
                //bytecodeWeaver.weaveSingleClassToFile(file, outputFile, inputDirPath);
                transformSingleFile(file, outputFile, inputDirPath);
            }
        }
    }

    private void transformSingleFile(final File inputFile, final File outputFile, final String srcBaseDir) throws IOException {
        bytecodeWeaver.weaveSingleClassToFile(inputFile, outputFile, srcBaseDir);
    }
}
