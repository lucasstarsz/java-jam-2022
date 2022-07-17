package tech.fastj.network.rpc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import tech.fastj.network.client.Client;
import tech.fastj.network.rpc.classes.Classes;
import tech.fastj.network.rpc.classes.Classes0;
import tech.fastj.network.rpc.classes.Classes1;
import tech.fastj.network.rpc.classes.Classes2;
import tech.fastj.network.rpc.classes.Classes3;
import tech.fastj.network.rpc.classes.Classes4;
import tech.fastj.network.rpc.classes.Classes5;
import tech.fastj.network.rpc.classes.Classes6;
import tech.fastj.network.rpc.commands.Command;
import tech.fastj.network.rpc.commands.Command0;
import tech.fastj.network.rpc.commands.Command1;
import tech.fastj.network.rpc.commands.Command2;
import tech.fastj.network.rpc.commands.Command3;
import tech.fastj.network.rpc.commands.Command4;
import tech.fastj.network.rpc.commands.Command5;
import tech.fastj.network.rpc.commands.Command6;
import tech.fastj.network.serial.Networkable;
import tech.fastj.network.serial.Serializer;
import tech.fastj.network.serial.read.NetworkableInputStream;

public abstract class CommandHandler {

    private final Map<UUID, Command> commands;
    private final Map<UUID, Classes> idsToClasses;
    private final Map<String, UUID> aliasesToIds;

    private final Serializer serializer;

    protected CommandHandler() {
        commands = new HashMap<>();
        idsToClasses = new HashMap<>();
        aliasesToIds = new HashMap<>();
        serializer = new Serializer();
    }

    public void addCommand(String alias, Command0 command) {
        UUID id = aliasCheck(alias);
        commands.put(id, command);
        idsToClasses.put(id, new Classes0());
    }

    public <T1> void addCommand(String alias, Class<T1> class1, Command1<T1> command) {
        UUID id = aliasCheck(alias);
        commands.put(id, command);
        idsToClasses.put(id, new Classes1<>(class1));
        tryAddSerializer(class1);
    }

    public <T1, T2> void addCommand(String alias, Class<T1> class1, Class<T2> class2, Command2<T1, T2> command) {
        UUID id = aliasCheck(alias);
        commands.put(id, command);
        idsToClasses.put(id, new Classes2<>(class1, class2));
        tryAddSerializer(class1, class2);
    }

    public <T1, T2, T3> void addCommand(String alias, Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                        Command3<T1, T2, T3> command) {
        UUID id = aliasCheck(alias);
        commands.put(id, command);
        idsToClasses.put(id, new Classes3<>(class1, class2, class3));
        tryAddSerializer(class1, class2, class3);
    }

    public <T1, T2, T3, T4> void addCommand(String alias, Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                            Class<T4> class4, Command4<T1, T2, T3, T4> command) {
        UUID id = aliasCheck(alias);
        commands.put(id, command);
        idsToClasses.put(id, new Classes4<>(class1, class2, class3, class4));
        tryAddSerializer(class1, class2, class3, class4);
    }

    public <T1, T2, T3, T4, T5> void addCommand(String alias, Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                                Class<T4> class4, Class<T5> class5, Command5<T1, T2, T3, T4, T5> command) {
        UUID id = aliasCheck(alias);
        commands.put(id, command);
        idsToClasses.put(id, new Classes5<>(class1, class2, class3, class4, class5));
        tryAddSerializer(class1, class2, class3, class4, class5);
    }

    public <T1, T2, T3, T4, T5, T6> void addCommand(String alias, Class<T1> class1, Class<T2> class2, Class<T3> class3,
                                                    Class<T4> class4, Class<T5> class5, Class<T6> class6,
                                                    Command6<T1, T2, T3, T4, T5, T6> command) {
        UUID id = aliasCheck(alias);
        commands.put(id, command);
        idsToClasses.put(id, new Classes6<>(class1, class2, class3, class4, class5, class6));
        tryAddSerializer(class1, class2, class3, class4, class5, class6);
    }

    private UUID aliasCheck(String alias) {
        UUID id = aliasesToIds.get(alias);

        if (id == null) {
            throw new IllegalArgumentException(
                    "Command alias \"" + alias + "\" has not been registered. Please add the alias on the server side."
            );
        }

        return id;
    }

    @SuppressWarnings("unchecked")
    private void tryAddSerializer(Class<?>... possibleClasses) {
        for (Class<?> possibleClass : possibleClasses) {
            if (Networkable.class.isAssignableFrom(possibleClass)) {
                serializer.registerSerializer(UUID.randomUUID(), (Class<? extends Networkable>) possibleClass);
            }
        }
    }

