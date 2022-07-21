package tech.fastj.network.rpc.commands;

import tech.fastj.network.rpc.ClientBase;

@FunctionalInterface
public interface Command6<T extends ClientBase<?>, T1, T2, T3, T4, T5, T6> extends Command {
    void runCommand(T client, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6);
}
