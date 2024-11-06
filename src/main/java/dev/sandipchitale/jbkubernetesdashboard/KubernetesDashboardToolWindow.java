package dev.sandipchitale.jbkubernetesdashboard;

import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefCallback;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.security.CefSSLInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

public class KubernetesDashboardToolWindow {

    // Paths to various files
    private static final String INDEX = Path.of(
            Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.getId("dev.sandipchitale.jb-kubernetes-dashboard"))).getPluginPath().toString(),
            "kubernetes",
            "html",
            "index.html").toUri().toString();

    private static final String SERVICE_ACCOUNT_MANIFEST_PATH = Path.of(
            Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.getId("dev.sandipchitale.jb-kubernetes-dashboard"))).getPluginPath().toString(),
            "kubernetes",
            "kubectl",
            "kubernetes-dashboard-service-account.yml").toString();

    private static final String CLUSTER_ROLE_BINDING_MANIFEST_PATH = Path.of(
            Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.getId("dev.sandipchitale.jb-kubernetes-dashboard"))).getPluginPath().toString(),
            "kubernetes",
            "kubectl",
            "kubernetes-dashboard-cluster-role-binding.yml").toString();

    private static final String SECRET_MANIFEST_PATH = Path.of(
            Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.getId("dev.sandipchitale.jb-kubernetes-dashboard"))).getPluginPath().toString(),
            "kubernetes",
            "kubectl",
            "kubernetes-dashboard-secret.yml").toString();

    private static final String KUBERNETES_DASHBOARD_HELM_CHART_PATH = Path.of(
            Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.getId("dev.sandipchitale.jb-kubernetes-dashboard"))).getPluginPath().toString(),
            "kubernetes",
            "helm",
            "kubernetes-dashboard.tgz").toString();

    public static final String KUBERNETES_DASHBOARD = "kubernetes-dashboard";
    public static final String ADMIN_USER_SECRET = "admin-user-secret";
    public static final String KUBERNETES_DASHBOARD_URL_PREFIX = "https://127.0.0.1:8443";
    public static final String KUBERNETES_DASHBOARD_URL = KUBERNETES_DASHBOARD_URL_PREFIX + "/#/pod?namespace=" + KUBERNETES_DASHBOARD;

    private final JPanel contentToolWindow;

    private final JButton connectToClusterButton;

    private final JButton primeClusterButton;
    private final JButton deployKubernetesDashboardHelmChartButton;
    private final JButton portForwardButton;
    private final JButton loadKubernetesDashboardButton;
    private final JButton copyTokenToClipboardButton;
    private final JButton undeployKubernetesDashboardHelmChartButton;

    private final JButton disconnectFromCusterButton;

    private final JBCefBrowser browser;

    private KubernetesClient kubernetesClient = null;

    public KubernetesDashboardToolWindow(@NotNull Project project) {
        contentToolWindow = new SimpleToolWindowPanel(true, true);

        browser = new JBCefBrowser(INDEX);
        browser.getComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                e.consume();
            }
        });
        JBCefClient jbCefClient = browser.getJBCefClient();
        jbCefClient.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onCertificateError(CefBrowser browser, CefLoadHandler.ErrorCode cert_error, String request_url, CefSSLInfo sslInfo, CefCallback callback) {
                if (request_url.startsWith(KUBERNETES_DASHBOARD_URL_PREFIX)) {
                    // Ignore certificate error
                    callback.Continue();
                    return true;
                }
                return super.onCertificateError(browser, cert_error, request_url, sslInfo, callback);
            }
        }, browser.getCefBrowser());

        contentToolWindow.add(browser.getComponent(), BorderLayout.CENTER);

        JPanel toolbarsWrapper = new JPanel(new GridLayout(1, 1));

        JPanel topToolBarPanel = new JPanel(new BorderLayout(10, 10));

        JPanel topRightToolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        connectToClusterButton = new JButton(AllIcons.Actions.Execute);
        connectToClusterButton.setToolTipText("Connect to Cluster");
        connectToClusterButton.addActionListener(this::connectToCluster);
        topRightToolBar.add(connectToClusterButton);

        primeClusterButton = new JButton("1", AllIcons.Actions.Lightning);
        primeClusterButton.setToolTipText("Prime Cluster with required Service Account, CLuster Role Binding and Secret");
        primeClusterButton.addActionListener(this::primeCluster);
        topRightToolBar.add(primeClusterButton);

        deployKubernetesDashboardHelmChartButton = new JButton("2", AllIcons.Actions.Install);
        deployKubernetesDashboardHelmChartButton.setToolTipText("Deploy Kubernetes Dashboard Helm Chart");
        deployKubernetesDashboardHelmChartButton.addActionListener(this::deployKubernetesDashboardHelmChart);
        topRightToolBar.add(deployKubernetesDashboardHelmChartButton);

        portForwardButton = new JButton("3", AllIcons.Actions.SwapPanels);
        portForwardButton.setToolTipText("Port forwards 8443:8443");
        portForwardButton.addActionListener(this::portForward);
        topRightToolBar.add(portForwardButton);

        loadKubernetesDashboardButton = new JButton("4", AllIcons.Actions.OpenNewTab);
        loadKubernetesDashboardButton.setToolTipText("Open Kubernetes Dashboard at " + KUBERNETES_DASHBOARD_URL);
        loadKubernetesDashboardButton.addActionListener(this::loadKubernetesDashboard);
        topRightToolBar.add(loadKubernetesDashboardButton);

        copyTokenToClipboardButton = new JButton("4.1", AllIcons.Actions.Copy);
        copyTokenToClipboardButton.setToolTipText("Copy Token to Clipboard");
        copyTokenToClipboardButton.addActionListener(this::copyTokenToClipBoard);
        topRightToolBar.add(copyTokenToClipboardButton);

        undeployKubernetesDashboardHelmChartButton = new JButton("-2", AllIcons.Actions.Uninstall);
        undeployKubernetesDashboardHelmChartButton.setToolTipText("Undeploy Kubernetes Dashboard Helm Chart");
        undeployKubernetesDashboardHelmChartButton.addActionListener(this::undeployKubernetesDashboardHelmChart);
        topRightToolBar.add(undeployKubernetesDashboardHelmChartButton);

        disconnectFromCusterButton = new JButton(AllIcons.Actions.Exit);
        disconnectFromCusterButton.setToolTipText("Disconnect from Cluster");
        disconnectFromCusterButton.addActionListener(this::disconnectFromCluster);
        topRightToolBar.add(disconnectFromCusterButton);

        topToolBarPanel.add(topRightToolBar, BorderLayout.EAST);

        topToolBarPanel.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1));

        toolbarsWrapper.add(topToolBarPanel);

        contentToolWindow.add(toolbarsWrapper, BorderLayout.NORTH);

        adjustStates();
    }

    private void connectToCluster(ActionEvent actionEvent) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    kubernetesClient = new KubernetesClientBuilder().build();
                } finally {
                    adjustStates();
                }
            });
        });
    }

    private void disconnectFromCluster(ActionEvent actionEvent) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    if (kubernetesClient != null) {
                        kubernetesClient.close();
                    }
                } finally {
                    kubernetesClient = null;
                    adjustStates();
                    browser.loadURL(INDEX);
                }
            });
        });
    }

    private void adjustStates() {
        connectToClusterButton.setVisible(kubernetesClient == null);
        primeClusterButton.setVisible(kubernetesClient != null);
        deployKubernetesDashboardHelmChartButton.setVisible(kubernetesClient != null);
        portForwardButton.setVisible(kubernetesClient != null);
        loadKubernetesDashboardButton.setVisible(kubernetesClient != null);
        copyTokenToClipboardButton.setVisible(kubernetesClient != null);
        undeployKubernetesDashboardHelmChartButton.setVisible(kubernetesClient != null);
        disconnectFromCusterButton.setVisible(kubernetesClient != null);
    }

    public @Nullable JComponent getContent() {
        return contentToolWindow;
    }

    private boolean isConnected() {
        return kubernetesClient != null;
    }

    private void primeCluster(ActionEvent actionEvent) {
        if (isConnected()) {
            // Create service account
            kubernetesClient.serviceAccounts().load(SERVICE_ACCOUNT_MANIFEST_PATH).get();
            // Create cluster role binding
            kubernetesClient.rbac().clusterRoleBindings().load(CLUSTER_ROLE_BINDING_MANIFEST_PATH).get();
            // Create secret
            kubernetesClient.secrets().load(SECRET_MANIFEST_PATH).get();
        } else {
            Messages.showErrorDialog("Not connected to the cluster! Connect first.", "Not Connected");
        }
    }

    private void deployKubernetesDashboardHelmChart(ActionEvent actionEvent) {
        if (isConnected()) {
            CommandLauncher.launch("helm install -n " + KUBERNETES_DASHBOARD + " " + KUBERNETES_DASHBOARD + " " + KUBERNETES_DASHBOARD_HELM_CHART_PATH);
        } else {
            Messages.showErrorDialog("Not connected to the cluster! Connect first.", "Not Connected");
        }
    }

    private void portForward(ActionEvent actionEvent) {
        if (isConnected()) {
            kubernetesClient.pods().inNamespace(KUBERNETES_DASHBOARD).list().getItems().forEach((Pod pod) -> {
                if (KUBERNETES_DASHBOARD.equals(pod.getMetadata().getLabels().get("app.kubernetes.io/name"))) {
                    CommandLauncher.launch("kubectl port-forward -n " + KUBERNETES_DASHBOARD + " " + pod.getMetadata().getName() + " 8443:8443");
                }
            });
        } else {
            Messages.showErrorDialog("Not connected to the cluster! Connect first.", "Not Connected");
        }
    }

    private void loadKubernetesDashboard(ActionEvent actionEvent) {
        if (isConnected()) {
            browser.loadURL(KUBERNETES_DASHBOARD_URL);
            copyTokenToClipBoard(actionEvent);
            Messages.showInfoMessage("Token copied to clipboard. Paste it in the login screen.", "Token Copied");
        } else {
            Messages.showErrorDialog("Not connected to the cluster! Connect first.", "Not Connected");
        }
    }

    private void copyTokenToClipBoard(ActionEvent actionEvent) {
        if (isConnected()) {
            Secret secret = kubernetesClient.secrets().inNamespace(KUBERNETES_DASHBOARD).withName(ADMIN_USER_SECRET).get();
            if (secret == null) {
                Messages.showErrorDialog("Cannot get token as the secret: " + ADMIN_USER_SECRET + " in namespace " + KUBERNETES_DASHBOARD + " is missing.",
                        "Token Unavailable");
            } else {
                String token = new String(Base64.getDecoder().decode(secret.getData().get("token")), StandardCharsets.UTF_8);
                CopyPasteManager.getInstance().setContents(new StringSelection(token));
            }
        } else {
            Messages.showErrorDialog("Not connected to the cluster! Connect first.", "Not Connected");
        }
    }

    private void undeployKubernetesDashboardHelmChart(ActionEvent actionEvent) {
        if (isConnected()) {
            CommandLauncher.launch("helm uninstall -n " + KUBERNETES_DASHBOARD + " " + KUBERNETES_DASHBOARD);
            browser.loadURL(INDEX);
        } else {
            Messages.showErrorDialog("Not connected to the cluster! Connect first.", "Not Connected");
        }
    }
}
