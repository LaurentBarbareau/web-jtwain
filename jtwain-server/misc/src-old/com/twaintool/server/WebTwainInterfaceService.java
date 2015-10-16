package com.twaintool.server;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.imageio.ImageIO;

import uk.co.mmscomputing.device.scanner.Scanner;
import uk.co.mmscomputing.device.scanner.ScannerIOException;
import uk.co.mmscomputing.device.scanner.ScannerIOMetadata;

public class WebTwainInterfaceService extends Thread {

	private static final Object CMD_ACQUIRE = "ACQUIRE";

	private static final Object CMD_SELECT = "SELECT";

	private static final Object CMD_CANCEL = "CANCEL";

	private static final String CMD_STOP = "STOP";

	private ServerSocket serverSocket;
	private Scanner scanner;

	public WebTwainInterfaceService(int port) throws IOException {

		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(0);
	}

	public static void main(String[] args) {

		int port = determinePort(args);
		try {
			Thread t = new WebTwainInterfaceService(port);
			t.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void update(ScannerIOMetadata.Type type, ScannerIOMetadata metadata) {

		if (type.equals(ScannerIOMetadata.ACQUIRED)) {

			BufferedImage image = metadata.getImage();

			System.out.println("Have an image now!");

			try {

				ImageIO.write(image, "jpg", new File("test.jpg"));

				// new
				// uk.co.mmscomputing.concurrent.Semaphore(0,true).tryAcquire(2000,null);

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (type.equals(ScannerIOMetadata.NEGOTIATE)) {

			// ScannerDevice device = metadata.getDevice();
			// try{
			// device.setResolution(100);
			//// device.setRegionOfInterest(0.0,0.0,40.0,50.0); // top-left
			// corner 40x50 mm
			// device.setRegionOfInterest(0,0,400,500); // top-left corner
			// 400x500 pixels
			// device.setShowUserInterface(false);
			// device.setShowProgressBar(false);
			// }catch(Exception e){
			// e.printStackTrace();
			// }

		} else if (type.equals(ScannerIOMetadata.STATECHANGE)) {

			System.err.println(metadata.getStateStr());

		} else if (type.equals(ScannerIOMetadata.EXCEPTION)) {

			metadata.getException().printStackTrace();
		}
	}

	public void run() {

		while (true) {

			try (Socket server = serverSocket.accept();
					DataInputStream in = new DataInputStream(server.getInputStream());
					DataOutputStream out = new DataOutputStream(server.getOutputStream())) {

				System.out.println("[S] Server running on port " + serverSocket.getLocalPort() + "... (SockTimeOut : "
						+ serverSocket.getSoTimeout() + ")");

				System.out.println("[S] Just connected to " + server.getRemoteSocketAddress());

				String clientCommand = in.readUTF();
				System.out.println("[S] " + clientCommand);

				if (CMD_ACQUIRE.equals(clientCommand)) {

					out.writeUTF("[ " + CMD_STOP + " ] command acknowledged - system is shutting down ! ]");
					acquire();
				} else if (CMD_SELECT.equals(clientCommand)) {

					out.writeUTF("[ " + CMD_STOP + " ] command acknowledged - system is shutting down ! ]");
					select();
				} else if (CMD_CANCEL.equals(clientCommand)) {

					out.writeUTF("[ " + CMD_STOP + " ] command acknowledged - system is shutting down ! ]");
					cancel();
				} else if (CMD_STOP.equals(clientCommand)) {

					out.writeUTF("[ " + CMD_STOP + " ] command acknowledged - system is shutting down ! ]");
					System.out.println("[S] Le serveur va s'arrêter... ");

					break;
				} else {
					System.out.println("Command : " + clientCommand);
				}

				out.writeUTF("[ Thank you for connecting to " + server.getLocalSocketAddress() + " - Goodbye ! ]");

			} catch (SocketTimeoutException e) {

				System.out.println("[S] Socket timed out!");
				break;

			} catch (IOException e) {

				e.printStackTrace();
				break;
			}
		}
	}

	private static int determinePort(String[] args) {
		return 10100; // Integer.parseInt(args[0]);
	}

	private void acquire() throws ScannerIOException {

		scanner.acquire();
	}

	private void select() throws ScannerIOException {

		scanner.select();
	}

	private void cancel() throws ScannerIOException {

		scanner.setCancel(true);
	}

}
