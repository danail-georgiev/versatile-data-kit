/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.vmware.taurus.datajobs.it;

import com.vmware.taurus.ControlplaneApplication;
import com.vmware.taurus.datajobs.it.common.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static com.vmware.taurus.datajobs.it.common.WebHookServerMockExtension.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = ControlplaneApplication.class)
public class DataJobListManyJobsIT extends BaseIT {

  private static final String DEFAULT_QUERY_WITH_VARS =
      "query($filter: [Predicate], $search: String, $pageNumber: Int, $pageSize: Int) { "
          + " jobs(pageNumber: $pageNumber, pageSize: $pageSize, filter: $filter, search: $search)"
          + " {    content {      jobName      config {        team        description       "
          + " schedule {          scheduleCron        }      }    }    totalPages    totalItems  }"
          + "}";

  private static final int JOB_COUNT = 50;

  @Test
  public void testListManyJobs() throws Exception {
    createDummyJobs();

    mockMvc
            .perform(
                    get(String.format("/data-jobs/for-team/%s/jobs", TEST_TEAM_NAME))
                            .with(user("user"))
                            .param("query", DEFAULT_QUERY_WITH_VARS)
                            .param(
                                    "variables",
                                    "{"
                                            + "\"filter\": ["
                                            + "    {"
                                            + "      \"property\": \"config.team\","
                                            + "      \"pattern\": \""
                                            + TEST_TEAM_NAME
                                            + "\""
                                            + "    }"
                                            + "  ],"
                                            + "\"pageNumber\": 1,"
                                            + "\"pageSize\": 10"
                                            + "}")
                            .contentType(MediaType.APPLICATION_JSON));
            //.andExpect(status().isNotFound());
//            .andExpect(
//                    content()
//                            .string(
//                                    lambdaMatcher(
//                                            s ->
//                                                    (s.contains(TEST_JOB_1))
//                                                            && (s.contains(TEST_JOB_2))
//                                                            && (s.contains(TEST_JOB_6))
//                                                            && (!s.contains(TEST_JOB_3))
//                                                            && (!s.contains(TEST_JOB_4))
//                                                            && (!s.contains(TEST_JOB_5)))));

    deleteDummyJobs();
  }

  private void createDummyJobs() throws Exception {
    for (int i = 0; i < JOB_COUNT; i++) {
      String dataJobTestBody = getDataJobRequestBody(TEST_TEAM_NAME, "test-job-" + (i + 1));
      createJob(dataJobTestBody, TEST_TEAM_NAME);
    }
  }

  private void deleteDummyJobs() throws Exception {
    // Clean up
    for (int i = 0; i < JOB_COUNT; i++) {
      deleteJob("test-job-" + (i + 1), TEST_TEAM_NAME);
    }
  }

  private void createJob(String body, String teamName) throws Exception {
    mockMvc
        .perform(
            post(String.format("/data-jobs/for-team/%s/jobs", teamName))
                .with(user("user"))
                .content(body)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());
  }

  private void deleteJob(String jobName, String teamName) throws Exception {
    mockMvc
        .perform(
            delete(String.format("/data-jobs/for-team/%s/jobs/%s", teamName, jobName))
                .with(user("user"))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  private boolean checkContentContainsJobNames(String content) {
    for (int i = 0; i < JOB_COUNT; i++) {
      if (!content.contains("test-job-" + (i + 1))) {
        return false;
      }
    }
    return true;
  }
}