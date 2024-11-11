package dev.sandipchitale.jbkubernetesdashboard;

import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Secret;

public record NamespaceSecretReleaseRevision(String namespace, V1Secret secret, String release, String revision) {}
