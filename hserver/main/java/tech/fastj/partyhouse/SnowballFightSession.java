package tech.fastj.partyhouse;

import tech.fastj.network.rpc.ServerClient;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;
import tech.fastj.network.sessions.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.fastj.partyhousecore.*;

public class SnowballFightSession extends Session {

    private static final Logger SnowballFightSessionLogger = LoggerFactory.getLogger(SnowballFightSession.class);

    private final Map<UUID, PositionState> clientPositions;
    private final Map<UUID, PointsState> clientPoints;
    private ScheduledExecutorService survivorPoints;

    private boolean isGameRunning;

    public SnowballFightSession(GameLobby lobby) {
        super(lobby, SessionNames.SnowballFight, new ArrayList<>());
        clientPositions = new HashMap<>();
        clientPoints = new HashMap<>();

        setOnClientJoin(this::addNewClientStates);
        setOnClientLeave(this::removeClientStates);
        addCommand(Commands.UpdateClientGameState, ClientInfo.class, ClientPosition.class, ClientVelocity.class, this::updatePositionState);
        addCommand(Commands.SnowballThrow, SnowballInfo.class, this::notifySnowballThrow);
        addCommand(Commands.SnowballHit, ClientInfo.class, SnowballInfo.class, this::notifySnowballHit);
    }

