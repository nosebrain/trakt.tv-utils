package de.nosebrain.trakt.stats;

import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.Episode;
import com.uwetrottmann.trakt.v2.entities.HistoryEntry;
import com.uwetrottmann.trakt.v2.entities.Season;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.entities.Username;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.enums.Status;
import com.uwetrottmann.trakt.v2.services.Users;
import de.nosebrain.trakt.util.AuthUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Stats {
  private static final int STEP_SIZE = 100;

  public static void main(final String[] args) throws Exception {
    final Properties properties = new Properties();
    final File propertiesFile = new File(args[0]);
    properties.load(new FileInputStream(propertiesFile));
    
    final Username username = new Username(properties.getProperty("username"));
    final String clientId = properties.getProperty("client.id");
    final String clientSecret = properties.getProperty("client.secret");
    final TraktV2 trakt = AuthUtil.getAccess(clientId, clientSecret);
    
    final Set<String> startedSeries = new TreeSet<>();
    
    final Map<String, Episode> showsInfo = new HashMap<>();
    final List<String> watchedShows = new LinkedList<>();
    
    int moviesWatched = 0;
    
    int episodeCount = 0;
    
    final Users users = trakt.users();
    List<HistoryEntry> history;
    int page = 0;
    final int statYear = Integer.parseInt(args[1]);
    loop:
    do {
      history = users.history(username, page, STEP_SIZE, Extended.DEFAULT_MIN);
      page++;
      
      for (final HistoryEntry historyEntry : history) {
        final int year = historyEntry.watched_at.getYear();
        if (year != statYear) {
          continue;
        }
        if (year < statYear) {
          break loop;
        }
        
        if (historyEntry.action.equals("watch")) {
          switch (historyEntry.type) {
            case "movie":
              moviesWatched++;
              break;
            case "episode":
              episodeCount++;
              final Episode episode = historyEntry.episode;
              final Show show = historyEntry.show;
              final String slug = show.ids.slug;
              if ((episode.number == 1) && (episode.season == 1)) {
                startedSeries.add(show.title);
              }
              
              final Episode currentLastEpisode = showsInfo.get(slug);
              if ((currentLastEpisode == null) || (episode.season.intValue() > currentLastEpisode.season.intValue()) || ((episode.season.intValue() == currentLastEpisode.season.intValue()) && (episode.number.intValue() > currentLastEpisode.number.intValue()))) {
                showsInfo.put(slug, episode);
              }
              
              watchedShows.add(show.title);
              break;
            default:
              System.err.print(historyEntry.type);
              break;
          }
        }
      }
    } while (history.size() == STEP_SIZE);
    
    final Set<String> finishedShows = new TreeSet<>();
    
    for (final Entry<String, Episode> showEpisodeEntry : showsInfo.entrySet()) {
      final Episode episode = showEpisodeEntry.getValue();
      final String slug = showEpisodeEntry.getKey();
      final List<Season> seasons = trakt.seasons().summary(slug, Extended.FULL);
      final Season lastSeason = seasons.stream().max((s1, s2) -> s1.number - s2.number).get();
      final int maxSeasonNumber = lastSeason.number;
      
      if (maxSeasonNumber == episode.season) {
        if (lastSeason.episode_count == episode.number) {
          final Show show = trakt.shows().summary(slug, Extended.FULL);
          final Status status = show.status;
          if (Status.CANCELED.equals(status) || Status.ENDED.equals(status)) {
            finishedShows.add(show.title);
          }
        }
      }
    }
    
    System.out.println("Stats for " + statYear);
    System.out.println("movies watched: \t" + moviesWatched);
    System.out.println("episodes watched: \t" + episodeCount);
    
    final Map<String, Long> counted = watchedShows.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    counted.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(System.out::println);
    
    System.out.println("you started the following shows (total: " + startedSeries.size() + ")");
    int number = 1;
    for (final String show : startedSeries) {
      System.out.println(number++ + ".\t" + show);
    }
    System.out.println("you finished the following shows (total: " + finishedShows.size() + ")");
    number = 1;
    for (final String show : finishedShows) {
      System.out.println(number++ + ".\t" + show);
    }
  }
}
