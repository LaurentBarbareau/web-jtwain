package com.twaintool.server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import uk.co.mmscomputing.device.twain.TwainScanner;

@SuppressWarnings("restriction")
public class WebTwainInterfaceService implements ScannerListener {
	
	private static final String					CMD_SCAN			= "/SCAN";
	
	private static final String					CMD_PICTURE			= "/PICTURE";
	
	private static final String					CMD_SELECT			= "/SELECT";
	private static final String					CMD_DRV_			= "/DRV_";
	
	private static final String					CMD_CANCEL			= "/CANCEL";
	
	private static final String					INDEX_PAGE		= "<!DOCTYPE html>"
																			+ "\n<html>"
																			+ "\n	<head>"
																			+ "\n		<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"
																			+ "\n		<title>MaelisWebDevice</title>"
																			+ "\n	</head>"
																			+ "\n	<body>"
																			+ "\n		<p>Test des périphériques :"
																			+ "\n		<ul>"
																			+ "\n			<li><a href=\"http://localhost:10100/SELECT\">SELECT</a></li>"
																			+ "\n			<li><a href=\"http://localhost:10100/SCAN\">SCAN</a></li>"
																			+ "\n			<li><a href=\"http://localhost:10100/PICTURE\">PICTURE</a></li>"
																			+ "\n			<li><a href=\"http://localhost:10100/CANCEL\">CANCEL</a></li>"
																			+ "\n		</ul>"
																			+ "\n		</p>"
																			+ "\n	</body>"
																			+ "\n</html>";
	
	private static final String					PAGE_BEGIN			= "<!DOCTYPE html>"
																			+ "\n<html>"
																			+ "\n	<head>"
																			+ "\n		<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"
																			+ "\n		<title>MaelisWebDevice</title>"
																			+ "\n	</head>"
																			+ "\n	<body>";
	
	private static final String					PAGE_END			= "	</body>"
																			+ "</html>";
	private static final String					LINK_HOME			= "<a href=\"#\" onclick=\"location.href='http://localhost:10100/';return false;\">Retour</a>";
	
	private static Scanner						scanner;
	
	private static final Map<String, String>	SCANNED_PICTURES	= new HashMap<>();
	
	public static void main (String[] args) throws IOException {
	
		WebTwainInterfaceService server = new WebTwainInterfaceService();
		server.init();
	}
	
	private void init () {
	
		initScanner();
		
		initHttpServer();
	}
	
