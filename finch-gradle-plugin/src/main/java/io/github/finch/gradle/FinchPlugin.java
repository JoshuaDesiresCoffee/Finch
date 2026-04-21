package io.github.finch.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class FinchPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        FinchExtension extension = project.getExtensions()
                .create("finch", FinchExtension.class);

        project.getTasks().register("finch", FinchTask.class, task -> {
            task.setGroup("finch");
            task.setDescription("Runs the Finch plugin.");
        });
    }
}
