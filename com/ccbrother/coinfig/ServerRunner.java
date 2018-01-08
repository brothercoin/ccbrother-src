//package com.hykj.ccbrother.config;
//
//import com.hykj.ccbrother.contral.MessageEventHandler;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import com.corundumstudio.socketio.SocketIOServer;
//
//@Component
//public class ServerRunner implements CommandLineRunner {
//    private final SocketIOServer server;
//    private static final Logger logger = LoggerFactory.getLogger(CommandLineRunner.class);
//    @Autowired
//    public ServerRunner(SocketIOServer server) {
//        this.server = server;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        logger.info("run");
//        server.start();
//    }
//}