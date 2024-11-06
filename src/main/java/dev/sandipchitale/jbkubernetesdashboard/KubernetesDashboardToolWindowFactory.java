package dev.sandipchitale.jbkubernetesdashboard;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class KubernetesDashboardToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        KubernetesDashboardToolWindow kubernetesDashboardToolWindow = new KubernetesDashboardToolWindow(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(kubernetesDashboardToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

}