    private void notifySnowballThrow(ServerClient client, SnowballInfo snowballInfo) {
        SnowballFightSessionLogger.info(
            "Telling {} clients {} has thrown a snowball",
            getClients().size(),
            snowballInfo.clientInfo().clientName()
        );

        for (ServerClient serverClient : getClients()) {
            if (serverClient.getClientId().equals(snowballInfo.clientInfo().clientId())) {
                continue;
            }

            try {
                SnowballFightSessionLogger.debug(
                    "Telling {} that {} threw a snowball",
                    clientPositions.get(serverClient.getClientId()).getClientInfo().clientName(),
                    snowballInfo.clientInfo().clientName()
                );
                serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.SnowballThrow, snowballInfo);
            } catch (IOException exception) {
                SnowballFightSessionLogger.warn("error while trying to send snowball throw update to {}: {}", serverClient.getClientId(), exception);
            }
        }
    }

    private void notifySnowballHit(ServerClient client, ClientInfo clientHit, SnowballInfo snowballInfo) {
        SnowballFightSessionLogger.info(
            "Telling {} clients {} has been hit by {}'s snowball",
            getClients().size(),
            clientHit.clientName(),
            snowballInfo.clientInfo().clientName()
        );

        clientPoints.get(snowballInfo.clientInfo().clientId()).modifyPoints(Points.SuccessfulSnowballHit);
        clientPoints.get(clientHit.clientId()).modifyPoints(Points.HitBySnowball);

        clientPositions.get(clientHit.clientId()).setPlayerDead(true);

        for (ServerClient serverClient : getClients()) {
            if (serverClient.getClientId().equals(snowballInfo.clientInfo().clientId())) {
                continue;
            }

            try {
                SnowballFightSessionLogger.debug(
                    "Telling {} that {} has been hit by {}'s snowball",
                    clientPositions.get(serverClient.getClientId()).getClientInfo().clientName(),
                    clientHit.clientName(),
                    snowballInfo.clientInfo().clientName()
                );
                serverClient.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.SnowballThrow, snowballInfo);
            } catch (IOException exception) {
                SnowballFightSessionLogger.warn("error while trying to send snowball hit update to {}: {}", serverClient.getClientId(), exception);
            }
        }

        checkForWinner();
    }

    private void addNewClientStates(Session session, ServerClient client) {
        GameLobby lobby = (GameLobby) this.lobby;
        ClientInfo clientInfo = lobby.getClientInfo(client);

        PositionState newGameState = new PositionState();
        newGameState.setClientInfo(clientInfo);
        newGameState.setClientPosition(new ClientPosition(640f, 360f));
        newGameState.setClientVelocity(new ClientVelocity());

        clientPositions.put(client.getClientId(), newGameState);

        PointsState newPointsState = new PointsState();
        newPointsState.setClientInfo(clientInfo);
        clientPoints.put(client.getClientId(), newPointsState);

        SnowballFightSessionLogger.info("{} to notify from session about client game state adding", getClients().size());

        for (ServerClient serverClient : getClients()) {
            try {
                SnowballFightSessionLogger.info("sending new game state from {} to {}", client.getClientId(), serverClient.getClientId());

                serverClient.sendCommand(
                    NetworkType.TCP, CommandTarget.Client, Commands.UpdateClientGameState,
                    newGameState.getClientInfo(), newGameState.getClientPosition(), newGameState.getClientVelocity()
                );
            } catch (IOException exception) {
                SnowballFightSessionLogger.warn("Error while trying to send client game state to {}: {}", serverClient.getClientId(), exception);
            }

            try {
                SnowballFightSessionLogger.info("sending existing game state from {} to {}", serverClient.getClientId(), client.getClientId());

                PositionState existingGameState = clientPositions.get(serverClient.getClientId());
                client.sendCommand(
                    NetworkType.TCP, CommandTarget.Client, Commands.UpdateClientGameState,
                    existingGameState.getClientInfo(), existingGameState.getClientPosition(), existingGameState.getClientVelocity()
                );
            } catch (IOException exception) {
                SnowballFightSessionLogger.warn("Error while trying to send client game state to {}: {}", client.getClientId(), exception);
            }
        }
    }

    private void updatePositionState(ServerClient client, ClientInfo info, ClientPosition position, ClientVelocity velocity) {
        SnowballFightSessionLogger.debug("Telling {} clients {} has moved to: {}, {}", getClients().size(), info.clientName(), position.x(), position.y());

        PositionState positionState = clientPositions.get(info.clientId());
        positionState.setClientInfo(info);
        positionState.setClientPosition(position);
        positionState.setClientVelocity(velocity);

        for (ServerClient serverClient : getClients()) {
            if (client.getClientId().equals(serverClient.getClientId())) {
                SnowballFightSessionLogger.debug(
                    "Don't tell {} that {} moved",
                    clientPositions.get(serverClient.getClientId()).getClientInfo().clientName(),
                    clientPositions.get(client.getClientId()).getClientInfo().clientName()
                );
                continue;
            }

            try {
                SnowballFightSessionLogger.debug(
                    "Telling {} that {} moved",
                    clientPositions.get(serverClient.getClientId()).getClientInfo().clientName(),
                    clientPositions.get(client.getClientId()).getClientInfo().clientName()
                );
                serverClient.sendCommand(NetworkType.UDP, CommandTarget.Client, Commands.UpdateClientGameState, info, position, velocity);
            } catch (IOException exception) {
                SnowballFightSessionLogger.warn("error while trying to send {}'s game state update: {}", serverClient.getClientId(), exception);
            }
        }
    }

    public void startGame() {
        SnowballFightSessionLogger.info("Start Snowball Fight!");

        isGameRunning = true;

        if (survivorPoints == null) {
            survivorPoints = Executors.newSingleThreadScheduledExecutor();
        }

        survivorPoints.scheduleAtFixedRate(this::awardSurvivorPoints, 1L, 1L, TimeUnit.SECONDS);
    }

    private void awardSurvivorPoints() {
        for (PositionState positionState : clientPositions.values()) {
            if (positionState.isPlayerDead()) {
                continue;
            }

            PointsState pointsState = clientPoints.get(positionState.getClientInfo().clientId());

            if (pointsState == null) {
                SnowballFightSessionLogger.warn(
                    "Cannot award points to {} because they have no points state.",
                    positionState.getClientInfo().clientName()
                );

                return;
            }

            pointsState.modifyPoints(Points.SnowSurvive);
        }
    }

    private synchronized void checkForWinner() {
        if (!isGameRunning) {
            return;
        }

        List<ClientInfo> playersRemaining = new ArrayList<>();

        for (PositionState positionState : clientPositions.values()) {
            if (positionState.isPlayerDead()) {
                continue;
            }

            playersRemaining.add(positionState.getClientInfo());
        }

        if (playersRemaining.size() != 1) {
            return;
        }

        isGameRunning = false;

        ClientInfo winnerInfo = playersRemaining.get(0);
        SnowballFightSessionLogger.info("Somehow, {} has won!", winnerInfo.clientName());

        try {
            startSessionSequence(() -> {
                for (ServerClient client : getClients()) {
                    client.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.GameFinished, winnerInfo);
                }

                ((GameLobby) lobby).updateTotalPoints(clientPoints);

                TimeUnit.SECONDS.sleep(1L);

                for (ServerClient client : getClients()) {
                    var allPoints = ((GameLobby) lobby).getTotalPoints();
                    PointsState pointsState = allPoints.get(client.getClientId());
                    if (pointsState == null) {
                        System.out.println("fuck this shit I'm out");
                        pointsState = new PointsState();
                        pointsState.setClientInfo(clientPoints.get(client.getClientId()).getClientInfo());
                    }

                    System.out.println("send to " + clientPoints.get(client.getClientId()).getClientInfo().clientName());

                    client.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.GameResults, pointsState.createClientPoints());
                }

                TimeUnit.SECONDS.sleep(Info.SessionSwitchTime);

                lobby.switchCurrentSession(SessionNames.Home);

                for (int i = getClients().size() - 1; i >= 0; i--) {
                    ServerClient client = getClients().get(i);

                    SnowballFightSessionLogger.info(
                        "Telling client {}:{} to switch scenes",
                        client.getClientId(),
                        clientPositions.get(client.getClientId()).getClientInfo().clientName()
                    );

                    client.sendCommand(NetworkType.TCP, CommandTarget.Client, Commands.SwitchScene, SessionNames.Home);
                }

                TimeUnit.SECONDS.sleep(1L);

                for (int i = getClients().size() - 1; i >= 0; i--) {
                    ServerClient client = getClients().get(i);

                    SnowballFightSessionLogger.info(
                        "Moving client {}:{}",
                        client.getClientId(),
                        clientPositions.get(client.getClientId()).getClientInfo().clientName()
                    );

                    clientLeave(client);
                    lobby.getCurrentSession().clientJoin(client);
                }

                return 0;
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeClientStates(Session session, ServerClient client) {
        clientPositions.remove(client.getClientId());
        clientPoints.remove(client.getClientId());
    }

    @Override
    public Logger getLogger() {
        return SnowballFightSessionLogger;
    }
}
