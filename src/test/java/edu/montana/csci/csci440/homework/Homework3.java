package edu.montana.csci.csci440.homework;

import edu.montana.csci.csci440.DBTest;
import edu.montana.csci.csci440.model.Track;
import edu.montana.csci.csci440.util.DB;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Homework3 extends DBTest {

    @Test
    /*
     * Use a transaction to safely move 10 milliseconds from one track to another.
     *
     * You will need to use the JDBC transaction API, outlined here:
     *
     *   https://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html
     *
     */
    public void useATransactionToSafelyMoveMillisecondsFromOneTrackToAnother() throws SQLException {

        Track track1 = Track.find(1);
        Long track1InitialTime = track1.getMilliseconds();
        Track track2 = Track.find(2);
        Long track2InitialTime = track2.getMilliseconds();

        try(Connection connection = DB.connect()){
            connection.setAutoCommit(false);
            PreparedStatement subtract = connection.prepareStatement("UPDATE tracks SET Milliseconds = Milliseconds - ?" + "WHERE TrackId = ?");
            subtract.setLong(1, 10);
            subtract.setLong(2,  1);
            subtract.execute();

            PreparedStatement add = connection.prepareStatement("UPDATE tracks SET Milliseconds = Milliseconds + ?" + "WHERE TrackId = ?");
            add.setLong(1, 10);
            add.setLong(2, 2);
            add.execute();

        // commit with the connection
            connection.commit();
        }

        // refresh tracks from db
        track1 = Track.find(1);
        track2 = Track.find(2);
        assertEquals(track1.getMilliseconds(), track1InitialTime - 10);
        assertEquals(track2.getMilliseconds(), track2InitialTime + 10);
    }

    @Test
    /*
     * Select tracks that have been sold more than once (> 1)
     *
     * Select the albumbs that have tracks that have been sold more than once (> 1)
     *   NOTE: This is NOT the same as albums whose tracks have been sold more than once!
     *         An album could have had three tracks, each sold once, and should not be included
     *         in this result.  It should only include the albums of the tracks found in the first
     *         query.
     * */
    public void selectPopularTracksAndTheirAlbums() throws SQLException {

        // HINT: join to invoice items and do a group by/having to get the right answer
        // basically if a TrackID appears in invoice_items more than once, select it
        List<Map<String, Object>> tracks = executeSQL("SELECT *\n" +
                "FROM tracks\n" +
                "         JOIN invoice_items ii on tracks.TrackId = ii.TrackId\n" +
                "GROUP BY tracks.TrackId\n" +
                "HAVING COUNT(tracks.TrackId) > 1;");
        assertEquals(256, tracks.size());

        // HINT: join to tracks and invoice items and do a group by/having to get the right answer
        //       note: you will need to use the DISTINCT operator to get the right result!
        List<Map<String, Object>> albums = executeSQL("SELECT DISTINCT AlbumId\n" +
                "FROM (SELECT *\n" +
                "      FROM tracks\n" +
                "               JOIN invoice_items ii on tracks.TrackId = ii.TrackId\n" +
                "      GROUP BY tracks.TrackId\n" +
                "      HAVING COUNT(tracks.TrackId) > 1);");
        assertEquals(166, albums.size());
    }

    @Test
    /*
     * Select customers emails who are assigned to Jane Peacock as a Rep and
     * who have purchased something from the 'Rock' Genre
     *
     * Please use an IN clause and a sub-select to generate customer IDs satisfying the criteria
     * */
    public void selectCustomersMeetingCriteria() throws SQLException {
        // HINT: join to invoice items and do a group by/having to get the right answer
        List<Map<String, Object>> tracks = executeSQL("SELECT *\n" +
                "FROM customers\n" +
                "         JOIN employees e on customers.SupportRepId = e.EmployeeId\n" +
                "WHERE e.FirstName LIKE 'Jane'\n" +
                "GROUP BY customers.CustomerId\n" +
                "HAVING e.LastName LIKE 'Peacock';" );
        assertEquals(21, tracks.size());
    }


}
