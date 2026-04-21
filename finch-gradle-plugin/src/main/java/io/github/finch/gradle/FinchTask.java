package io.github.finch.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class FinchTask extends DefaultTask {

    @TaskAction
    public void run() {
        getLogger().lifecycle("Finch Gradle Plugin running");
    }
}
