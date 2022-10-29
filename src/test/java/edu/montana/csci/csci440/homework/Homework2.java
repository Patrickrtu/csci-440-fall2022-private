package edu.montana.csci.csci440.homework;

import edu.montana.csci.csci440.DBTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Homework2 extends DBTest {

    @Test
    /*
     * Create a view tracksPlus to display the artist, song title, album, and genre for all tracks.
     */
    public void createTracksPlusView(){
        //TODO fill this in
        executeDDL("CREATE VIEW tracksPlus AS\n" +
                "SELECT tracks.Name, a2.Name as ArtistName, a.Title as AlbumTitle, g.Name as GenreName\n" +
                "FROM tracks\n" +
                "         JOIN albums a on tracks.AlbumId = a.AlbumId\n" +
                "         JOIN genres g on tracks.GenreId = g.GenreId\n" +
                "         JOIN artists a2 on a.ArtistId = a2.ArtistId;");

        List<Map<String, Object>> results = executeSQL(
                "SELECT *\n" +
                "FROM tracksPlus");
        assertEquals(3503, results.size());
        assertEquals("Rock", results.get(0).get("GenreName"));
        assertEquals("AC/DC", results.get(0).get("ArtistName"));
        assertEquals("For Those About To Rock We Salute You", results.get(0).get("AlbumTitle"));
    }

    @Test
    /*
     * Create a table grammy_infos to track grammy information for an artist.  The table should include
     * a reference to the artist, the album (if the grammy was for an album) and the song (if the grammy was
     * for a song).  There should be a string column indicating if the artist was nominated or won.  Finally,
     * there should be a reference to the grammy_category table
     *
     * Create a table grammy_category
     */
    public void createGrammyInfoTable(){
        //TODO fill these in
        executeDDL("CREATE TABLE grammy_infos\n" +
                "(\n" +
                "    GrammyInfosID    INTEGER       NOT NULL PRIMARY KEY,\n" +
                "    ArtistId         INTEGER,\n" +
                "    AlbumId          INTEGER,\n" +
                "    TrackId          INTEGER,\n" +
                "    Status           NVARCHAR(160) NOT NULL,\n" +
                "    GrammyCategoryId INTEGER       NOT NULL,\n" +
                "    FOREIGN KEY (ArtistId)\n" +
                "        REFERENCES artists (ArtistId),\n" +
                "    FOREIGN KEY (AlbumId)\n" +
                "        REFERENCES albums (AlbumId),\n" +
                "    FOREIGN KEY (TrackId)\n" +
                "        REFERENCES tracks (TrackId),\n" +
                "    FOREIGN KEY (GrammyCategoryId)\n" +
                "        REFERENCES grammy_categories (GrammyCategoryId)\n" +
                ");");
        executeDDL("CREATE TABLE grammy_categories\n" +
                "(\n" +
                "    GrammyCategoryId INTEGER       NOT NULL PRIMARY KEY,\n" +
                "    Name             NVARCHAR(160) NOT NULL\n" +
                ");");

        // TEST CODE
        executeUpdate("INSERT INTO grammy_categories(Name) VALUES ('Greatest Ever');");
        Object categoryId = executeSQL("SELECT GrammyCategoryId FROM grammy_categories").get(0).get("GrammyCategoryId");

        executeUpdate("INSERT INTO grammy_infos(ArtistId, AlbumId, TrackId, GrammyCategoryId, Status) VALUES (1, 1, 1, " + categoryId + ",'Won');");

        List<Map<String, Object>> results = executeSQL("SELECT * FROM grammy_infos");
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).get("ArtistId"));
        assertEquals(1, results.get(0).get("AlbumId"));
        assertEquals(1, results.get(0).get("TrackId"));
        assertEquals(1, results.get(0).get("GrammyCategoryId"));
    }

    @Test
    /*
     * Bulk insert five categories of your choosing in the genres table
     */
    public void bulkInsertGenres(){
        Integer before = (Integer) executeSQL("SELECT COUNT(*) as COUNT FROM genres").get(0).get("COUNT");

        //TODO fill this in
        executeUpdate("INSERT INTO genres (Name)\n" +
                "VALUES (\"Musical\"),\n" +
                "       (\"Show Tunes\"),\n" +
                "       (\"Soul\"),\n" +
                "       (\"Baroque Pop\"),\n" +
                "       (\"Neoclassical\");");

        Integer after = (Integer) executeSQL("SELECT COUNT(*) as COUNT FROM genres").get(0).get("COUNT");
        assertEquals(before + 5, after);
    }
}