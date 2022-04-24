package com.example.grpc.client.grpcclient;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Arrays;

@RestController
public class PingPongEndpoint {

	GRPCClientService grpcClientService;

	@Autowired
	public PingPongEndpoint(GRPCClientService grpcClientService) {
		this.grpcClientService = grpcClientService;
	}

	@GetMapping("/ping")
	public String ping() {
		return grpcClientService.ping();
	}

	@GetMapping("/viewpage")
	public String view() {
		int[][] uploaded1Matrix = grpcClientService.extractMatrix(0);
		int[][] uploaded2Matrix = grpcClientService.extractMatrix(1);

		String result = grpcClientService.view(uploaded1Matrix);
		result = result + grpcClientService.view(uploaded2Matrix);
		return result;
	}

	@PostMapping("/mult")
	public String mult(@RequestParam("setDeadline") int setDeadline) {
		int[][] uploaded1Matrix = grpcClientService.extractMatrix(1);
		int[][] uploaded2Matrix = grpcClientService.extractMatrix(0);

		int whileIter = 0;
		String functionResults = "";
		int[][] resultMatrix = grpcClientService.mult(uploaded1Matrix, uploaded2Matrix, setDeadline);

		while (whileIter < resultMatrix.length) {
			functionResults = functionResults + Arrays.toString(resultMatrix[whileIter]) + "<br>";
			whileIter = whileIter + 1;
		}

		functionResults = functionResults.replace("[", "");
		functionResults = functionResults.replace("]", "");
		functionResults = functionResults.replace(",", "");

		return functionResults;
	}
}
