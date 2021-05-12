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

package org.mobilitydata.gtfsvalidator.outputcomparator.util;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.mobilitydata.gtfsvalidator.notice.SeverityLevel;

/**
 * Used to deserialize a validation report. It is used to store information about one type of notice
 * encountered in a validation report: error code, severity level, the total number of notices
 * related to the error code and a list of notice contexts (which provides additional information
 * about each notice.
 */
public class NoticeAggregate {
  private final String code;
  private final SeverityLevel severity;
  private final int totalNotices;

  @SerializedName("notices")
  private final List<Map<String, Object>> contexts;

  public NoticeAggregate(
      String code, SeverityLevel severity, int totalNotices, List<Map<String, Object>> contexts) {
    this.code = code;
    this.severity = severity;
    this.totalNotices = totalNotices;
    this.contexts = contexts;
  }

  public int getTotalNotices() {
    return totalNotices;
  }

  public SeverityLevel getSeverity() {
    return severity;
  }

  public String getCode() {
    return code;
  }

  public List<Map<String, Object>> getContexts() {
    return Collections.unmodifiableList(contexts);
  }

  public boolean isError() {
    return getSeverity().ordinal() >= SeverityLevel.ERROR.ordinal();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof NoticeAggregate) {
      return this.getCode().equals(((NoticeAggregate) other).getCode())
          && this.getSeverity().equals(((NoticeAggregate) other).getSeverity())
          && this.getTotalNotices() == (((NoticeAggregate) other).getTotalNotices())
          && getContexts().equals(((NoticeAggregate) other).getContexts());
    }
    return false;
  }
}
