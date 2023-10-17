/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.crashlytics.internal

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.google.firebase.crashlytics.internal.model.CrashlyticsReport.Session.Application
import com.google.firebase.crashlytics.internal.model.ImmutableList

/**
 * Collect process details.
 *
 * @hide
 */
internal object ProcessDetails {
  /** Gets the details of all running app processes. */
  fun getAppProcesses(context: Context): ImmutableList<Application.Process> {
    val defaultProcessName = context.applicationInfo.processName
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val runningAppProcesses = activityManager?.runningAppProcesses ?: listOf()

    return ImmutableList.from(
      runningAppProcesses.filterNotNull().map { runningAppProcessInfo ->
        Application.Process.builder()
          .setName(runningAppProcessInfo.processName)
          .setPid(runningAppProcessInfo.pid)
          .setImportance(runningAppProcessInfo.importance)
          .setIsDefaultProcess(runningAppProcessInfo.processName == defaultProcessName)
          .build()
      }
    )
  }

  /**
   * Gets the current process details.
   *
   * If the current process details are not found for whatever reason, returns process details with
   * just the current process name and pid set.
   */
  fun getProcess(context: Context): Application.Process {
    val pid = Process.myPid()
    return getAppProcesses(context).find { process -> process.pid == pid }
      ?: buildProcess(getProcessName(), pid)
  }

  /** Gets the current process name. If the API is not available, returns an empty string. */
  private fun getProcessName(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Process.myProcessName()
    } else {
      ""
    }

  /** Builds a Process object from just a process name and pid. */
  private fun buildProcess(processName: String, pid: Int) =
    Application.Process.builder()
      .setName(processName)
      .setPid(pid)
      .setImportance(0)
      .setIsDefaultProcess(false)
      .build()
}