	private void initHttpServer () {
	
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
			System.exit(-1);
		}
	}
	
	private void initScanner () {
	
		try {
			
			scanner = TwainScanner.getDevice();
			
			if (scanner != null) {
				scanner.addListener(this);
			} else {
				System.err.println("No scan device found.");
				System.exit(1);
			}
			
			// TwainSourceManager sourceManager = ((TwainScanner2) scanner).getSourceManager(); //
			// N'apporte pas grand chose comme info...
			
			String[] deviceNames = scanner.getDeviceNames();
			
			if (deviceNames.length == 0) {
				
				System.err.println("Aucun périphérique trouvé !");
				System.exit(-1);
			}
			
			for (String deviceName : deviceNames) {
				System.out.println("Device : " + deviceName);
			}
			
		} catch (Exception e) {
			System.err.println("Initialisation du Scanner impossible");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private static class MyHandler implements HttpHandler {
		
		/**
		 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
		 */
		public void handle (HttpExchange exchange) throws IOException {
		
			String requestMethod = exchange.getRequestMethod();
			
			if (requestMethod.equalsIgnoreCase("GET")) {
				
				OutputStream responseBody = exchange.getResponseBody();
				
				String command = extractCommand(exchange);
				
				Headers responseHeaders = exchange.getResponseHeaders();
				
				processCommand(command, responseHeaders, responseBody, exchange);
				
				responseBody.close();
			}
		}
		
		private void processCommand (String command, Headers responseHeaders, OutputStream responseBody, HttpExchange exchange)
				throws IOException {
		
			String response = "";
			
			System.out.println("Request command : " + command);
			
			try {
				if (command.equals(CMD_SCAN)) {
					
					scanner.acquire();
					
					addDefaultResponseHeader(responseHeaders);
					response = PAGE_BEGIN + "OK<br/>" + LINK_HOME + PAGE_END;
					
					exchange.sendResponseHeaders(200, 0);
					responseBody.write(response.getBytes());
					
				} else if (command.equals(CMD_SELECT)) {
					
					addDefaultResponseHeader(responseHeaders);
					response = PAGE_BEGIN + "<ul>";
					
					String[] deviceNames = scanner.getDeviceNames();
					
					if (deviceNames != null) {
						
						for (String deviceName : deviceNames) {
							
							response += "<li><a href='./DRV_" + URLEncoder.encode(deviceName, "UTF-8") + "'>" + deviceName
									+ "</a></li>";
						}
						
					} else {
						System.out.println("No scanner device found !!");
					}
					
					response += "</ul>";
					response += LINK_HOME + PAGE_END;
					
					exchange.sendResponseHeaders(200, 0);
					responseBody.write(response.getBytes());
					
				} else if (command.startsWith(CMD_DRV_)) {
					
					scanner.select(URLDecoder.decode(command.replace(CMD_DRV_, ""), "UTF-8"));
					
					addDefaultResponseHeader(responseHeaders);
					response = PAGE_BEGIN + "Drv OK" + LINK_HOME + PAGE_END;
					
					exchange.sendResponseHeaders(200, 0);
					responseBody.write(response.getBytes());
					
				} else if (CMD_PICTURE.equals(command)) {
					
					responseHeaders.set("Content-Type", "image/jpeg");
					
					exchange.sendResponseHeaders(200, 0);
					
					ImageIO.write(ImageIO.read(new File(SCANNED_PICTURES.get("id"))), "jpg", responseBody);
					
				} else if (command.equals(CMD_CANCEL)) {
					
					scanner.setCancel(true);
					
					addDefaultResponseHeader(responseHeaders);
					
					response = PAGE_BEGIN + "Cancel OK<br/>" + LINK_HOME + PAGE_END;
					
					exchange.sendResponseHeaders(200, 0);
					responseBody.write(response.getBytes());
					
				} else {
					
					addDefaultResponseHeader(responseHeaders);
					exchange.sendResponseHeaders(200, 0);
					responseBody.write(INDEX_PAGE.getBytes());
				}
				
			} catch (ScannerIOException e) {
				
				System.err.println("Erreur lors du scan");
				e.printStackTrace();
				
				addDefaultResponseHeader(responseHeaders);
				response = PAGE_BEGIN + "ERREUR" + PAGE_END;
				
				exchange.sendResponseHeaders(200, 0);
				responseBody.write(response.getBytes());
			}
		}

		public void addDefaultResponseHeader (Headers responseHeaders) {
		
			responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
		}
		
		private String extractCommand (HttpExchange exchange) {
		
			return exchange.getRequestURI().getPath();
		}
	}
	
	/**
	 * @see uk.co.mmscomputing.device.scanner.ScannerListener#update(uk.co.mmscomputing.device.scanner.ScannerIOMetadata.Type,
	 *      uk.co.mmscomputing.device.scanner.ScannerIOMetadata)
	 */
	@Override
	public void update (ScannerIOMetadata.Type type, ScannerIOMetadata metadata) {
	
		if (type.equals(ScannerIOMetadata.ACQUIRED)) {
			
			System.out.println("--ACQUIRE--");
			
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
			
			System.out.println("--NEGOTIATE--");
			// ScannerDevice device = metadata.getDevice();
			// try{
			// device.setResolution(100);
			// // device.setRegionOfInterest(0.0,0.0,40.0,50.0); // top-left
			// corner 40x50 mm
			// device.setRegionOfInterest(0,0,400,500); // top-left corner
			// 400x500 pixels
			// device.setShowUserInterface(false);
			// device.setShowProgressBar(false);
			// }catch(Exception e){
			// e.printStackTrace();
			// }
		} else if (type.equals(ScannerIOMetadata.STATECHANGE)) {
			System.out.println("--STATECHANGE--");
			System.out.println(metadata.getStateStr());
		} else if (type.equals(ScannerIOMetadata.EXCEPTION)) {
			System.out.println("--EXCEPTION--");
			metadata.getException().printStackTrace();
		}
	}
}
