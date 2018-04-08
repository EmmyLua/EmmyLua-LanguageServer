package com.intellij.openapi.project;

public class ProjectCoreUtil {
    public static volatile Project theProject;

    public static Project theOnlyOpenProject() {
        return theProject;
    }
}
