/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.LogoutEvent;
import net.simonvt.cathode.settings.Settings;

public class LogoutDialog extends DialogFragment {

  @Inject Bus bus;

  @Override public Dialog onCreateDialog(Bundle inState) {
    return new AlertDialog.Builder(getActivity()).setTitle(R.string.logout_title)
        .setMessage(R.string.logout_message)
        .setPositiveButton(R.string.logout_button, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            final Context context = getActivity().getApplicationContext();
            CathodeApp.inject(context, LogoutDialog.this);

            PreferenceManager.getDefaultSharedPreferences(getActivity()) //
                .edit() //
                .putBoolean(Settings.TRAKT_LOGGED_IN, false) //
                .apply();

            bus.post(new LogoutEvent());
          }
        })
        .setNegativeButton(R.string.cancel, null)
        .create();
  }
}
