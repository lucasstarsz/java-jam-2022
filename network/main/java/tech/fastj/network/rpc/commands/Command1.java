package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.Client;

@FunctionalInterface
public interface Command1<T1> extends Command {
    void runCommand(Client client, T1 t1);
}
