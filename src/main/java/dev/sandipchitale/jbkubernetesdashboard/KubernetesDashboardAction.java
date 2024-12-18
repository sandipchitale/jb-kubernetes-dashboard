package dev.sandipchitale.jbkubernetesdashboard;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.jcef.JBCefBrowser;
import io.kubernetes.client.common.KubernetesObject;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class KubernetesDashboardAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        Project project = actionEvent.getProject();
        ToolWindow toolWindow = ToolWindowManager.getInstance(Objects.requireNonNull(project)).getToolWindow("Kubernetes Dashboard");
        KubernetesObject kubernetesObject = Utils.getKubernetesObject(actionEvent.getDataContext().getData(PlatformCoreDataKeys.SELECTED_ITEM));
        if (kubernetesObject != null) {
            String namespace = kubernetesObject.getMetadata().getNamespace();
            if (toolWindow == null) {
                if (namespace == null) {
                    Messages.showInfoMessage(String.format("%s : %s ", kubernetesObject.getMetadata().getName(), kubernetesObject.getKind()), "Resource");
                } else {
                    Messages.showInfoMessage(String.format("%s/%s: %s", namespace, kubernetesObject.getMetadata().getName(), kubernetesObject.getKind()), "Resource");
                }
            } else {
                JBCefBrowser browser = (JBCefBrowser) Objects.requireNonNull(toolWindow.getContentManager().getContent(0)).getComponent().getClientProperty("browser");
                if (browser != null) {
                    String url = browser.getCefBrowser().getURL();
                    if (KubernetesDashboardToolWindow.INDEX.equals(url)) {
                        // Well dashboard not loaded
                        Notification notification = new Notification("kubernetesDashboardNotificationGroup",
                                "Load kubernetes dashboard",
                                "Load kubernetes dashboard first.",
                                NotificationType.ERROR);
                        notification.notify(project);
                    } else {
                        String kind = kubernetesObject.getKind().toLowerCase();
                        String name = kubernetesObject.getMetadata().getName();
                        if (namespace == null) {
                            url = KubernetesDashboardToolWindow.KUBERNETES_DASHBOARD_URL_PREFIX + String.format("/#/%s/%s?namespace=_all",
                                    kind,
                                    name);
                        } else {
                            url = KubernetesDashboardToolWindow.KUBERNETES_DASHBOARD_URL_PREFIX + String.format("/#/%s/%s/%s?namespace=%s",
                                    kind,
                                    namespace,
                                    name,
                                    namespace);
                        }
                        browser.loadURL(url);
                    }
                }
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent actionEvent) {
        actionEvent.getPresentation().setVisible(Utils.getKubernetesObject(actionEvent.getDataContext().getData(PlatformCoreDataKeys.SELECTED_ITEM)) != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
