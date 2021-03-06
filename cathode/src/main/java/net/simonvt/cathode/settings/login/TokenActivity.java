/*
 * Copyright (C) 2016 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.settings.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.TraktSettings;
import net.simonvt.cathode.api.entity.AccessToken;
import net.simonvt.cathode.api.entity.UserSettings;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.jobscheduler.AuthJobHandlerJob;
import net.simonvt.cathode.jobscheduler.Jobs;
import net.simonvt.cathode.remote.sync.SyncJob;
import net.simonvt.cathode.remote.sync.SyncUserActivity;
import net.simonvt.cathode.settings.Accounts;
import net.simonvt.cathode.settings.ProfileSettings;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.settings.setup.CalendarSetupActivity;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;

public class TokenActivity extends BaseActivity implements TokenTask.Callback {

  static final String EXTRA_CODE = "code";

  @Inject JobManager jobManager;
  @Inject TraktSettings traktSettings;

  @BindView(R.id.buttonContainer) View buttonContainer;
  @BindView(R.id.error_message) TextView errorMessage;

  @BindView(R.id.progressContainer) View progressContainer;

  private int task;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    Intent intent = getIntent();
    task = intent.getIntExtra(LoginActivity.EXTRA_TASK, LoginActivity.TASK_LOGIN);

    setContentView(R.layout.activity_login_token);
    ButterKnife.bind(this);
    Injector.inject(this);

    if (TokenTask.runningInstance != null) {
      TokenTask.runningInstance.setCallback(this);
    } else {
      final String code = getIntent().getStringExtra(EXTRA_CODE);
      TokenTask.start(this, code, this);
    }

    setRefreshing(true);
  }

  @OnClick(R.id.retry) void onRetryClicked() {
    Intent login = new Intent(this, LoginActivity.class);
    login.putExtra(LoginActivity.EXTRA_TASK, task);
    startActivity(login);
    finish();
  }

  void setRefreshing(boolean refreshing) {
    if (refreshing) {
      buttonContainer.setVisibility(View.GONE);
      progressContainer.setVisibility(View.VISIBLE);
    } else {
      buttonContainer.setVisibility(View.VISIBLE);
      progressContainer.setVisibility(View.GONE);
    }
  }

  @Override public void onTokenFetched(AccessToken accessToken, UserSettings userSettings) {
    final boolean wasLinked = Settings.get(this).getBoolean(TraktLinkSettings.TRAKT_LINKED, false);

    Settings.get(this)
        .edit()
        .putBoolean(TraktLinkSettings.TRAKT_LINKED, true)
        .putBoolean(TraktLinkSettings.TRAKT_AUTH_FAILED, false)
        .apply();

    ProfileSettings.clearProfile(this);
    ProfileSettings.updateProfile(this, userSettings);

    Accounts.setupAccount(this);

    jobManager.addJob(new SyncJob());
    if (Jobs.usesScheduler()) {
      AuthJobHandlerJob.schedulePeriodic(this);
      SyncUserActivity.schedulePeriodic(this);
    }

    if (wasLinked) {
      Intent home = new Intent(this, HomeActivity.class);
      startActivity(home);
    } else {
      Intent setup = new Intent(this, CalendarSetupActivity.class);
      startActivity(setup);
    }

    finish();
  }

  @Override public void onTokenFetchedFail(int error) {
    setRefreshing(false);

    errorMessage.setVisibility(View.VISIBLE);
    errorMessage.setText(error);
  }
}
