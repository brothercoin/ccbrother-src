package com.hykj.ccbrother;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CcbrotherApplication {

	private static final Logger logger = LoggerFactory.getLogger(CcbrotherApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(CcbrotherApplication.class, args);
	}


//	@Value("${wss.server.host}")
//	private String host;
//
//	@Value("${wss.server.port}")
//	private Integer port;
//
//	@Bean
//	public SocketIOServer socketIOServer()
//	{
//		Configuration config = new Configuration();
//		config.setHostname(host);
//		config.setPort(port);
//
//		//该处可以用来进行身份验证
//		config.setAuthorizationListener(new AuthorizationListener() {
//			@Override
//			public boolean isAuthorized(HandshakeData data) {
//				logger.info("isAuthorized");
//				//http://localhost:8081?username=test&password=test
//				//例如果使用上面的链接进行connect，可以使用如下代码获取用户密码信息，本文不做身份验证
////              String username = data.getSingleUrlParam("username");
////              String password = data.getSingleUrlParam("password");
//				return true;
//			}
//		});
//
//		final SocketIOServer server = new SocketIOServer(config);
//		return server;
//	}
//
//	@Bean
//	public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketServer) {
//		return new SpringAnnotationScanner(socketServer);
//	}
}