    protected void readCommand(UUID commandId, NetworkableInputStream inputStream, Client client) throws IOException {
        Classes classes = getClasses(commandId);

        if (classes instanceof Classes0) {
            runCommand(commandId, client);
        } else if (classes instanceof Classes1<?> classes1) {
            Object o1 = readObject(classes1.t1(), inputStream);
            runCommand(commandId, client, o1);
        } else if (classes instanceof Classes2<?, ?> classes2) {
            Object o1 = readObject(classes2.t1(), inputStream);
            Object o2 = readObject(classes2.t2(), inputStream);
            runCommand(commandId, client, o1, o2);
        } else if (classes instanceof Classes3<?, ?, ?> classes3) {
            Object o1 = readObject(classes3.t1(), inputStream);
            Object o2 = readObject(classes3.t2(), inputStream);
            Object o3 = readObject(classes3.t3(), inputStream);
            runCommand(commandId, client, o1, o2, o3);
        } else if (classes instanceof Classes4<?, ?, ?, ?> classes4) {
            Object o1 = readObject(classes4.t1(), inputStream);
            Object o2 = readObject(classes4.t2(), inputStream);
            Object o3 = readObject(classes4.t3(), inputStream);
            Object o4 = readObject(classes4.t4(), inputStream);
            runCommand(commandId, client, o1, o2, o3, o4);
        } else if (classes instanceof Classes5<?, ?, ?, ?, ?> classes5) {
            Object o1 = readObject(classes5.t1(), inputStream);
            Object o2 = readObject(classes5.t2(), inputStream);
            Object o3 = readObject(classes5.t3(), inputStream);
            Object o4 = readObject(classes5.t4(), inputStream);
            Object o5 = readObject(classes5.t5(), inputStream);
            runCommand(commandId, client, o1, o2, o3, o4, o5);
        } else if (classes instanceof Classes6<?, ?, ?, ?, ?, ?> classes6) {
            Object o1 = readObject(classes6.t1(), inputStream);
            Object o2 = readObject(classes6.t2(), inputStream);
            Object o3 = readObject(classes6.t3(), inputStream);
            Object o4 = readObject(classes6.t4(), inputStream);
            Object o5 = readObject(classes6.t5(), inputStream);
            Object o6 = readObject(classes6.t6(), inputStream);
            runCommand(commandId, client, o1, o2, o3, o4, o5, o6);
        }
    }

    @SuppressWarnings("unchecked")
    private Object readObject(Class<?> objectClass, NetworkableInputStream inputStream) throws IOException {
        Object result;

        if (Networkable.class.isAssignableFrom(objectClass)) {
            result = serializer.readNetworkable(inputStream, (Class<? extends Networkable>) objectClass);
        } else {
            result = inputStream.readObject(objectClass);
        }

        return result;
    }

    protected void runCommand(UUID id, Client client) {
        Command0 command = (Command0) commands.get(id);
        command.runCommand(client);
    }

    @SuppressWarnings("unchecked")
    protected <T1> void runCommand(UUID id, Client client, T1 t1) {
        var command = (Command1<T1>) commands.get(id);
        command.runCommand(client, t1);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2> void runCommand(UUID id, Client client, T1 t1, T2 t2) {
        var command = (Command2<T1, T2>) commands.get(id);
        command.runCommand(client, t1, t2);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3> void runCommand(UUID id, Client client, T1 t1, T2 t2, T3 t3) {
        var command = (Command3<T1, T2, T3>) commands.get(id);
        command.runCommand(client, t1, t2, t3);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4> void runCommand(UUID id, Client client, T1 t1, T2 t2, T3 t3, T4 t4) {
        var command = (Command4<T1, T2, T3, T4>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5> void runCommand(UUID id, Client client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        var command = (Command5<T1, T2, T3, T4, T5>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5);
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5, T6> void runCommand(UUID id, Client client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        var command = (Command6<T1, T2, T3, T4, T5, T6>) commands.get(id);
        command.runCommand(client, t1, t2, t3, t4, t5, t6);
    }

    protected UUID getAliasId(String alias) {
        return aliasesToIds.get(alias);
    }

    @SuppressWarnings("unchecked")
    protected <T extends Classes> T getClasses(UUID id) {
        return (T) idsToClasses.get(id);
    }

    protected void assignAliasId(String alias, UUID aliasId) {
        aliasesToIds.put(alias, aliasId);
    }

    Map<String, UUID> getAliasesToIds() {
        return aliasesToIds;
    }

    void registerAliasId(String alias) {
        aliasesToIds.put(alias, UUID.randomUUID());
    }
}