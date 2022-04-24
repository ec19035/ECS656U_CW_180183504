package com.example.grpc.client.grpcclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.lang.Math;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.nio.file.Files;

import com.example.grpc.server.grpcserver.PingRequest;
import com.example.grpc.server.grpcserver.PongResponse;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import com.example.grpc.server.grpcserver.PingPongServiceGrpc;
import com.example.grpc.server.grpcserver.MatrixRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.example.grpc.client.grpcclient.storage.StorageService;
import com.example.grpc.server.grpcserver.MatrixReply;
import com.example.grpc.server.grpcserver.MatrixServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.google.common.util.concurrent.ListenableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
public class GRPCClientService {

	private final StorageService storageService;

	// Store internal IP addresses of servers for gRPC scaling creating channels and
	// stubs
	String[] internalIPAddresses = new String[] {
			"10.128.0.3",
			"10.128.0.4",
			"10.128.0.5",
			"10.128.0.6",
			"10.128.0.7",
			"10.128.0.8",
			"10.128.0.9",
			"10.128.0.10" };

	@Autowired
	public GRPCClientService(StorageService storageService) {
		this.storageService = storageService;
	}

	public String ping() {
		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
				.usePlaintext()
				.build();
		PingPongServiceGrpc.PingPongServiceBlockingStub stub = PingPongServiceGrpc.newBlockingStub(channel);
		PongResponse helloResponse = stub.ping(PingRequest.newBuilder()
				.setPing("")
				.build());
		channel.shutdown();
		return helloResponse.getPong();
	}

