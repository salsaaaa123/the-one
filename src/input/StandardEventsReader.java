/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import core.SimError;

/**
 * <P>
 * External events reader for standard-format events
 * (created e.g by the dtnsim2parser).
 * </P>
 * <P>
 * Syntax:<BR>
 * <TT>
 * &lt;time&gt; &lt;actionId&gt; &lt;msgId&gt; &lt;hostId&gt; 
 * [&lt;host2Id&gt; [&lt;size&gt;] [&lt;respSize&gt;]]
 * </TT>
 * </P>
 * <P>
 * All actions (except CONNECTION) must have first four fields. SEND, DELIVERED
 * and ABORT actions need host2Id field too (the host who the message is/was
 * being transferred to). CREATE action needs the additional size
 * (of the message) field and can have also size-of-the-response field if
 * a response to this message is requested.
 * </P>
 * <P>
 * CONNNECTION action is followed by the two hosts which connect (or
 * disconnect) to each other and then either "up" or "down" depending on whether
 * the connection was created or destroyed.
 * </P>
 * <P>
 * Message DROP and REMOVE events can use {@value #ALL_MESSAGES_ID} as the
 * message ID for referring to all messages the node has in message buffer
 * (i.e., to delete all messages).
 * </P>
 */
public class StandardEventsReader implements ExternalEventsReader {
	/** Identifier of message creation event ({@value}) */
	public static final String CREATE = "C";
	/** Identifier of message transfer start event ({@value}) */
	public static final String SEND = "S";
	/** Identifier of message delivered event ({@value}) */
	public static final String DELIVERED = "DE";
	/** Identifier of message transfer aborted event ({@value}) */
	public static final String ABORT = "A";
	/** Identifier of message dropped event ({@value}) */
	public static final String DROP = "DR";
	/** Identifier of message removed event ({@value}) */
	public static final String REMOVE = "R";
	/** Identifier of connection event ({@value}) */
	public static final String CONNECTION = "CONN";
	/** Value identifier of connection down event ({@value}) */
	public static final String CONNECTION_DOWN = "down";
	/** Value identifier of connection up event ({@value}) */
	public static final String CONNECTION_UP = "up";
	/** Message identifier to use to refer to all messages ({@value}) */
	public static final String ALL_MESSAGES_ID = "*";

	private Scanner scanner;

	public StandardEventsReader(File eventsFile) {
		try {
			this.scanner = new Scanner(eventsFile);
		} catch (FileNotFoundException e) {
			throw new SimError(e.getMessage(), e);
		}
	}

	public List<ExternalEvent> readEvents(int nrof) {
		ArrayList<ExternalEvent> events = new ArrayList<ExternalEvent>(nrof);
		int eventsRead = 0;
		// skip empty and comment lines
		Pattern skipPattern = Pattern.compile("(#.*)|(^\\s*$)");

		while (eventsRead < nrof && scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (skipPattern.matcher(line).matches()) {
				// skip empty and comment lines
				continue;
			}

			double time;
			String action;
			String msgId;
			int hostAddr;
			int host2Addr;

			try (Scanner lineScan = new Scanner(line)) {
				time = lineScan.nextDouble();
				action = lineScan.next();

				if (action.equals(DROP)) {
					msgId = lineScan.next();
					hostAddr = getHostAddress(lineScan.next());
					events.add(new MessageDeleteEvent(hostAddr, msgId,
							time, true));
				} else if (action.equals(REMOVE)) {
					msgId = lineScan.next();
					hostAddr = getHostAddress(lineScan.next());
					events.add(new MessageDeleteEvent(hostAddr, msgId,
							time, false));
				} else if (action.equals(CONNECTION)) {
					String connEventType;
					boolean isUp;
					hostAddr = getHostAddress(lineScan.next());
					host2Addr = getHostAddress(lineScan.next());
					connEventType = lineScan.next();

					String interfaceId = null;
					if (lineScan.hasNext()) {
						interfaceId = lineScan.next();
					}

					if (connEventType.equalsIgnoreCase(CONNECTION_UP)) {
						isUp = true;
					} else if (connEventType.equalsIgnoreCase(CONNECTION_DOWN)) {
						isUp = false;
					} else {
						throw new SimError("Unknown up/down value '" +
								connEventType + "'");
					}

					ConnectionEvent ce = new ConnectionEvent(hostAddr,
							host2Addr, interfaceId, isUp, time);

					events.add(ce);
				} else {
					msgId = lineScan.next();
					hostAddr = getHostAddress(lineScan.next());

					host2Addr = getHostAddress(lineScan.next());

					if (action.equals(CREATE)) {
						int size = lineScan.nextInt();
						int respSize = 0;
						if (lineScan.hasNextInt()) {
							respSize = lineScan.nextInt();
						}
						events.add(new MessageCreateEvent(hostAddr, host2Addr,
								msgId, size, respSize, time));
					} else {
						int stage = -1;
						if (action.equals(SEND)) {
							stage = MessageRelayEvent.SENDING;
						} else if (action.equals(DELIVERED)) {
							stage = MessageRelayEvent.TRANSFERRED;
						} else if (action.equals(ABORT)) {
							stage = MessageRelayEvent.ABORTED;
						} else {
							throw new SimError("Unknown action '" + action +
									"' in external events");
						}
						events.add(new MessageRelayEvent(hostAddr, host2Addr,
								msgId, time, stage));
					}
				}
				// discard the newline in the end
				if (lineScan.hasNextLine()) {
					lineScan.nextLine(); // TODO: test
				}
				eventsRead++;
			} catch (Exception e) {
				throw new SimError("Can't parse external event " +
						(eventsRead + 1) + " from '" + line + "'", e);
			}
		}

		return events;
	}

	/**
	 * Parses a host address from a hostId string (the numeric part after
	 * optional non-numeric part).
	 * 
	 * @param hostId The id to parse the address from
	 * @return The address
	 * @throws SimError if no address could be parsed from the id
	 */
	private int getHostAddress(String hostId) {
		String addressPart = "";
		if (hostId.matches("^\\d+$")) {
			addressPart = hostId; // host id is only the address
		} else if (hostId.matches("^\\D+\\d+$")) {
			String[] parts = hostId.split("\\D");
			addressPart = parts[parts.length - 1]; // last occurence is the addr
		} else {
			throw new SimError("Invalid host ID '" + hostId + "'");
		}

		return Integer.parseInt(addressPart);
	}

	public void close() {
		this.scanner.close();
	}

}
