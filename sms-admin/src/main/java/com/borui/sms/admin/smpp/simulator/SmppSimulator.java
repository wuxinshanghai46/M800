package com.borui.sms.admin.smpp.simulator;

import org.jsmpp.PDUStringException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.*;
import org.jsmpp.util.MessageIDGenerator;
import org.jsmpp.util.MessageId;
import org.jsmpp.util.RandomMessageIDGenerator;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Embedded SMPP simulator for local integration testing.
 * - Accepts bind_transceiver with configurable systemId/password
 * - Receives submit_sm, returns message_id
 * - Sends deliver_sm (DLR) back after configurable delay
 * - Configurable delivery success rate
 *
 * Usage: java SmppSimulator [port] [systemId] [password] [dlrDelayMs] [deliveryRate%]
 */
public class SmppSimulator {

    private final int port;
    private final String systemId;
    private final String password;
    private final int dlrDelayMs;
    private final int deliveryRate; // percentage 0-100

    private final MessageIDGenerator msgIdGen = new RandomMessageIDGenerator();
    private final ExecutorService executor = Executors.newCachedThreadPool(
            r -> { Thread t = new Thread(r, "smpp-sim"); t.setDaemon(true); return t; });
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger msgCount = new AtomicInteger(0);
    private final AtomicInteger dlrCount = new AtomicInteger(0);

    private SMPPServerSessionListener listener;

    public SmppSimulator(int port, String systemId, String password, int dlrDelayMs, int deliveryRate) {
        this.port = port;
        this.systemId = systemId;
        this.password = password;
        this.dlrDelayMs = dlrDelayMs;
        this.deliveryRate = deliveryRate;
    }

