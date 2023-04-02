package org.globex.retail.kubernetes;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@ApplicationScoped
public class KubernetesRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesRunner.class);

    @Inject
    KubernetesClient client;

    public int run() {

        String statefulSetName = System.getenv("STATEFULSET");
        if (statefulSetName == null || statefulSetName.isBlank()) {
            LOGGER.error("Environment variable 'STATEFULSET' for deployment not set. Exiting...");
            return -1;
        }

        String namespace = System.getenv("NAMESPACE");
        if (namespace == null || namespace.isBlank()) {
            LOGGER.error("Environment variable 'NAMESPACE' for namespace not set. Exiting...");
            return -1;
        }

        String maxTimeToWaitStr = System.getenv().getOrDefault("MAX_TIME_TO_WAIT_MS", "60000");

        long maxTimeToWait = Long.parseLong(maxTimeToWaitStr);

        Resource<StatefulSet> statefulset = client.apps().statefulSets().inNamespace(namespace).withName(statefulSetName);

        try {
            statefulset.waitUntilCondition(ss -> ss != null && Objects.equals(ss.getStatus().getAvailableReplicas(), ss.getStatus().getReadyReplicas()),
                    maxTimeToWait, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.error("StatefulSet " + statefulSetName + " is not ready after " + maxTimeToWaitStr + " milliseconds. Exiting...");
            return -1;
        }

        if (statefulset.get() == null) {
            LOGGER.error("Statefulset " + statefulSetName + " is not ready after " + maxTimeToWaitStr + " milliseconds. Exiting...");
            return -1;
        } else {
            LOGGER.error("Statefulset " + statefulSetName + " is ready. Exiting...");
            return 0;
        }
    }

}
