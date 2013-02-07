package de.nosebrain.trakt.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowSeason;

public class Backup {
  public static void main(final String[] args) throws IOException {
    final Properties properties = new Properties();
    properties.load(new FileInputStream(new File(args[0])));
    
    final File outputFolder = new File(args[1]);
    if (!outputFolder.exists()) {
      outputFolder.mkdirs();
    }
    
    final String username = properties.getProperty("username");
    final String apiKey = properties.getProperty("apikey");
    final String password = properties.getProperty("password");
    final ServiceManager manager = new ServiceManager();
    manager.setAuthentication(username, password);
    manager.setApiKey(apiKey);
    
    final List<TvShow> shows = manager.userService().libraryShowsWatched(username).fire();
    for (final TvShow tvShow : shows) {
      final File showFile = new File(outputFolder, tvShow.title + ".txt");
      final Writer writer = new OutputStreamWriter(new FileOutputStream(showFile));
      System.out.println(tvShow.title);
      
      for (final TvShowSeason season : sort(tvShow.seasons)) {
        writer.write(season.season + ". Staffel\n");
        for (final Integer episodeNumber : season.episodes.numbers) {
          writer.write(String.valueOf(episodeNumber));
          writer.write("\n");
        }
        writer.write("\n");
      }
      writer.close();
    }
  }

  private static SortedSet<TvShowSeason> sort(final List<TvShowSeason> seasons) {
    final SortedSet<TvShowSeason> sortedSeasons = new TreeSet<TvShowSeason>(new Comparator<TvShowSeason>() {
      public int compare(final TvShowSeason o1, final TvShowSeason o2) {
        return o1.season - o2.season;
      }
    });
    
    for (final TvShowSeason tvShowSeason : seasons) {
      sortedSeasons.add(tvShowSeason);
    }
    return sortedSeasons;
  }
}
