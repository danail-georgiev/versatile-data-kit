/*
 * Copyright 2021-2023 VMware, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.vmware.taurus.service;

import com.google.common.annotations.VisibleForTesting;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static com.vmware.taurus.service.KubernetesService.V1_K8S_DATA_JOB_TEMPLATE_RESOURCE;

@ConditionalOnProperty("datajobs.control.k8s.k8sSupportsV1CronJob")
@Component
@Slf4j
public class V1CronJobDeployer implements CronJobDeployer {

  private final String datajobTemplateFileLocation;
  private final BatchV1Api batchV1Api;
  private final String namespace;

  public V1CronJobDeployer(
      @org.springframework.beans.factory.annotation.Value(
              "${datajobs.control.k8s.data.job.template.file}")
          String datajobTemplateFileLocation,
      @Qualifier("deploymentBatchV1Api") BatchV1Api batchV1Api,
      @Value("${datajobs.deployment.k8s.namespace:}") String namespace) {
    this.datajobTemplateFileLocation = datajobTemplateFileLocation;
    this.batchV1Api = batchV1Api;
    this.namespace = namespace;
    // Run it on startup to assert the template is valid
    loadV1CronjobTemplate();
  }

  // TODO:  container/volume args are breaking a bit abstraction of KubernetesService by leaking
  // impl. details
  @Override
  public void create(
      String name,
      String image,
      String schedule,
      boolean enable,
      V1Container jobContainer,
      V1Container initContainer,
      List<V1Volume> volumes,
      Map<String, String> jobAnnotations,
      Map<String, String> jobLabels,
      List<String> imagePullSecrets)
      throws ApiException {
    log.debug("Creating k8s V1 cron job name:{}, image:{}", name, image);
    var cronJob =
        v1CronJobFromTemplate(
            name,
            schedule,
            !enable,
            jobContainer,
            initContainer,
            volumes,
            jobAnnotations,
            jobLabels,
            imagePullSecrets);
    V1CronJob nsJob =
        batchV1Api.createNamespacedCronJob(namespace, cronJob, null, null, null, null);
    log.debug("Created k8s V1 cron job: {}", nsJob);
    log.debug(
        "Created k8s cron job name: {}, api_version: {}, uid:{}, link:{}",
        nsJob.getMetadata().getName(),
        nsJob.getApiVersion(),
        nsJob.getMetadata().getUid(),
        nsJob.getMetadata().getSelfLink());
  }

  @Override
  public void update(
      String name,
      String image,
      String schedule,
      boolean enable,
      V1Container jobContainer,
      V1Container initContainer,
      List<V1Volume> volumes,
      Map<String, String> jobAnnotations,
      Map<String, String> jobLabels,
      List<String> imagePullSecrets)
      throws ApiException {
    var cronJob =
        v1CronJobFromTemplate(
            name,
            schedule,
            !enable,
            jobContainer,
            initContainer,
            volumes,
            jobAnnotations,
            jobLabels,
            imagePullSecrets);
    V1CronJob nsJob =
        batchV1Api.replaceNamespacedCronJob(name, namespace, cronJob, null, null, null, null);
    log.debug(
        "Updated k8s V1 cron job status for name:{}, image:{}, uid:{}, link:{}",
        name,
        image,
        nsJob.getMetadata().getUid(),
        nsJob.getMetadata().getSelfLink());
  }

  @VisibleForTesting
  V1CronJob v1CronJobFromTemplate(
      String name,
      String schedule,
      boolean suspend,
      V1Container jobContainer,
      V1Container initContainer,
      List<V1Volume> volumes,
      Map<String, String> jobAnnotations,
      Map<String, String> jobLabels,
      List<String> imagePullSecrets) {
    V1CronJob cronjob = loadV1CronjobTemplate();
    v1checkForMissingEntries(cronjob);
    cronjob.getMetadata().setName(name);
    cronjob.getSpec().setSchedule(schedule);
    cronjob.getSpec().setSuspend(suspend);
    cronjob
        .getSpec()
        .getJobTemplate()
        .getSpec()
        .getTemplate()
        .getSpec()
        .setContainers(Collections.singletonList(jobContainer));
    cronjob
        .getSpec()
        .getJobTemplate()
        .getSpec()
        .getTemplate()
        .getSpec()
        .setInitContainers(Collections.singletonList(initContainer));
    cronjob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().setVolumes(volumes);
    // Merge the annotations and the labels.

    cronjob.getSpec().getJobTemplate().getMetadata().getAnnotations().putAll(jobAnnotations);
    cronjob.getSpec().getJobTemplate().getMetadata().getLabels().putAll(jobLabels);

    List<V1LocalObjectReference> imagePullSecretsObj =
        Optional.ofNullable(imagePullSecrets).stream()
            .flatMap(secrets -> secrets.stream())
            .filter(secret -> StringUtils.isNotEmpty(secret))
            .map(secret -> new V1LocalObjectReferenceBuilder().withName(secret).build())
            .collect(Collectors.toList());

    if (!CollectionUtils.isEmpty(imagePullSecretsObj)) {
      cronjob
          .getSpec()
          .getJobTemplate()
          .getSpec()
          .getTemplate()
          .getSpec()
          .setImagePullSecrets(imagePullSecretsObj);
    }

    return cronjob;
  }

  private V1CronJob loadV1CronjobTemplate(File datajobTemplateFile) throws Exception {
    String cronjobTemplateString = Files.readString(datajobTemplateFile.toPath());
    // Check whether the string template is a valid datajob template.
    V1CronJob cronjobTemplate = Yaml.loadAs(cronjobTemplateString, V1CronJob.class);
    log.debug(
        "Datajob template for file '{}': \n{}",
        datajobTemplateFile.getCanonicalPath(),
        cronjobTemplate);

    return cronjobTemplate;
  }

  public V1CronJob loadV1CronjobTemplate() {
    if (StringUtils.isEmpty(datajobTemplateFileLocation)) {
      log.debug("Datajob template file location is not set. Using internal datajob template.");
      return loadInternalV1CronjobTemplate();
    }
    V1CronJob cronjobTemplate = loadConfigurableV1CronjobTemplate();
    if (cronjobTemplate == null) {
      return loadInternalV1CronjobTemplate();
    }
    v1checkForMissingEntries(cronjobTemplate);

    return cronjobTemplate;
  }

  private V1CronJob loadInternalV1CronjobTemplate() {
    try {
      return loadV1CronjobTemplate(
          new ClassPathResource(V1_K8S_DATA_JOB_TEMPLATE_RESOURCE).getFile());
    } catch (Exception e) {
      // This should never happen unless we are testing locally and we've messed up
      // with the internal template resource file.
      throw new RuntimeException(
          "Unrecoverable error while loading the internal datajob template. Cannot continue.", e);
    }
  }

  private V1CronJob loadConfigurableV1CronjobTemplate() {
    // Check whether to use configurable datajob template at all.
    if (StringUtils.isEmpty(datajobTemplateFileLocation)) {
      log.debug("Datajob template file location is not set.");
      return null;
    }

    try {
      // Load the configurable datajob template file.
      return loadV1CronjobTemplate(new ClassPathResource(datajobTemplateFileLocation).getFile());
    } catch (Exception e) {
      log.error("Error while loading the datajob template file.", e);
      return null;
    }
  }

  private void v1checkForMissingEntries(V1CronJob cronjob) {
    V1CronJob internalCronjobTemplate = loadInternalV1CronjobTemplate();
    if (cronjob.getMetadata() == null) {
      cronjob.setMetadata(internalCronjobTemplate.getMetadata());
    }
    V1ObjectMeta metadata = cronjob.getMetadata();
    if (metadata.getAnnotations() == null) {
      metadata.setAnnotations(new HashMap<>());
    }
    if (cronjob.getSpec() == null) {
      cronjob.setSpec(internalCronjobTemplate.getSpec());
    }
    V1CronJobSpec spec = cronjob.getSpec();
    if (spec.getJobTemplate() == null) {
      spec.setJobTemplate(internalCronjobTemplate.getSpec().getJobTemplate());
    }
    if (spec.getJobTemplate().getMetadata() == null) {
      spec.getJobTemplate()
          .setMetadata(internalCronjobTemplate.getSpec().getJobTemplate().getMetadata());
    }
    if (spec.getJobTemplate().getMetadata().getLabels() == null) {
      spec.getJobTemplate().getMetadata().setLabels(new HashMap<>());
    }
    if (spec.getJobTemplate().getMetadata().getAnnotations() == null) {
      spec.getJobTemplate().getMetadata().setAnnotations(new HashMap<>());
    }
    if (spec.getJobTemplate().getSpec() == null) {
      spec.getJobTemplate().setSpec(internalCronjobTemplate.getSpec().getJobTemplate().getSpec());
    }
    if (spec.getJobTemplate().getSpec().getTemplate() == null) {
      spec.getJobTemplate()
          .getSpec()
          .setTemplate(internalCronjobTemplate.getSpec().getJobTemplate().getSpec().getTemplate());
    }
    if (spec.getJobTemplate().getSpec().getTemplate().getSpec() == null) {
      spec.getJobTemplate()
          .getSpec()
          .getTemplate()
          .setSpec(
              internalCronjobTemplate.getSpec().getJobTemplate().getSpec().getTemplate().getSpec());
    }
    if (spec.getJobTemplate().getSpec().getTemplate().getMetadata() == null) {
      spec.getJobTemplate()
          .getSpec()
          .getTemplate()
          .setMetadata(
              internalCronjobTemplate
                  .getSpec()
                  .getJobTemplate()
                  .getSpec()
                  .getTemplate()
                  .getMetadata());
    }
    if (spec.getJobTemplate().getSpec().getTemplate().getMetadata().getLabels() == null) {
      spec.getJobTemplate().getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
    }
  }
}
