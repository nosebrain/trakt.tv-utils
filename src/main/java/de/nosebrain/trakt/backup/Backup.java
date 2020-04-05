package de.nosebrain.trakt.backup;

import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseEpisode;
import com.uwetrottmann.trakt.v2.entities.BaseMovie;
import com.uwetrottmann.trakt.v2.entities.BaseSeason;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.entities.Episode;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.entities.Username;
import com.uwetrottmann.trakt.v2.services.Seasons;
import com.uwetrottmann.trakt.v2.services.Users;
import de.nosebrain.trakt.util.AuthUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

public class Backup {
  private static final String NEW_LINE = "\n";

  public static void main(final String[] args) throws Exception {
    final Properties properties = new Properties();
    final File propertiesFile = new File(args[0]);
    properties.load(new FileInputStream(propertiesFile));
    
    final File outputFolder = new File(args[1]);
    if (!outputFolder.exists()) {
      outputFolder.mkdirs();
    }

    final String username = properties.getProperty("username");
    final String clientId = properties.getProperty("client.id");
    final String clientSecret = properties.getProperty("client.secret");
    
    final TraktV2 trakt = AuthUtil.getAccess(clientId, clientSecret);

    final Username user = new Username(username);
    final File showOutputFolder = new File(outputFolder, "shows");
    showOutputFolder.mkdir();
    
    final Users users = trakt.users();
    final Seasons seasonsApi = trakt.seasons();
    final List<BaseShow> watchedShows = users.watchedShows(user, null);
    
    for (final BaseShow baseShow : watchedShows) {
      final Show show = baseShow.show;
      final String showTitle = show.title + " (" + show.year + ")";
      System.out.println(showTitle);
      
      final File showFile = new File(showOutputFolder, showTitle + ".txt");
      final Writer writer = new OutputStreamWriter(new FileOutputStream(showFile));
      
      for (final BaseSeason season : baseShow.seasons) {
        final Integer seasonNumber = season.number;
        final List<Episode> episodes = seasonsApi.season(baseShow.show.ids.slug, seasonNumber, null);

        writer.write("# " + seasonNumber + ". Season\n");
        for (final BaseEpisode episode : sortEpisodes(season.episodes)) {
          final Integer episodeNumber = episode.number;
          writer.write(String.valueOf(episodeNumber));

          final Optional<Episode> episodeInfo = findEpisode(episodes, episodeNumber);
          if (episodeInfo.isPresent()) {
            writer.write(" ");
            final String episodeTitle = episodeInfo.get().title;
            if (episodeTitle != null) {
              writer.write(episodeTitle);
            }
          }

          writer.write(NEW_LINE);
        }
        writer.write(NEW_LINE);
      }
      writer.close();
    }
    
    final File movieFile = new File(outputFolder, "Movies.txt");
    final Writer writer = new OutputStreamWriter(new FileOutputStream(movieFile), "UTF-8");
    
    final List<BaseMovie> watchedMovies = users.watchedMovies(user, null);
    for (final BaseMovie baseMovie : sortMovies(watchedMovies)) {
      writer.write(baseMovie.movie.title);
      writer.write(NEW_LINE);
    }
    
    writer.close();
  }

  private static Optional<Episode> findEpisode(List<Episode> episodes, Integer episodeNumber) {
    return episodes.stream().filter(episode -> episode.number == episodeNumber).findFirst();
  }

  private static <T> SortedSet<T> sort(final Collection<T> collection, final Comparator<T> comparator) {
    final SortedSet<T> sorted = new TreeSet<>(comparator);
    sorted.addAll(collection);
    return sorted;
  }

  private static SortedSet<BaseMovie>  sortMovies(final List<BaseMovie> watchedMovies) {
    return sort(watchedMovies, Comparator.comparing(o -> o.movie.title));
  }

  private static SortedSet<BaseEpisode> sortEpisodes(final List<BaseEpisode> episodes) {
    return sort(episodes, Comparator.comparingInt(o -> o.number));
  }
}
