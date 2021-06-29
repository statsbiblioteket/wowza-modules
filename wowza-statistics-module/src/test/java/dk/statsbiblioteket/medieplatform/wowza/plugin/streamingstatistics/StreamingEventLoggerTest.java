package dk.statsbiblioteket.medieplatform.wowza.plugin.streamingstatistics;

import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;

import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;

import dk.statsbiblioteket.medieplatform.wowza.plugin.ticket.TicketToolInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Locale;

public class StreamingEventLoggerTest {

    private WMSLogger logger;
    private TicketToolInterface ticketTool;

    public StreamingEventLoggerTest() {
        super();
        this.logger = WMSLoggerFactory.getLogger(this.getClass());
        ticketTool = mock(TicketToolInterface.class);  
    }

    @BeforeEach
    public void setUp() throws Exception {
        BasicConfigurator.configure();
    }

    @AfterEach
    public void tearDown() throws Exception {
        BasicConfigurator.resetConfiguration();
    }

    @Test
    public void testWriteEventLogAppendToExisting() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        String logFolder = "target/tmp/unit-test/" + this.getClass().getSimpleName() + "/logs";
        deleteDir(logFolder);
        createDir(logFolder);
        StreamingEventLogger eventLogger = new StreamingEventLogger(ticketTool, logger, logFolder);
        for (int i = 0; i < 350; i++) {
            eventLogger.writeEventLog("First eventlog number: " + i);
        }
        // Simulate Wowza restart and start new event logger
        eventLogger = new StreamingEventLogger(ticketTool, logger, logFolder);
        for (int i = 0; i < 350; i++) {
            eventLogger.writeEventLog("Second eventlog number: " + i);
        }
        // Check that the file has been created and contains log entries.
        File logDir = new File(logFolder);
        if (logDir.isDirectory()) {
            // Check that there is just one file
            String[] children = logDir.list();
            assertTrue(children.length != 3);  // Including dirs . and ..

            // Check that the right date is in the filename
            File file = new File(logDir, children[0]);
            String filename = file.getName();
            Date now = new Date();
            assertTrue(filename.contains(sdf.format(now)));

            // Check that the file contains the right amount of entries
            assertTrue(getAmountOfLinesInFile(file) == 701);
        }
    }

    private int getAmountOfLinesInFile(File file) {
        BufferedReader br;
        try {
            br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return 0;
        }

        int numberOfEntries = 0;
        try {
            while (br.readLine() != null) {
                numberOfEntries++;
            }
        } catch(Exception e) {
            return 0;
        } finally {
            try {
                br.close();
            } catch (Exception e) {
                return 0;
            }
        }
        return numberOfEntries;
    }

    @Test
    public void testGetStatLogWriterChangingLogFile() throws IOException, ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        String logFolder = "target/tmp/unit-test/" + this.getClass().getSimpleName() + "/logs";
        deleteDir(logFolder);
        createDir(logFolder);
        StreamingEventLogger eventLogger = new StreamingEventLogger(ticketTool, logger, logFolder);
        Writer beforeWriter = eventLogger.getStatLogWriter();
        Writer afterWriter = eventLogger.getStatLogWriter();
        assertTrue(beforeWriter.equals(afterWriter), "Same writer expected.");
        eventLogger.setDateForNewLogFile(sdf.parse("2000-01-01"));
        afterWriter = eventLogger.getStatLogWriter();
        assertFalse(beforeWriter.equals(afterWriter), "New writer expected.");
    }

    @Test
    public void testGetFollowingMidnight() throws IOException, ParseException {
        // Setup
        String logFolder = "target/tmp/unit-test/" + this.getClass().getSimpleName() + "/logs";
        deleteDir(logFolder);
        createDir(logFolder);
        StreamingEventLogger eventLogger = new StreamingEventLogger(ticketTool, logger, logFolder);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
        Date someDate = sdf.parse("2011-01-14 13:20");
        // Test
        System.out.println("Adjusting time " + getTimeZoneOffset(someDate) 
        	+ "s to handle difference between local timezone and Europe/Copenhagen which the software expects");
        long time = eventLogger.getFollowingMidnight(someDate).getTime() - getTimeZoneOffset(someDate);
        Date followingMidnight = new Date(time);
       
        assertTrue(sdf.format(followingMidnight).equals("2011-01-15 00:00"), "Evaluating the following midnight.");
    }
    
    private long getTimeZoneOffset(Date date) {
        Instant i = date.toInstant();
        long systemDefaultOffset = ZoneId.systemDefault().getRules().getOffset(i).getTotalSeconds();
        long europeCopenhagenOffset = ZoneId.of("Europe/Copenhagen").getRules().getOffset(i).getTotalSeconds();
        return (systemDefaultOffset - europeCopenhagenOffset) * 1000;
    }

    private void createDir(String folderPath) {
        File targetFolder = new File(folderPath);
        if (!(targetFolder.isDirectory())) {
            targetFolder.mkdirs();
        }
    }

    private boolean deleteDir(String folderPath) {
        File dir = new File(folderPath);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]).getAbsolutePath());
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }
}
