package com.twaintool.server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import uk.co.mmscomputing.device.scanner.Scanner;
import uk.co.mmscomputing.device.scanner.ScannerIOException;
import uk.co.mmscomputing.device.scanner.ScannerIOMetadata;
import uk.co.mmscomputing.device.scanner.ScannerListener;

public class HttpServerDemo implements ScannerListener {

	private static final String CMD_SCAN = "/SCAN";

	private static final String CMD_PICTURE = "/PICTURE";

	private static final String CMD_SELECT = "/SELECT";
	private static final String CMD_DRV_ = "/DRV_";

	private static final String CMD_CANCEL = "/CANCEL";

	private static Scanner scanner;

	private static final Map<String, String> SCANNED_PICTURES = new HashMap<>();

	public static void main(String[] args) throws IOException {

		HttpServerDemo server = new HttpServerDemo();
		server.init();
	}

	private void init() {

		initScanner();

		initHttpServer();
	}

	private void initHttpServer() {

		try {
			int port = 10100;

			InetSocketAddress addr = new InetSocketAddress(port);

			HttpServer server = HttpServer.create(addr, 0);

			server.createContext("/", new MyHandler());
			server.setExecutor(Executors.newCachedThreadPool());
			server.start();

			System.out.println("Server is listening on port " + port);

		} catch (IOException e) {

			System.err.println("Initialisation du Serveur impossible");
			e.printStackTrace();
		}
	}

	private void initScanner() {
		try {
			scanner = Scanner.getDevice();
			scanner.addListener(this);

			for (String deviceName : scanner.getDeviceNames()) {
				System.out.println("Device : " + deviceName);
			}
		} catch (Exception e) {
			System.err.println("Initialisation du Scanner impossible");
			e.printStackTrace();
		}
	}

	private static class MyHandler implements HttpHandler {

		/**
		 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
		 */
		public void handle(HttpExchange exchange) throws IOException {

			String requestMethod = exchange.getRequestMethod();

			if (requestMethod.equalsIgnoreCase("GET")) {

				OutputStream responseBody = exchange.getResponseBody();

				String command = extractCommand(exchange);

				Headers responseHeaders = exchange.getResponseHeaders();

				processCommand(command, responseHeaders, responseBody, exchange);

				responseBody.close();
			}
		}

		private void processCommand(String command, Headers responseHeaders, OutputStream responseBody, HttpExchange exchange)
				throws IOException {

			String response = "";

			System.out.println("Request command : " + command);

			try {
				if (command.equals(CMD_SCAN)) {

					scanner.acquire();

					responseHeaders.set("Content-Type", "text/plain");
					response = "OK";
					exchange.sendResponseHeaders(200, 0);
					responseBody.write(response.getBytes());

				} else if (command.equals(CMD_SELECT)) {

					responseHeaders.set("Content-Type", "text/html");
					exchange.sendResponseHeaders(200, 0);
					response = "<html><head><title>Scan Drv select</title></head><body><ul>";

					for (String deviceName : scanner.getDeviceNames()) {

						response += "<li><a href='./DRV_" + URLEncoder.encode(deviceName, "UTF-8") + "'>" + deviceName
								+ "</a></li>";
					}

					response += "</ul></body></html>";
					responseBody.write(response.getBytes());

				} else if (command.startsWith(CMD_DRV_)) {

					scanner.select(URLDecoder.decode(command.replace(CMD_DRV_, ""), "UTF-8"));
					
					responseHeaders.set("Content-Type", "text/plain");
					response = "Drv OK";
					exchange.sendResponseHeaders(200, 0);
					responseBody.write(response.getBytes());

				} else if (CMD_PICTURE.equals(command)) {

					responseHeaders.set("Content-Type", "image/jpeg");

					exchange.sendResponseHeaders(200, 0);

					BufferedImage bi = ImageIO.read(new File(SCANNED_PICTURES.get("id")));

					ImageIO.write(bi, "jpg", responseBody);

				} else if (command.equals(CMD_CANCEL)) {

					scanner.setCancel(true);
					response = "Cancel OK";

				} else {
					
					responseHeaders.set("Content-Type", "text/plain");
					exchange.sendResponseHeaders(200, 0);
					response = "Commande '" + command + "' inconnue !";
					responseBody.write(response.getBytes());
				}

			} catch (ScannerIOException e) {

				response = "ERREUR";
				System.err.println("Erreur lors du scan");
				e.printStackTrace();
			}
		}

		private String extractCommand(HttpExchange exchange) {

			return exchange.getRequestURI().getPath();
		}

	}

	@Override
	public void update(ScannerIOMetadata.Type type, ScannerIOMetadata metadata) {

		System.out.println("Scanner exchange : " + type);

		if (type.equals(ScannerIOMetadata.ACQUIRED)) {

			BufferedImage image = metadata.getImage();

			System.out.println("Have an image now!");
			try {
				File output = new File("test.jpg");

				ImageIO.write(image, "jpg", output);

				SCANNED_PICTURES.put("id", output.getAbsolutePath());
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
}
