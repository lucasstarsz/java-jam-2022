package unittest;

import tech.fastj.network.config.ClientConfig;
import tech.fastj.network.config.ServerConfig;
import tech.fastj.network.rpc.Client;
import tech.fastj.network.rpc.Server;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import mock.ChatMessage;
import mock.GameState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ConnectionTests {

    private static Server server;
    private static final InetAddress ClientTargetAddress;
    private static final Logger ConnectionTestsLogger = LoggerFactory.getLogger(ConnectionTests.class);

    static {
        try {
            ClientTargetAddress = InetAddress.getByName("partyhouse.lucasz.tech");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int Port = 19999;

    @BeforeAll
    static void startServer() throws IOException {
        ServerConfig serverConfig = new ServerConfig(Port);

        server = new Server(serverConfig, null);
        server.start();
        server.allowClients();
    }

    @AfterEach
    void cleanServer() {
        server.disconnectAllClients();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @Test
    void checkConnectClientToServer() {
        assertDoesNotThrow(() -> {
            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client = new Client(clientConfig);

            client.connect();
        });
    }

    @Test
    void checkSendDataToServer_usingCommandsWithZeroMessages() throws InterruptedException {
        AtomicBoolean receivedTCPData = new AtomicBoolean();
        AtomicBoolean receivedUDPData = new AtomicBoolean();

        CountDownLatch latch = new CountDownLatch(2);

        assertDoesNotThrow(() -> {
            Command.Id receiveTCPChatMessage = Command.named("Receive TCP Chat Message");
            Command.Id receiveUDPChatMessage = Command.named("Receive UDP Chat Message");

            server.addCommand(receiveTCPChatMessage, (client) -> {
                receivedTCPData.set(true);
                latch.countDown();
            });

            server.addCommand(receiveUDPChatMessage, (client) -> {
                receivedUDPData.set(true);
                latch.countDown();
            });

            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client = new Client(clientConfig);
            client.connect();
            client.getSerializer().registerSerializer(ChatMessage.class);
            client.sendCommand(NetworkType.TCP, CommandTarget.Server, receiveTCPChatMessage);

            while (!receivedUDPData.get()) {
                client.sendCommand(NetworkType.UDP, CommandTarget.Server, receiveUDPChatMessage);
                TimeUnit.MILLISECONDS.sleep(100L);
            }
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail(
                "Server did not receive both TCP and UDP properly.\n" +
                    "TCP Received: " + receivedTCPData.get() + ", UDP received: " + receivedUDPData.get()
            );
        }
    }

    @Test
    void checkSendDataToServer_usingCommandsWithOneMessage() throws InterruptedException {
        ChatMessage tcpData = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());
        ChatMessage udpData = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());

        AtomicBoolean receivedTCPData = new AtomicBoolean();
        AtomicBoolean receivedUDPData = new AtomicBoolean();

        CountDownLatch latch = new CountDownLatch(2);

        assertDoesNotThrow(() -> {
            Command.Id receiveTCPChatMessage = Command.named("Receive TCP Chat Message");
            Command.Id receiveUDPChatMessage = Command.named("Receive UDP Chat Message");

            server.addCommand(receiveTCPChatMessage, ChatMessage.class, (client, chatMessage) -> {
                assertEquals(tcpData, chatMessage, "The TCP data should match.");
                receivedTCPData.set(true);
                latch.countDown();
            });

            server.addCommand(receiveUDPChatMessage, ChatMessage.class, (client, chatMessage) -> {
                assertEquals(udpData, chatMessage, "The UDP data should match.");
                receivedUDPData.set(true);
                latch.countDown();
            });

            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client = new Client(clientConfig);
            client.connect();
            client.getSerializer().registerSerializer(ChatMessage.class);
            client.sendCommand(NetworkType.TCP, CommandTarget.Server, receiveTCPChatMessage, tcpData);

            while (!receivedUDPData.get()) {
                client.sendCommand(NetworkType.UDP, CommandTarget.Server, receiveUDPChatMessage, udpData);
                TimeUnit.MILLISECONDS.sleep(100L);
            }
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail(
                "Server did not receive both TCP and UDP properly.\n" +
                    "TCP Received: " + receivedTCPData.get() + ", UDP received: " + receivedUDPData.get()
            );
        }
    }

    @Test
    void checkSendDataToServer_usingCommandsWithMultipleMessages() throws InterruptedException {
        ChatMessage tcpData1 = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());
        ChatMessage tcpData2 = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());
        ChatMessage tcpData3 = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());
        ChatMessage udpData1 = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());
        ChatMessage udpData2 = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());
        ChatMessage udpData3 = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());

        AtomicBoolean receivedMultipleTCPData = new AtomicBoolean();
        AtomicBoolean receivedMultipleUDPData = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(2);

        assertDoesNotThrow(() -> {
            Command.Id receiveTCPMultipleChatMessage = Command.named("Receive TCP Multiple Chat Messages");
            Command.Id receiveUDPMultipleChatMessage = Command.named("Receive UDP Multiple Chat Messages");

            server.addCommand(receiveTCPMultipleChatMessage, ChatMessage.class, ChatMessage.class, ChatMessage.class,
                (client, chatMessage1, chatMessage2, chatMessage3) -> {
                    assertEquals(tcpData1, chatMessage1, "The TCP data should match for message 1.");
                    assertEquals(tcpData2, chatMessage2, "The TCP data should match for message 2.");
                    assertEquals(tcpData3, chatMessage3, "The TCP data should match for message 3.");
                    receivedMultipleTCPData.set(true);
                    latch.countDown();
                }
            );

            server.addCommand(receiveUDPMultipleChatMessage, ChatMessage.class, ChatMessage.class, ChatMessage.class,
                (client, chatMessage1, chatMessage2, chatMessage3) -> {
                    assertEquals(udpData1, chatMessage1, "The UDP data should match for message 1.");
                    assertEquals(udpData2, chatMessage2, "The UDP data should match for message 2.");
                    assertEquals(udpData3, chatMessage3, "The UDP data should match for message 3.");
                    receivedMultipleUDPData.set(true);
                    latch.countDown();
                }
            );

            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client = new Client(clientConfig);
            client.connect();
            client.getSerializer().registerSerializer(ChatMessage.class);
            client.sendCommand(NetworkType.TCP, CommandTarget.Server, receiveTCPMultipleChatMessage, tcpData1, tcpData2, tcpData3);

            while (!receivedMultipleUDPData.get()) {
                client.sendCommand(NetworkType.UDP, CommandTarget.Server, receiveUDPMultipleChatMessage, udpData1, udpData2, udpData3);
                TimeUnit.MILLISECONDS.sleep(100L);
            }
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail(
                "Server did not receive both TCP and UDP properly.\n" +
                    "TCP Received: " + receivedMultipleTCPData.get() + ", UDP received: " + receivedMultipleUDPData.get()
            );
        }
    }

    @Test
    void checkSendDataToServer_usingCommandsWithOneObject() throws InterruptedException {
        Random random = ThreadLocalRandom.current();
        GameState tcpData = GameState.values()[random.nextInt(0, GameState.values().length - 1)];
        UUID udpData = UUID.randomUUID();

        AtomicBoolean receivedTCPData = new AtomicBoolean();
        AtomicBoolean receivedUDPData = new AtomicBoolean();

        CountDownLatch latch = new CountDownLatch(2);

        assertDoesNotThrow(() -> {
            Command.Id receiveTCPGameState = Command.named("Receive TCP GameState Enum");
            Command.Id receiveUDPUuid = Command.named("Receive UDP UUID");

            server.addCommand(receiveTCPGameState, GameState.class, (client, gameState) -> {
                assertEquals(tcpData, gameState, "The TCP data should match.");
                receivedTCPData.set(true);
                latch.countDown();
            });

            server.addCommand(receiveUDPUuid, UUID.class, (client, uuid) -> {
                assertEquals(udpData, uuid, "The UDP data should match.");
                receivedUDPData.set(true);
                latch.countDown();
            });

            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client = new Client(clientConfig);
            client.connect();
            client.getSerializer().registerSerializer(ChatMessage.class);
            client.sendCommand(NetworkType.TCP, CommandTarget.Server, receiveTCPGameState, tcpData);

            while (!receivedUDPData.get()) {
                client.sendCommand(NetworkType.UDP, CommandTarget.Server, receiveUDPUuid, udpData);
                TimeUnit.MILLISECONDS.sleep(100L);
            }
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail(
                "Server did not receive both TCP and UDP properly.\n" +
                    "TCP Received: " + receivedTCPData.get() + ", UDP received: " + receivedUDPData.get()
            );
        }
    }

    @Test
    void checkSendDataToServer_usingCommandsWithMultipleObjects() throws InterruptedException {
        Random random = ThreadLocalRandom.current();
        boolean tcpData1 = random.nextBoolean();
        byte tcpData2 = (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
        short tcpData3 = (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
        byte[] tcpData4 = new byte[random.nextInt(10, 20)];
        int[] tcpData5 = random.ints(random.nextInt(10, 20), Integer.MIN_VALUE, Integer.MAX_VALUE).toArray();
        String tcpData6 = UUID.randomUUID().toString();

        random.nextBytes(tcpData4);

        int udpData1 = random.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE - 1);
        float udpData2 = random.nextFloat(Float.MIN_VALUE, Float.MAX_VALUE);
        double udpData3 = random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE);
        long udpData4 = random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
        float[] udpData5 = new float[random.nextInt(10, 20)];
        ChatMessage udpData6 = new ChatMessage(UUID.randomUUID().toString(), System.currentTimeMillis(), UUID.randomUUID().toString());

        for (int i = 0; i < udpData5.length; i++) {
            udpData5[i] = random.nextFloat(Float.MIN_VALUE, Float.MAX_VALUE);
        }

        AtomicBoolean receivedMultipleTCPData = new AtomicBoolean();
        AtomicBoolean receivedMultipleUDPData = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(2);

        assertDoesNotThrow(() -> {
            Command.Id receiveTCPMultipleValues = Command.named("Receive TCP Multiple Values");
            Command.Id receiveUDPMultipleValues = Command.named("Receive UDP Multiple Values");

            server.addCommand(receiveTCPMultipleValues,
                boolean.class, byte.class, short.class, byte[].class, int[].class, String.class,
                (client, bl, bt, sh, bytes, ints, string) -> {
                    assertEquals(tcpData1, bl, "The TCP data should match for value 1.");
                    assertEquals(tcpData2, bt, "The TCP data should match for value 2.");
                    assertEquals(tcpData3, sh, "The TCP data should match for value 3.");
                    assertArrayEquals(tcpData4, bytes, "The TCP data should match for value 4.");
                    assertArrayEquals(tcpData5, ints, "The TCP data should match for value 5.");
                    assertEquals(tcpData6, string, "The TCP data should match for value 6.");
                    receivedMultipleTCPData.set(true);
                    latch.countDown();
                }
            );

            server.addCommand(receiveUDPMultipleValues,
                int.class, float.class, double.class, long.class, float[].class, ChatMessage.class,
                (client, i, f, d, l, floats, chatMessage) -> {
                    assertEquals(udpData1, i, "The UDP data should match for value 1.");
                    assertEquals(udpData2, f, "The UDP data should match for value 2.");
                    assertEquals(udpData3, d, "The UDP data should match for value 3.");
                    assertEquals(udpData4, l, "The UDP data should match for value 4.");
                    assertArrayEquals(udpData5, floats, "The UDP data should match for value 5.");
                    assertEquals(udpData6, chatMessage, "The UDP data should match for value 6.");
                    receivedMultipleUDPData.set(true);
                    latch.countDown();
                }
            );

            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client = new Client(clientConfig);
            client.connect();
            client.getSerializer().registerSerializer(ChatMessage.class);
            client.sendCommand(NetworkType.TCP, CommandTarget.Server, receiveTCPMultipleValues, tcpData1, tcpData2, tcpData3, tcpData4, tcpData5, tcpData6);

            while (!receivedMultipleUDPData.get()) {
                client.sendCommand(NetworkType.UDP, CommandTarget.Server, receiveUDPMultipleValues, udpData1, udpData2, udpData3, udpData4, udpData5, udpData6);
                TimeUnit.MILLISECONDS.sleep(100L);
            }
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail(
                "Server did not receive both TCP and UDP properly.\n" +
                    "TCP Received: " + receivedMultipleTCPData.get() + ", UDP received: " + receivedMultipleUDPData.get()
            );
        }
    }

    @Test
    void checkPingsAndKeepAlives() throws InterruptedException {
        int countdownStart = 50;
        CountDownLatch latch = new CountDownLatch(countdownStart);
        AtomicReference<Client> client = new AtomicReference<>();

        List<Long> pings = new ArrayList<>();

        assertDoesNotThrow(() -> {
            ClientConfig clientConfig = new ClientConfig(Port);
            client.set(new Client(clientConfig));
            client.get().connect();

            client.get().onPingReceived((ping) -> {
                long pingMs = TimeUnit.MILLISECONDS.convert(ping, TimeUnit.NANOSECONDS);
                ConnectionTestsLogger.debug("Received ping: {}ms", pingMs);
                pings.add(pingMs);
                latch.countDown();
            });

            assertTrue(client.get().startPings(100L, TimeUnit.MILLISECONDS));
            assertTrue(client.get().isSendingPings());
            assertTrue(client.get().startKeepAlives(3L, TimeUnit.SECONDS));
            assertTrue(client.get().isSendingKeepAlives());
        });

        boolean pingsSuccess = latch.await(30L, TimeUnit.SECONDS);

        ConnectionTestsLogger.debug("Total pings received: {}", countdownStart - latch.getCount());
        ConnectionTestsLogger.debug("Average ping: {}ms", pings.stream().mapToLong(Long::longValue).average().orElseThrow());

        assertTrue(client.get().stopPings());
        assertTrue(client.get().stopKeepAlives());
        assertFalse(client.get().isSendingPings());
        assertFalse(client.get().isSendingKeepAlives());
        client.get().disconnect();

        assertTrue(pingsSuccess, "Should have received 500 pings");
    }

    @Test
    void checkUDPMessageIsReceivedFromCorrectClient() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        Command.Id messageReceiverTest = Command.named("order");

        assertDoesNotThrow(() -> {
            ClientConfig clientConfig = new ClientConfig(ClientTargetAddress, Port);
            Client client1 = new Client(clientConfig);
            Client client2 = new Client(clientConfig);

            server.addCommand(messageReceiverTest, client -> {
                ConnectionTestsLogger.debug("client {} received message test", client.getClientId());
                assertEquals(client1.getClientId(), client.getClientId());
                latch.countDown();
            });

            client1.connect();
            ConnectionTestsLogger.debug("client 1: {}", client1.getClientId());

            client2.connect();
            ConnectionTestsLogger.debug("client 2: {}", client2.getClientId());

            while (latch.getCount() > 0) {
                client1.sendCommand(NetworkType.UDP, CommandTarget.Server, messageReceiverTest);
                TimeUnit.MILLISECONDS.sleep(100L);
            }
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);

        if (!success) {
            fail("Server failed to create lobby");
        }
    }
}
