/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.scheduler;

import android.content.Context;
import javax.inject.Inject;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.ListWrapper;
import net.simonvt.cathode.remote.action.lists.CreateList;

public class ListsTaskScheduler extends BaseTaskScheduler {

  @Inject SyncService syncService;

  public ListsTaskScheduler(Context context) {
    super(context);
  }

  public void createList(final String name, final String description, final Privacy privacy,
      final boolean displayNumbers, final boolean allowComments) {
    execute(new Runnable() {
      @Override public void run() {
        final long listId =
            ListWrapper.createList(context.getContentResolver(), name, description, privacy,
                displayNumbers, allowComments);

        queue(new CreateList(listId, name, description, privacy, displayNumbers, allowComments));
      }
    });
  }
}
