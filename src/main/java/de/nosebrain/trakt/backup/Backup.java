package de.nosebrain.trakt.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;

import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseEpisode;
import com.uwetrottmann.trakt.v2.entities.BaseMovie;
import com.uwetrottmann.trakt.v2.entities.BaseSeason;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.services.Users;

public class Backup {
  private static final String ACCESS_TOKEN = "access.token";

  public static void main(final String[] args) throws Exception {
    final Properties properties = new Properties();
    final File propertiesFile = new File(args[0]);
    properties.load(new FileInputStream(propertiesFile));
    
    final File outputFolder = new File(args[1]);
    if (!outputFolder.exists()) {
      outputFolder.mkdirs();
    }
    
    final String username = properties.getProperty("username");
    final TraktV2 trakt = new TraktV2();
    
    final String clientId = properties.getProperty("client.id");
    final String clientSecret = properties.getProperty("client.secret");
    trakt.setApiKey(clientId);
    
    String accessToken = properties.getProperty(ACCESS_TOKEN);
    if (accessToken == null) {
      System.out.println("Enter authCode: ");
      final Scanner sc = new Scanner(System.in);
      final String authCode = sc.next();
      sc.close();
      final OAuthAccessTokenResponse code = TraktV2.getAccessToken(clientId, clientSecret, "urn:ietf:wg:oauth:2.0:oob", authCode);
      accessToken = code.getAccessToken();
      properties.setProperty(ACCESS_TOKEN, accessToken);
      properties.store(new FileOutputStream(propertiesFile), null);
    }
    
    trakt.setAccessToken(accessToken);
    
    final File showOutputFolder = new File(outputFolder, "shows");
    showOutputFolder.mkdir();
    
    final Users users = trakt.users();
    final List<BaseShow> watchedShows = users.watchedShows(username, null);
    
    for (final BaseShow baseShow : watchedShows) {
      final Show show = baseShow.show;
      final String title = show.title;
      System.out.println(title);
      
      final File showFile = new File(showOutputFolder, title + ".txt");
      final Writer writer = new OutputStreamWriter(new FileOutputStream(showFile));
      
      for (final BaseSeason season : baseShow.seasons) {
        writer.write(season.number + ". Staffel\n");
        for (final BaseEpisode episode : sortEpisodes(season.episodes)) {
          writer.write(String.valueOf(episode.number));
          writer.write("\n");
        }
      }
      writer.close();
    }
    
    final File movieFile = new File(outputFolder, "Movies.txt");
    final Writer writer = new OutputStreamWriter(new FileOutputStream(movieFile), "UTF-8");
    
    final List<BaseMovie> watchedMovies = users.watchedMovies(username, null);
    for (final BaseMovie baseMovie : sortMovies(watchedMovies)) {
      writer.write(baseMovie.movie.title);
      writer.write("\n");
    }
    
    writer.close();
  }
  
  private static <T> SortedSet<T> sort(final Collection<T> collection, final Comparator<T> comparator) {
    final SortedSet<T> sorted = new TreeSet<T>(comparator);
    
    for (final T element : collection) {
      sorted.add(element);
    }
    return sorted;
  }

  private static SortedSet<BaseMovie> sortMovies(final List<BaseMovie> watchedMovies) {
    return sort(watchedMovies, new Comparator<BaseMovie>() {
      @Override
      public int compare(final BaseMovie o1, final BaseMovie o2) {
        return o1.movie.title.compareTo(o2.movie.title);
      }
    });
  }

  private static SortedSet<BaseEpisode> sortEpisodes(final List<BaseEpisode> episodes) {
    return sort(episodes, new Comparator<BaseEpisode>() {
      @Override
      public int compare(final BaseEpisode o1, final BaseEpisode o2) {
        return o1.number - o2.number;
      }
    });
  }
}
