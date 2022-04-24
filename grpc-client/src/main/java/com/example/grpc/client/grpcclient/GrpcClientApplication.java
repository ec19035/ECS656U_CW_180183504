package com.example.grpc.client.grpcclient;

import com.example.grpc.server.grpcserver.HelloRequest;
import com.example.grpc.server.grpcserver.HelloResponse;
import com.example.grpc.server.grpcserver.HelloServiceGrpc;

import com.example.grpc.client.grpcclient.storage.StorageProperties;
import com.example.grpc.client.grpcclient.storage.StorageService;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.devh.boot.grpc.client.inject.GrpcClient;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)

public class GrpcClientApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(GrpcClientApplication.class, args);
	}

	@Bean
	CommandLineRunner init(StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}

}
