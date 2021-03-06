package dk.statsbiblioteket.medieplatform.wowza.plugin;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.client.IClient;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamFileMapper;
import dk.statsbiblioteket.medieplatform.contentresolver.lib.ContentResolver;
import dk.statsbiblioteket.medieplatform.contentresolver.lib.DirectoryBasedContentResolver;
import dk.statsbiblioteket.medieplatform.wowza.plugin.utilities.ConfigReader;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the content resolver mapper.
 */
public class ContentResolverMapperTest {

    public static final String RTMP_HYPOTHETICAL_URL = "rtmp://hypothetical-test-machine:1935/mediestream";
    private Logger logger;

    String storageDir = new File(
            getClass().getClassLoader().getResource("streamingDir/README.streamingDir").getPath()).getParent()
            .toString();
    IApplicationInstance iAppInstance = mock(IApplicationInstance.class);


    String programID = "0ef8f946-4e90-4c9d-843a-a03504d2ee6c";
    String name = "0ef8f946-4e90-4c9d-843a-a03504d2ee6c.flv";


    public ContentResolverMapperTest() {
        super();
        this.logger = WMSLoggerFactory.getLogger(this.getClass());
    }

    @BeforeEach
    public void setUp() throws Exception {
        org.apache.log4j.BasicConfigurator.configure();
    }

    @AfterEach
    public void tearDown() throws Exception {
        org.apache.log4j.BasicConfigurator.resetConfiguration();
    }

    @Test
    public void testStdCase() throws IOException {
        // Setup environment
        String queryString = RTMP_HYPOTHETICAL_URL;
        IMediaStreamFileMapper defaultMapper = null;


         //rtmp://iapetus.statsbiblioteket.dk:1937/mediestream?ticket=[ticketId]/flv:853a0b31-c944-44a5-8e42-bc9b5bc697be.flv
        IClient iClient = mock(IClient.class);
        IMediaStream stream = mock(IMediaStream.class);
        when(stream.getClient()).thenReturn(iClient);
        when(stream.getQueryStr()).thenReturn(queryString);
        when(stream.getExt()).thenReturn("flv");
        when(stream.getName()).thenReturn(name);

        ContentResolver contentResolver = new DirectoryBasedContentResolver("Stream", new File(storageDir), 4,
                                                                            "%s\\.flv", "file://" + storageDir + "/%s");
        ContentResolverMapper contentResolverMapper = new ContentResolverMapper("Stream", defaultMapper, contentResolver);
        // Run test
        File result = contentResolverMapper.streamToFileForRead(stream);
        // Validate result
        assertEquals(new File(storageDir + "/0/e/f/8/" + programID + ".flv").getAbsolutePath(),
                     result.getAbsolutePath(), 
                     "Expected equal result");
    }

    @Test
    public void testGetContentResolver() throws IOException {
        // Setup environment
        ContentResolverModule cm = new ContentResolverModule();
        ConfigReader cr = mock(ConfigReader.class);
        when(cr.get("subdirectory","")).thenReturn("");
        when(cr.get("characterDirs","4")).thenReturn("4");
        when(cr.get("characterDirsWidth","1")).thenReturn("0");
        when(cr.get("filenameRegexPattern", "missing-filename-regex-pattern-in-property-file")).thenReturn("missing-filename-regex-pattern-in-property-file");
        ContentResolver contentResolver = cm.getContentResolver(cr, storageDir);
        assertNotNull(contentResolver);
        
        when(cr.get("contentResolverNames")).thenReturn("name1");
        when(cr.get("name1.subdirectory","")).thenReturn("subdir");
        when(cr.get("name1.characterDirs","4")).thenReturn("5");
        when(cr.get("name1.characterDirsWidth","1")).thenReturn("2");
        when(cr.get("name1.filenameRegexPattern", "missing-filename-regex-pattern-in-property-file")).thenReturn("missing-filename-regex-pattern-in-property-file");
        ContentResolver contentResolver2 = cm.getContentResolver(cr, storageDir);
        assertNotNull(contentResolver2);
        
        
/*
                new DirectoryBasedContentResolver("Stream", new File(storageDir), 4,
                "%s\\.flv", "file://" + storageDir + "/%s");
        ContentResolverMapper contentResolverMapper = new ContentResolverMapper("Stream", defaultMapper, contentResolver);
        // Run test
        File result = contentResolverMapper.streamToFileForRead(stream);
        // Validate result
        assertEquals("Expected equal result", new File(storageDir + "/0/e/f/8/" + programID + ".flv").getAbsolutePath(),
                result.getAbsolutePath());
*/
    }


    @Test
    public void testGetFileToStreamSucces() {
        // Setup
        String queryString = RTMP_HYPOTHETICAL_URL;
        IMediaStreamFileMapper defaultMapper = null;

        IClient iClient = mock(IClient.class); 
        IMediaStream stream = mock(IMediaStream.class);
        when(stream.getClient()).thenReturn(iClient);
        when(stream.getQueryStr()).thenReturn(queryString);
        when(stream.getExt()).thenReturn("flv");
        when(stream.getName()).thenReturn(name);
        
        ContentResolver contentResolver = new DirectoryBasedContentResolver("Stream", new File(storageDir), 4,
                                                                            "%s\\.flv", "file://" + storageDir + "/%s");
        ContentResolverMapper contentResolverMapper = new ContentResolverMapper("Stream", defaultMapper, contentResolver);
        // Test
        File result = contentResolverMapper.streamToFileForRead(stream);
        // Validate
        assertEquals(new File(storageDir + "/0/e/f/8/" + programID + ".flv").getAbsolutePath(),
                     result.getAbsolutePath(),
                     "Expected equal result");
    }

    @Test
    public void testRetrieveMediaFileRelativePath() {
        // Setup
        String queryString = RTMP_HYPOTHETICAL_URL;
        IMediaStreamFileMapper defaultMapper = null;

        IClient iClient = mock(IClient.class);
        IMediaStream stream = mock(IMediaStream.class);
        when(stream.getClient()).thenReturn(iClient);
        when(stream.getQueryStr()).thenReturn(queryString);
        when(stream.getExt()).thenReturn("flv");
        when(stream.getName()).thenReturn(name);

        ContentResolver contentResolver = new DirectoryBasedContentResolver("Stream", new File(storageDir), 4,
                                                                            "%s\\.flv", "%s");
        ContentResolverMapper contentResolverMapper = new ContentResolverMapper("Stream", defaultMapper, contentResolver);
        // Test

        String result = contentResolverMapper.streamToFileForRead(stream)
                .getPath();
        // Validate
        assertEquals("0/e/f/8/0ef8f946-4e90-4c9d-843a-a03504d2ee6c.flv", result, "Expected equal result");
    }
}
