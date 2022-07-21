module fastj.network {
    requires org.slf4j;
    exports tech.fastj.network.config;

    exports tech.fastj.network.rpc;
    exports tech.fastj.network.rpc.commands;
    exports tech.fastj.network.rpc.classes;

    exports tech.fastj.network.serial;
    exports tech.fastj.network.serial.util;
    exports tech.fastj.network.serial.read;
    exports tech.fastj.network.serial.write;
}