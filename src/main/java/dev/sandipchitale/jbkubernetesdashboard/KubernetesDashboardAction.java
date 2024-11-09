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

import java.lang.reflect.Field;
import java.util.Objects;

public class KubernetesDashboardAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        Project project = actionEvent.getProject();
        ToolWindow toolWindow = ToolWindowManager.getInstance(Objects.requireNonNull(project)).getToolWindow("Kubernetes Dashboard");
        KubernetesObject kubernetesObject = getKubernetesObject(actionEvent.getDataContext().getData(PlatformCoreDataKeys.SELECTED_ITEM));
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
                        if (namespace == null) {
                            url = KubernetesDashboardToolWindow.KUBERNETES_DASHBOARD_URL_PREFIX + String.format("/#/%s/%s",
                                    kubernetesObject.getKind().toLowerCase(),
                                    kubernetesObject.getMetadata().getName());
                        } else {
                            url = KubernetesDashboardToolWindow.KUBERNETES_DASHBOARD_URL_PREFIX + String.format("/#/%s/%s/%s?namespace=%s",
                                    kubernetesObject.getKind().toLowerCase(),
                                    namespace,
                                    kubernetesObject.getMetadata().getName(),
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
        actionEvent.getPresentation().setVisible(getKubernetesObject(actionEvent.getDataContext().getData(PlatformCoreDataKeys.SELECTED_ITEM)) != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private static KubernetesObject getKubernetesObject(Object selectedItem) {
        if (selectedItem != null) {
            Class<?> clazz = selectedItem.getClass();
            while ((clazz != null) && !clazz.getName().equals(Object.class.getName())) {
                try {
                    Field resourceField = clazz.getDeclaredField("resource");
                    // Yay!
                    resourceField.setAccessible(true);
                    return (KubernetesObject) resourceField.get(selectedItem);
                } catch (NoSuchFieldException | IllegalAccessException ignore) {
                }
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
