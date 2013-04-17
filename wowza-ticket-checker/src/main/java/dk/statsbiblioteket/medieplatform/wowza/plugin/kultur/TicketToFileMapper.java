package dk.statsbiblioteket.medieplatform.wowza.plugin.kultur;

import com.wowza.wms.client.IClient;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamFileMapper;
import dk.statsbiblioteket.medieplatform.contentresolver.lib.ContentResolver;
import dk.statsbiblioteket.medieplatform.contentresolver.model.Resource;
import dk.statsbiblioteket.medieplatform.ticketsystem.Ticket;
import dk.statsbiblioteket.medieplatform.wowza.plugin.ticket.TicketToolInterface;
import dk.statsbiblioteket.medieplatform.wowza.plugin.utilities.IllegallyFormattedQueryStringException;
import dk.statsbiblioteket.medieplatform.wowza.plugin.utilities.QueryUtil;

import java.io.File;
import java.util.List;

/**
 * This class is used to validate the ticket and let the user see the correct file
 */
public class TicketToFileMapper implements IMediaStreamFileMapper {

    private final WMSLogger logger;
    private String presentationType;
    private final IMediaStreamFileMapper defaultMapper;
    private final TicketToolInterface ticketTool;
    private final String invalidTicketVideo;
    private final ContentResolver contentResolver;

    public TicketToFileMapper(String presentationType, IMediaStreamFileMapper defaultMapper, TicketToolInterface ticketTool,
                              String invalidTicketVideo, ContentResolver contentResolver) {
        super();
        this.presentationType = presentationType;
        this.defaultMapper = defaultMapper;
        this.contentResolver = contentResolver;
        this.logger = WMSLoggerFactory.getLogger(this.getClass());
        this.ticketTool = ticketTool;
        this.invalidTicketVideo = invalidTicketVideo;
    }

    @Override
    public File streamToFileForRead(IMediaStream stream) {
        logger.trace("streamToFileForRead(IMediaStream stream=" + stream + ")");
        String name = stream.getName();
        String ext = stream.getExt();
        String query = stream.getQueryStr();
        return streamToFileForRead(stream, name, ext, query);
    }

    /**
     * This method is invoked when Wowza tries to figure out which file to play
     * @param stream the stream requested
     * @param name the name of the file to play
     * @param ext the extension of the file
     * @param streamQuery ?
     * @return the file to play
     */
    @Override
    public File streamToFileForRead(IMediaStream stream, String name, String ext, String streamQuery) {
        logger.trace("streamToFileForRead(IMediaStream stream=" + stream + ", String name=" + name
                            + ", String ext=" + ext + ", String streamQuery=" + streamQuery + ")");
        IClient client = stream.getClient();
        if (client == null) {
            // This is the case when a live stream is generated.
            // Two streams are created, and one streams from VLC to Wowza and has no client.
            // If omitted, no live stream is played.
            logger.debug("No client, returning ", stream);
            return null;
        }
        String clientQuery = stream.getClient().getQueryStr();
        File streamingFile;
        try {
            dk.statsbiblioteket.medieplatform.ticketsystem.Ticket streamingTicket = getTicket(clientQuery);
            logger.info("Ticket received: " + streamingTicket);
            if (
                    streamingTicket != null &&
                    isClientAllowed(stream, streamingTicket) &&
                            ticketForThisPresentationType(streamingTicket) &&
                    doesTicketAllowThisStream(name,streamingTicket)
                    ) {
                logger.info("Streaming allowed");
                streamingFile = getFileToStream(name);

            } else {
                logger.info("Client not allowed to get content streamed");
                streamingFile = getErrorMediaFile();
                stream.setName(streamingFile.getName());
            }
        } catch (IllegallyFormattedQueryStringException e) {
            logger.error("Exception received.");
            streamingFile = getErrorMediaFile();
            stream.setName(streamingFile.getName());
            logger.warn("Illegally formatted query string [" + clientQuery + "]." +
                    " Playing file: " + streamingFile.getAbsolutePath());
        }
        logger.info("Resulting streaming file: " + streamingFile.getAbsolutePath());
        logger.info("Resulting streaming file exist: " + streamingFile.exists());
        return streamingFile;
    }

