package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Track extends Model {

    private Long trackId;
    private Long albumId;
    private Long mediaTypeId;
    private Long genreId;
    private String name;
    private Long milliseconds;
    private Long bytes;
    private BigDecimal unitPrice;

    public static final String REDIS_CACHE_KEY = "cs440-tracks-count-cache";
    public static final String REDIS_CACHE_KEY_ARTIST_NAME = "cs440-tracks-artist-name-cache";
    public static final String REDIS_CACHE_KEY_ALBUM_TITLE = "cs440-tracks-albume-title-cache";

    public Track() {
        mediaTypeId = 1l;
        genreId = 1l;
        milliseconds  = 0l;
        bytes  = 0l;
        unitPrice = new BigDecimal("0");
    }

    Track(ResultSet results) throws SQLException {
        name = results.getString("Name");
        milliseconds = results.getLong("Milliseconds");
        bytes = results.getLong("Bytes");
        unitPrice = results.getBigDecimal("UnitPrice");
        trackId = results.getLong("TrackId");
        albumId = results.getLong("AlbumId");
        mediaTypeId = results.getLong("MediaTypeId");
        genreId = results.getLong("GenreId");
    }

    public static Track find(long i) {
        // SELECT artist and album associated with the track and store track artist's name and album title info as cache
        Jedis redisClient = new Jedis();
        try (Connection conn = DB.connect();
             // eliminate ambiguity with AS clause
             PreparedStatement stmt = conn.prepareStatement("SELECT *, album.Title AS AlbumTitle, artist.Name AS ArtistName\n" +
                     "FROM tracks\n" +
                     "         JOIN albums album on tracks.AlbumId = album.AlbumId\n" +
                     "         JOIN artists artist on album.ArtistId = artist.ArtistId\n" +
                     "WHERE TrackId = ?;")) {
            stmt.setLong(1, i);
            ResultSet results = stmt.executeQuery();
            if (results.next()) {
                // set cache based on new track info
                String albumTitleString = new StringBuilder().append(results.getString("AlbumTitle")).toString();
                redisClient.set(REDIS_CACHE_KEY_ALBUM_TITLE, albumTitleString);
                String artistNameString = new StringBuilder().append(results.getString("ArtistName")).toString();
                redisClient.set(REDIS_CACHE_KEY_ARTIST_NAME, artistNameString);
                return new Track(results);
            } else {
                return null;
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public static Long count() {
        Jedis redisClient = new Jedis(); // use this class to access redis and create a cache
        String currentCacheValue = redisClient.get(REDIS_CACHE_KEY);
        // check if the redis cache is null
        if (currentCacheValue == null) {
            // if so, do query
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as Count FROM tracks")) {
                ResultSet results = stmt.executeQuery();
                if (results.next()) {
                    // store it in redis
                    String countString = new StringBuilder().append(results.getLong("Count")).toString();
                    redisClient.set(REDIS_CACHE_KEY, countString);
                    return results.getLong("Count");
                } else {
                    throw new IllegalStateException("Should find a count!");
                }
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        }
        // otherwise convert to long and return
        return Long.parseLong(currentCacheValue);

        // think of operations (like adding and deleting) that invalidates the COUNT(*)
    }

    public Album getAlbum() {
        return Album.find(albumId);
    }

    public MediaType getMediaType() {
        return null;
    }
    public Genre getGenre() {
        return null;
    }
    public List<Playlist> getPlaylists(){
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT *\n" +
                             "FROM playlists\n" +
                             "         JOIN playlist_track pt on playlists.PlaylistId = pt.PlaylistId\n" +
                             "         JOIN tracks t on pt.TrackId = t.TrackId\n" +
                             "WHERE t.TrackId = ?;"
             )) {
            stmt.setLong(1, this.getTrackId());
            ResultSet results = stmt.executeQuery();
            List<Playlist> resultList = new LinkedList<Playlist>();
            while (results.next()) {
                resultList.add(new Playlist(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public Long getTrackId() {
        return trackId;
    }

    public void setTrackId(Long trackId) {
        this.trackId = trackId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMilliseconds() {
        return milliseconds;
    }

    public void setMilliseconds(Long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public void setAlbum(Album album) {
        albumId = album.getAlbumId();
    }

    public Long getMediaTypeId() {
        return mediaTypeId;
    }

    public void setMediaTypeId(Long mediaTypeId) {
        this.mediaTypeId = mediaTypeId;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public String getArtistName() {
        // need to SELECT artist and album associated with the track in find() method and store these info as cache
        Jedis redisClient = new Jedis(); // use this class to access redis and create a cache
        String currentCacheValue = redisClient.get(REDIS_CACHE_KEY_ARTIST_NAME);
        // check if the redis cache is null
        if (currentCacheValue == null) {
            String artistName = getAlbum().getArtist().getName();
            // cache on this model object
            redisClient.set(REDIS_CACHE_KEY_ARTIST_NAME, artistName);
            return artistName;
        }
        return currentCacheValue;
        // consider cache invalidation
    }

    public String getAlbumTitle() {
        Jedis redisClient = new Jedis();
        String currentCacheValue = redisClient.get(REDIS_CACHE_KEY_ALBUM_TITLE);
        if (currentCacheValue == null) {
            String albumTitle = getAlbum().getTitle();
            redisClient.set(REDIS_CACHE_KEY_ALBUM_TITLE, albumTitle);
            return albumTitle;
        }
        return currentCacheValue;
        // consider cache invalidation
    }

    @Override
    public boolean create() {
        if (verify()) {
            Jedis redisClient = new Jedis();
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO tracks (Name, AlbumId, MediaTypeId, Milliseconds, UnitPrice) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setString(1, this.getName());
                stmt.setLong(2, this.getAlbumId());
                stmt.setLong(3, this.getMediaTypeId());
                stmt.setLong(4, this.getMilliseconds());
                stmt.setBigDecimal(5, this.getUnitPrice());
                stmt.executeUpdate();
                trackId = DB.getLastID(conn);
                // invalidating cached COUNT(*) of tracks since we just added a new row to the table
                // I believe incrementing the count cache is a better approach here...
                redisClient.del(REDIS_CACHE_KEY);
                // invalidating cached artist name and album title
                redisClient.del(REDIS_CACHE_KEY_ARTIST_NAME);
                redisClient.del(REDIS_CACHE_KEY_ALBUM_TITLE);
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

    @Override
    public void delete() {
        Jedis redisClient = new Jedis();
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM tracks WHERE TrackId=?")) {
            stmt.setLong(1, this.getTrackId());
            stmt.executeUpdate();
            redisClient.del(REDIS_CACHE_KEY);
            redisClient.del(REDIS_CACHE_KEY_ARTIST_NAME);
            redisClient.del(REDIS_CACHE_KEY_ALBUM_TITLE);
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    @Override
    public boolean verify() {
        _errors.clear(); // clear any existing errors
        if (name == null || "".equals(name)) {
            addError("Name can't be null or blank!");
        }
        if (albumId == null || "".equals(albumId)) {
            addError("albumId can't be null!");
        }
        return !hasErrors();
    }

    @Override
    public boolean update() {
        if (verify()) {
            Jedis redisClient = new Jedis();
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE tracks SET Name = ? WHERE TrackId = ?")) {
                stmt.setString(1, this.getName());
                stmt.setLong(2, this.getTrackId());
                stmt.executeUpdate();
                redisClient.del(REDIS_CACHE_KEY_ARTIST_NAME);
                redisClient.del(REDIS_CACHE_KEY_ALBUM_TITLE);
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }



    public static List<Track> advancedSearch(int page, int count,
                                             String search, Integer artistId, Integer albumId,
                                             Integer maxRuntime, Integer minRuntime) {
        LinkedList<Object> args = new LinkedList<>();

        String query = "SELECT * , tracks.AlbumId AS TracksAlbumId FROM tracks " +
                "JOIN albums ON tracks.AlbumId = albums.AlbumId " +
                "WHERE name LIKE ?";
        args.add("%" + search + "%");

        // Here is an example of how to conditionally
        if (artistId != null) {
            query += " AND ArtistId=? ";
            args.add(artistId);
        }
        if (albumId != null) {
            query += " AND TracksAlbumId = ?";
            args.add(albumId);
        }
        if (maxRuntime != null) {
            query += " AND Milliseconds < ?";
            args.add(maxRuntime);
        }
        if (minRuntime != null) {
            query += " AND Milliseconds > ?";
            args.add(minRuntime);
        }

        //  include the limit (you should include the page too :)
        query += " LIMIT ?";
        args.add(count);
        int offset = count * (page - 1);
        query += " OFFSET ?";
        args.add(offset);

        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < args.size(); i++) {
                Object arg = args.get(i);
                stmt.setObject(i + 1, arg);
            }
            ResultSet results = stmt.executeQuery();
            List<Track> resultList = new LinkedList<>();
            while (results.next()) {
                resultList.add(new Track(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public static List<Track> search(int page, int count, String orderBy, String search) {
        String query = "SELECT * FROM tracks WHERE name LIKE ? LIMIT ?";
        search = "%" + search + "%";
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, search);
            stmt.setInt(2, count);
            ResultSet results = stmt.executeQuery();
            List<Track> resultList = new LinkedList<>();
            while (results.next()) {
                resultList.add(new Track(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public static List<Track> forAlbum(Long albumId) {
        String query = "SELECT * FROM tracks WHERE AlbumId=?";
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, albumId);
            ResultSet results = stmt.executeQuery();
            List<Track> resultList = new LinkedList<>();
            while (results.next()) {
                resultList.add(new Track(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    // Sure would be nice if java supported default parameter values
    public static List<Track> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Track> all(int page, int count) {
        return all(page, count, "TrackId");
    }

    public static List<Track> all(int page, int count, String orderBy) {
        int offset = count * (page - 1);
        try (Connection conn = DB.connect();
             // an order by can't use a prepared statement variable THIS IS DUMB GIVE ME MY HOUR BACK
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM tracks ORDER BY " + orderBy + " LIMIT ? OFFSET ?;"
             )) {
            stmt.setInt(1, count);
            stmt.setInt(2, offset);
            ResultSet results = stmt.executeQuery();
            List<Track> resultList = new LinkedList<>();
            while (results.next()) {
                resultList.add(new Track(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

}

