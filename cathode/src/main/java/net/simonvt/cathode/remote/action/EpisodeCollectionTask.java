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
package net.simonvt.cathode.remote.action;

import javax.inject.Inject;
import net.simonvt.cathode.api.body.ShowEpisodeBody;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.remote.TraktTask;

public class EpisodeCollectionTask extends TraktTask {

  @Inject transient ShowService showService;

  private int tvdbId;

  private int season;

  private int episode;

  private boolean inCollection;

  public EpisodeCollectionTask(int tvdbId, int season, int episode, boolean inCollection) {
    this.tvdbId = tvdbId;
    this.season = season;
    this.episode = episode;
    this.inCollection = inCollection;
  }

  @Override protected void doTask() {
    if (inCollection) {
      Response response = showService.episodeLibrary(new ShowEpisodeBody(tvdbId, season, episode));
    } else {
      Response response =
          showService.episodeUnlibrary(new ShowEpisodeBody(tvdbId, season, episode));
    }

    EpisodeWrapper.setInCollection(service.getContentResolver(), tvdbId, season, episode,
        inCollection);
    postOnSuccess();
  }
}