    private boolean ticketForThisPresentationType(Ticket streamingTicket) {
        return streamingTicket.getType().equals(presentationType);
    }

    private boolean doesTicketAllowThisStream(String name, dk.statsbiblioteket.medieplatform.ticketsystem.Ticket streamingTicket) {
        name = clean(name);
        boolean ticketForThis = false;
        for (String resource : streamingTicket.getResources()) {
            if (resource.contains(name)){
                ticketForThis = true;
                break;
            }
        }
        return ticketForThis;
    }

    private String clean(String name) {
        if (name.contains(".")){
            name = name.substring(0,name.indexOf("."));
        }
        if (name.contains(":")) {
            name = name.substring(name.lastIndexOf(':') + 1);
        }

        return name;  //To change body of created methods use File | Settings | File Templates.
    }

    private File getErrorMediaFile() {
        return new File(this.invalidTicketVideo);
    }

    /**
     * This method gets the ticketID from the querystring and resolves it through the ticket-system
     * @param queryString
     * @return an Unmarshalled ticket
     * @throws IllegallyFormattedQueryStringException
     */
    private dk.statsbiblioteket.medieplatform.ticketsystem.Ticket getTicket(String queryString) throws IllegallyFormattedQueryStringException {
        logger.info("getTicket: Query: " + queryString);
        String ticketID = QueryUtil.extractTicketID(queryString);
        logger.info("getTicket: query: " + ticketID);
        dk.statsbiblioteket.medieplatform.ticketsystem.Ticket streamingTicket = ticketTool.resolveTicket(ticketID);
        logger.info("getTicket: streamingTicket: " + streamingTicket);
        logger.info("queryString     : " + queryString);
        logger.info("ticketID        : " + ticketID);
        return streamingTicket;
    }

    /**
     * This method checks if the ticket is given to the same IP address as the client
     * @param stream the stream
     * @param streamingTicket the ticket
     * @return true if the ip is the same for the ticket and the user
     */
    private boolean isClientAllowed(IMediaStream stream, dk.statsbiblioteket.medieplatform.ticketsystem.Ticket streamingTicket) {
        String ipOfClient = stream.getClient().getIp();
        //TODO test presentationType

        boolean isAllowed = (ipOfClient != null) && (ipOfClient.equals(streamingTicket.getUserIdentifier()));
        logger.info("isClientAllowedStreamingContent - ipOfClient: " + ipOfClient);
        logger.info(
                "isClientAllowedStreamingContent - streamingTicket.getUsername(): " + streamingTicket.getUserIdentifier());
        logger.info("isClientAllowedStreamingContent - isAllowed: " + isAllowed);
        return isAllowed;
    }

    /**
     * This method retrieves the filename from the ticket, by querying the content resolver to get the
     * streaming resource
     * @param name the filename
     * @return the file to stream
     */
    protected File getFileToStream(String name) {
        // Extract
        name = clean(name);
        String filenameAndPath = getErrorMediaFile().getPath();
        logger.info("Looking up '" + name + "'");
        List<Resource> resources = contentResolver.getContent(name).getResources();
        if (resources != null) {
            for (Resource resource : resources) {
                if (resource.getType().equals("Stream")) {
                    filenameAndPath = resource.getUris().get(0).getPath();
                }
            }
        }
        File streamingFile = new File(filenameAndPath);
        logger.info("filenameAndPath : " + filenameAndPath);
        return streamingFile;
    }

    @Override
    public File streamToFileForWrite(IMediaStream stream) {
        logger.info("streamToFileForWrite(IMediaStream stream):" + stream);
        return defaultMapper.streamToFileForRead(stream);
    }

    @Override
    public File streamToFileForWrite(IMediaStream stream, String name, String ext, String query) {
        logger.info("streamToFileForWrite(IMediaStream stream, String name, String ext, String query)" + stream);
        return defaultMapper.streamToFileForRead(stream, name, ext, query);
    }

}
