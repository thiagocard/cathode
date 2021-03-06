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
package net.simonvt.cathode.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.R;

public class LicensesDialog extends DialogFragment {

  @Override public Dialog onCreateDialog(Bundle inState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) //
        .setTitle(R.string.licenses).setAdapter(new LicenseAdapter(), null);

    return builder.create();
  }

  private static final class License {
    int library;

    int license;

    private License(int library, int license) {
      this.library = library;
      this.license = license;
    }
  }

  private final class LicenseAdapter extends BaseAdapter {

    List<License> licenses = new ArrayList<>();

    private LicenseAdapter() {
      licenses.add(new License(R.string.license_gson, R.string.license_text_gson));
      licenses.add(new License(R.string.license_android_support_library,
          R.string.license_android_support_library_text));
      licenses.add(new License(R.string.license_dagger, R.string.license_dagger_text));
      licenses.add(new License(R.string.license_okhttp, R.string.license_okhttp_text));
      licenses.add(new License(R.string.license_picasso, R.string.license_picasso_text));
      licenses.add(new License(R.string.license_retrofit, R.string.license_retrofit_text));
      licenses.add(new License(R.string.license_butterknife, R.string.license_butterknife_text));
      licenses.add(new License(R.string.license_timber, R.string.license_timber_text));
      licenses.add(new License(R.string.license_dashclock, R.string.license_dashclock_text));
      licenses.add(new License(R.string.license_tmdb_java, R.string.license_tmdb_java_text));
    }

    @Override public int getCount() {
      return licenses.size();
    }

    @Override public Object getItem(int position) {
      return licenses.get(position);
    }

    @Override public long getItemId(int position) {
      return position;
    }

    @Override public boolean areAllItemsEnabled() {
      return false;
    }

    @Override public boolean isEnabled(int position) {
      return false;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      License item = licenses.get(position);

      if (v == null) {
        v = LayoutInflater.from(getActivity()).inflate(R.layout.row_license, parent, false);
      }

      TextView library = (TextView) v.findViewById(R.id.library);
      library.setText(item.library);

      TextView license = (TextView) v.findViewById(R.id.license);
      license.setText(item.license);

      return v;
    }
  }
}
