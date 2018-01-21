import com.maschel.roomba.RoombaJSSCServerSerial;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class RoombaJSSCServer {

    private final static Logger LOGGER = Logger.getLogger(RoombaJSSCServer.class);

    public static void main(String[] args) {
        try {

            final RoombaJSSCServerSerial roomba = new RoombaJSSCServerSerial();
            final ServerSocket serverSocket = new ServerSocket(13950);

            while (true) {
                LOGGER.debug("Server entered main loop.");
                Socket socket;
                Scanner scanner;
                PrintWriter printWriter;

                while (true) {
                    LOGGER.debug("Server entered discovery loop.");

                    socket = serverSocket.accept();
                    scanner = new Scanner(socket.getInputStream());
                    printWriter = new PrintWriter(socket.getOutputStream(), true);

                    if (scanner.hasNextLine()) {
                        String message = scanner.nextLine();

                        // Potential client requesting serial port information.
                        if (message.equalsIgnoreCase("PORTS")) {
                            LOGGER.debug("Server received PORTS message.");

                            printWriter.println("OK PORTS");
                            String[] ports = roomba.portList();
                            printWriter.println(ports.length);
                            if (ports.length != 0) {
                                for (String s : ports) {
                                    printWriter.println(s);
                                }
                            } else {
                                LOGGER.info("No ports detected!");
                            }

                            // Client requesting to connect with server.
                        } else if (message.equalsIgnoreCase("CONNECT")) {
                            LOGGER.debug("Server received CONNECT message.");
                            String device = scanner.nextLine();
			                roomba.connect(device);
                            printWriter.println("CONNECTED " + device);
                            break;

                            // Invalid message received.
                        } else {
                            LOGGER.warn("Server received an invalid message.");
                            LOGGER.warn(message);
                        }
                    }
                }

                while (true) {
                    LOGGER.debug("Server entered communications loop.");

                    if (scanner.hasNextLine()) {
                        String message = scanner.nextLine();

                        if (message.equalsIgnoreCase("SEND BYTES")) {
                            LOGGER.debug("Server received SEND BYTES message.");

                            // Redirect bytes to roomba.
                            int length = Integer.parseInt(scanner.nextLine());
                            byte[] bytes = new byte[length];
                            for (int i = 0; i < length; i++) {
                                bytes[i] = Byte.parseByte(scanner.nextLine());
                            }
                            roomba.send(bytes);

                            // Check for sensor data.
                            if (roomba.hasSensorData()) {

                                // Send sensor data.
                                byte[] sensorData = roomba.getSensorData();
                                printWriter.println("SEND BYTES");
                                printWriter.println(sensorData.length);
                                for (byte b : sensorData) {
                                    printWriter.println(b);
                                }
                            }

                            printWriter.println("DONE");

                        } else if (message.equalsIgnoreCase("SEND INT")) {
                            LOGGER.debug("Server received SEND INT message.");

                            int value = Integer.parseInt(scanner.nextLine());
                            roomba.send(value);
                            printWriter.println("DONE");

                        } else if (message.equalsIgnoreCase("DISCONNECT")) {
                            LOGGER.debug("Server received DISCONNECT message.");

                            printWriter.println("OKAY");
                            socket.close();
                            break;

                        } else {
                            LOGGER.warn("Server received an invalid message.");
                            LOGGER.warn(message);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
