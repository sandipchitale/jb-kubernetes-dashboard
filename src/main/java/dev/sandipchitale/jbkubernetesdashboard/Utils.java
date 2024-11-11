package dev.sandipchitale.jbkubernetesdashboard;

import io.kubernetes.client.common.KubernetesObject;

import java.lang.reflect.Field;

public class Utils {
    static KubernetesObject getKubernetesObject(Object selectedItem) {
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