	public int[][] mult(int[][] uploaded1Matrix, int[][] uploaded2Matrix, int setDeadline) {
		int[][] firstMatrix = uploaded1Matrix;
		int[][] secondMatrix = uploaded2Matrix;

		// Creating two arraylists, one for storing the channels and the other for
		// storing the stubs
		List<ManagedChannel> allChannels = new ArrayList<>();
		List<MatrixServiceGrpc.MatrixServiceFutureStub> allStubs = new ArrayList<>();

		// while loop for creating and storing all channels using the internal IP
		// addresses defined earlier
		int whileIter = 0;
		while (whileIter < internalIPAddresses.length) {
			allChannels
					.add(ManagedChannelBuilder.forAddress(internalIPAddresses[whileIter], 9090).usePlaintext().build());
			whileIter = whileIter + 1;
		}

		// while loop for creating and storing all stubs using the channels just created
		whileIter = 0;
		while (whileIter < allChannels.size()) {
			allStubs.add(MatrixServiceGrpc.newFutureStub(allChannels.get(whileIter)));
			whileIter = whileIter + 1;
		}

		// initialise variables fr deadline footprinting and scaling
		CountDownLatch countdownLatch = new CountDownLatch(1);
		Random randomInt = new Random();
		long footPrint = 0;

		long timerStart = System.nanoTime();

		MatrixServiceGrpc.MatrixServiceFutureStub sltStub = allStubs.get(randomInt.nextInt(8));

		MatrixRequest mtxRequest = MatrixRequest.newBuilder()
				.setA00(firstMatrix[0][0])
				.setB00(secondMatrix[secondMatrix.length - 1][secondMatrix.length - 1])
				.build();
		ListenableFuture<MatrixReply> lstnblFuture = sltStub.multiplyBlock(mtxRequest);
		// Usage of matrixserviceFutureStub and ListenableFuture for asynchronous calls
		MatrixReply mtxReply = null;

		try {
			mtxReply = lstnblFuture.get();
			countdownLatch.countDown();
			// countdownLatch used to make sure function has finished as await is used later
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		try {
			countdownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long timerEnd = System.nanoTime();

		footPrint = timerEnd - timerStart;
		// footprint used to get how long it takes to operate on blocks of the matrices

		System.out.println("The Footprint is: " + footPrint);

		// get size of matrix and square it to get the block call number
		double blockCallNumber = (double) Math.pow(firstMatrix.length, 2);
		System.out.println("Number of Block that have been Called: " + blockCallNumber);

		System.out.println("The Deadline is: " + setDeadline);

		// Using the footprint and blockCallNumber obtained earlier, calculate the
		// execution time
		double executionTime = (footPrint * blockCallNumber) / 1000000000;
		System.out.println("The Execution Time is: " + executionTime);

		// divide execution time by the deadline set by the user to get the number of
		// servers
		int totalServerNumber = (int) (executionTime / setDeadline);

		// Set the number of server limits
		if (totalServerNumber < 1) {
			totalServerNumber = 1;
		}
		if (totalServerNumber > 8) {
			totalServerNumber = 8;
		}
		System.out.println("The total Number of Servers is: " + totalServerNumber);

		int idx = 0;

		int[][] rsltMatrix = new int[firstMatrix.length][firstMatrix.length];

		// Usage of for loop for multiplying matrix blocks and then adding the blocks
		for (int i = 0; i < firstMatrix.length; i++) { // row
			for (int j = 0; j < firstMatrix.length; j++) { // col
				for (int k = 0; k < firstMatrix.length; k++) {
					ListenableFuture<MatrixReply> lstnblFuture1 = allStubs.get(idx).multiplyBlock(
							MatrixRequest.newBuilder().setA00(firstMatrix[i][k]).setB00(secondMatrix[k][j]).build());

					MatrixReply mtxReply1 = null;
					try {
						mtxReply1 = lstnblFuture1.get();
						countdownLatch.countDown();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}

					if (idx == totalServerNumber - 1) {
						idx = 0;
					} else {
						idx = idx + 1;
					}

					ListenableFuture<MatrixReply> lstnblFuture2 = allStubs.get(idx).addBlock(
							MatrixRequest.newBuilder().setA00(rsltMatrix[i][j]).setB00(mtxReply1.getC00()).build());

					MatrixReply mtxReply2 = null;
					try {
						mtxReply2 = lstnblFuture2.get();
						countdownLatch.countDown();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}

					rsltMatrix[i][j] = mtxReply2.getC00();

					if (idx == totalServerNumber - 1) {
						idx = 0;
					} else {
						idx = idx + 1;
					}
				}
			}
		}

		// While loop for shutting down all channels once they have been used
		whileIter = 0;
		while (whileIter < allChannels.size()) {
			allChannels.get(whileIter).shutdown();
			whileIter = whileIter + 1;
		}

		return rsltMatrix;
	}

	// Matrix extraction function for obtaining the matrix from external file and
	// sotring it into a matrix array
	public int[][] extractMatrix(int idx) {
		Stream<Path> uploadAllMatrices = storageService.loadAll();
		Path[] files = uploadAllMatrices.toArray(Path[]::new);
		int iter = 0;
		try {
			List<String> matrixElements = Files.readAllLines(storageService.load(files[idx].toString()),
					StandardCharsets.US_ASCII);
			int[][] matrix = new int[matrixElements.size()][matrixElements.size()];
			while (iter < matrixElements.size()) {
				String[] stringMatrixRow = matrixElements.get(iter).split(" ");
				for (int i = 0; i < stringMatrixRow.length; i++) {
					matrix[iter][i] = Integer.parseInt(stringMatrixRow[i]);
				}
				iter = iter + 1;
			}
			return matrix;
		} catch (IOException e) {
			System.out.println("ERROR WHILE READING FILE");
			e.printStackTrace();
		}
		return null;
	}

	// Matrix view function for allowing visual of uploaded matrix
	public String view(int[][] matrix) {
		String result = "";
		int iter = 0;
		while (iter < matrix.length) {
			for (int i = 0; i < matrix[0].length; i++) {
				result = result + matrix[iter][i] + " ";
			}
			result = result + "<br>";
			iter = iter + 1;
		}
		result = result + "<br>";
		return result;
	}
}