    public void start() throws IOException {
        listener = new SMPPServerSessionListener(port);
        listener.setTimeout(60000);

        System.out.println("==============================================");
        System.out.println("  SMPP Simulator started on port " + port);
        System.out.println("  SystemId: " + systemId);
        System.out.println("  Password: " + password);
        System.out.println("  DLR delay: " + dlrDelayMs + "ms");
        System.out.println("  Delivery rate: " + deliveryRate + "%");
        System.out.println("==============================================");

        // Accept connections in a loop
        executor.submit(() -> {
            while (running.get()) {
                try {
                    SMPPServerSession serverSession = listener.accept();
                    System.out.println("[SIM] New connection from client");
                    handleSession(serverSession);
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[SIM] Accept error: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void handleSession(SMPPServerSession session) {
        // Set message receiver to handle submit_sm
        session.setMessageReceiverListener(new ServerMessageReceiverListener() {

            @Override
            public SubmitSmResult onAcceptSubmitSm(SubmitSm submitSm, SMPPServerSession source)
                    throws ProcessRequestException {
                MessageId messageId = msgIdGen.newMessageId();
                int count = msgCount.incrementAndGet();

                String dest = submitSm.getDestAddress();
                byte[] shortMsg = submitSm.getShortMessage();
                String logContent = "";
                if (shortMsg != null && shortMsg.length > 0) {
                    logContent = new String(shortMsg);
                    if (logContent.length() > 50) logContent = logContent.substring(0, 50) + "...";
                }

                System.out.printf("[SIM] #%d submit_sm: to=%s, msgId=%s, content=%s%n",
                        count, dest, messageId, logContent);

                // Check if DLR was requested
                boolean dlrRequested = submitSm.getRegisteredDelivery() != 0;
                if (dlrRequested) {
                    executor.submit(() -> sendDlr(source, messageId, dest, submitSm.getSourceAddr()));
                }

                return new SubmitSmResult(messageId, new OptionalParameter[0]);
            }

            @Override
            public SubmitMultiResult onAcceptSubmitMulti(SubmitMulti submitMulti, SMPPServerSession source)
                    throws ProcessRequestException {
                MessageId messageId = msgIdGen.newMessageId();
                System.out.println("[SIM] submit_multi received, msgId=" + messageId);
                return new SubmitMultiResult(messageId.getValue(), new UnsuccessDelivery[0], new OptionalParameter[0]);
            }

            @Override
            public QuerySmResult onAcceptQuerySm(QuerySm querySm, SMPPServerSession source)
                    throws ProcessRequestException {
                System.out.println("[SIM] query_sm received");
                return null;
            }

            @Override
            public void onAcceptReplaceSm(ReplaceSm replaceSm, SMPPServerSession source)
                    throws ProcessRequestException {
                System.out.println("[SIM] replace_sm received");
            }

            @Override
            public void onAcceptCancelSm(CancelSm cancelSm, SMPPServerSession source)
                    throws ProcessRequestException {
                System.out.println("[SIM] cancel_sm received");
            }

            @Override
            public BroadcastSmResult onAcceptBroadcastSm(BroadcastSm broadcastSm, SMPPServerSession source)
                    throws ProcessRequestException {
                MessageId messageId = msgIdGen.newMessageId();
                return new BroadcastSmResult(messageId, new OptionalParameter[0]);
            }

            @Override
            public void onAcceptCancelBroadcastSm(CancelBroadcastSm cancelBroadcastSm, SMPPServerSession source)
                    throws ProcessRequestException {
            }

            @Override
            public QueryBroadcastSmResult onAcceptQueryBroadcastSm(QueryBroadcastSm queryBroadcastSm, SMPPServerSession source)
                    throws ProcessRequestException {
                MessageId messageId = msgIdGen.newMessageId();
                return new QueryBroadcastSmResult(messageId, new OptionalParameter[0]);
            }

            @Override
            public DataSmResult onAcceptDataSm(DataSm dataSm, Session source)
                    throws ProcessRequestException {
                System.out.println("[SIM] data_sm received");
                return null;
            }
        });

        // Wait for bind in background
        executor.submit(() -> {
            try {
                BindRequest bindReq = session.waitForBind(30000);
                System.out.printf("[SIM] Bind request: systemId=%s, type=%s%n",
                        bindReq.getSystemId(), bindReq.getBindType());

                if (systemId.equals(bindReq.getSystemId()) && password.equals(bindReq.getPassword())) {
                    bindReq.accept(systemId, InterfaceVersion.IF_34);
                    System.out.println("[SIM] Bind ACCEPTED for: " + bindReq.getSystemId());
                } else {
                    System.out.printf("[SIM] Bind REJECTED: expected %s/%s, got %s/%s%n",
                            systemId, password, bindReq.getSystemId(), bindReq.getPassword());
                    bindReq.reject(0x0000000E); // ESME_RINVPASWD
                }
            } catch (TimeoutException e) {
                System.err.println("[SIM] Bind timeout");
                session.close();
            } catch (PDUStringException | IOException e) {
                System.err.println("[SIM] Bind error: " + e.getMessage());
                session.close();
            }
        });
    }

    /**
     * Send DLR (deliver_sm) back to client after configured delay.
     */
    private void sendDlr(SMPPServerSession session, MessageId messageId, String dest, String source) {
        try {
            Thread.sleep(dlrDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (!session.getSessionState().isBound()) {
            System.out.println("[SIM] Cannot send DLR: session not bound");
            return;
        }

        boolean delivered = (Math.random() * 100) < deliveryRate;
        String stat = delivered ? "DELIVRD" : "UNDELIV";
        String dlrText = String.format(
                "id:%s sub:001 dlvrd:%s submit date:2603151200 done date:2603151201 stat:%s err:000 text:...",
                messageId, delivered ? "001" : "000", stat);

        try {
            session.deliverShortMessage(
                    "CMT",
                    TypeOfNumber.INTERNATIONAL,
                    NumberingPlanIndicator.ISDN,
                    dest,
                    TypeOfNumber.ALPHANUMERIC,
                    NumberingPlanIndicator.UNKNOWN,
                    source,
                    new ESMClass(MessageMode.DEFAULT, MessageType.SMSC_DEL_RECEIPT, GSMSpecificFeature.DEFAULT),
                    (byte) 0,
                    (byte) 0,
                    new RegisteredDelivery(0),
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    dlrText.getBytes()
            );
            int count = dlrCount.incrementAndGet();
            System.out.printf("[SIM] #%d DLR sent: msgId=%s, stat=%s%n", count, messageId, stat);
        } catch (Exception e) {
            System.err.println("[SIM] DLR send error: " + e.getMessage());
        }
    }

    public void stop() {
        running.set(false);
        try {
            if (listener != null) listener.close();
        } catch (IOException e) {
            // ignore
        }
        executor.shutdown();
        System.out.println("[SIM] Stopped. Messages: " + msgCount.get() + ", DLRs: " + dlrCount.get());
    }

    /**
     * Standalone entry point.
     * Args: [port] [systemId] [password] [dlrDelayMs] [deliveryRate%]
     */
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 2775;
        String sysId = args.length > 1 ? args[1] : "testsim";
        String pwd = args.length > 2 ? args[2] : "testpwd";
        int dlrDelay = args.length > 3 ? Integer.parseInt(args[3]) : 500;
        int delivRate = args.length > 4 ? Integer.parseInt(args[4]) : 85;

        SmppSimulator sim = new SmppSimulator(port, sysId, pwd, dlrDelay, delivRate);
        sim.start();

        // Keep running until process is killed
        Runtime.getRuntime().addShutdownHook(new Thread(sim::stop));
        System.out.println("\nSimulator running... kill process to stop.\n");
        Thread.currentThread().join(); // block forever
    }
}
