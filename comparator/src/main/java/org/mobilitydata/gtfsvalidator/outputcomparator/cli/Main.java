/*
 * Copyright 2020 MobilityData IO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mobilitydata.gtfsvalidator.outputcomparator.cli;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.flogger.FluentLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.mobilitydata.gtfsvalidator.outputcomparator.util.ValidationReport;

public class Main {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String INTEGRATION_REPORT_JSON = "integration_report.json";

  public static void main(String[] argv) throws IOException {
    Arguments args = new Arguments();
    new JCommander(args).parse(argv);
    File[] reportDirectory = new File(args.getReportDirectory()).listFiles();
    if (reportDirectory == null) {
      logger.atSevere().log("Specified output is not a directory, or an I/O error occurred");
      return;
    }
    if (reportDirectory.length == 0) {
      logger.atSevere().log(
          "Specified directory is empty, cannot generate integration tests report.");
      return;
    }
    ImmutableMap.Builder<String, Integer> mapBuilder = new Builder<>();
    int badDatasetCount = 0;
    List<File> reportDirs =
        Arrays.stream(reportDirectory).filter(File::isDirectory).collect(Collectors.toList());

    int totalDatasetCount = reportDirs.size();

    for (File file : reportDirs) {
      try {
        ValidationReport referenceReport =
            ValidationReport.fromPath(
                file.toPath().resolve(args.getReferenceValidationReportName()));
        ValidationReport latestReport =
            ValidationReport.fromPath(file.toPath().resolve(args.getLatestValidationReportName()));

        if (referenceReport.equals(latestReport)) {
          continue;
        }
        if (referenceReport.hasSameErrorCodes(latestReport)) {
          continue;
        }
        int newErrorCount = referenceReport.getNewErrorCount(latestReport);
        mapBuilder.put(file.getName(), newErrorCount);
        if (newErrorCount >= args.getThreshold()) {
          badDatasetCount += 1;
        }
      } catch (FileNotFoundException e) {
        logger.atSevere().withCause(e).log(String.format("No file found at %s.", file.getPath()));
        System.exit(1);
      }
    }
    exportIntegrationReport(mapBuilder.build(), args.getReportDirectory());
    System.out.printf(
        "%d out of %d datasets (~%.2f %%)  are invalid due to new implementation.%n",
        badDatasetCount,
        totalDatasetCount,
        100.0 * badDatasetCount / totalDatasetCount);
    isNewRuleValid(badDatasetCount, totalDatasetCount, args.getAcceptanceCriteria());
  }

  /**
   * Exports the integration test report (map of String, Object) as json.
   *
   * @param integrationReportData integration report content.
   * @param outputBase base path to output.
   * @throws IOException if an I/O error occurs writing to or creating the file.
   */
  private static void exportIntegrationReport(
      ImmutableMap<String, Integer> integrationReportData, String outputBase) throws IOException {
    Gson gson = new GsonBuilder().serializeNulls().create();
    Files.write(
        Paths.get(outputBase, INTEGRATION_REPORT_JSON),
        gson.toJson(integrationReportData).getBytes(StandardCharsets.UTF_8));
  }

  private static void isNewRuleValid(int badDatasetCount, int totalDatasetCount, double threshold){
    if (100.0 * badDatasetCount / totalDatasetCount >= threshold) {
      System.exit(2);
    }
  }
}
