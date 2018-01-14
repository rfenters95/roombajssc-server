import com.maschel.roomba.RoombaJSSC;
import com.maschel.roomba.RoombaJSSCServerSerial;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class RoombaJSSCServer {

    public static void main(String[] args) {
        try {
            RoombaJSSC roomba = new RoombaJSSCServerSerial();
            ServerSocket serverSocket = new ServerSocket(13950);

            // Handle marco polo and connection before comm loop
            while (true) {
                System.out.println("Server entered main loop");

                Socket socket;
                Scanner scanner;
                PrintWriter printWriter;

                while (true) {
                    System.out.println("Server entered ping loop");

                    socket = serverSocket.accept();
                    scanner = new Scanner(socket.getInputStream());
                    printWriter = new PrintWriter(socket.getOutputStream(), true);

                    if (scanner.hasNextLine()) {
                        String message = scanner.nextLine();
                        if (message.equalsIgnoreCase("PORTS")) {
                            System.out.println("Received ports");
                            printWriter.println("OK PORTS");
                            String[] ports = roomba.portList();
                            printWriter.println(ports.length + 1);
                            for (String s : ports) {
                                printWriter.println(s);
                                System.out.println("Sent " + s);
                            }
                            // delete after dev ends
                            printWriter.println("DEBUG");
                            System.out.println("Sent DEBUG");
                        } else if (message.equalsIgnoreCase("CONNECT")) {
                            System.out.println("Received connect");
                            String device = scanner.nextLine();
                            printWriter.println("CONNECTED " + device);
                            break;
                        }
                    }
                }

                // Loop communication with client
                while (true) {
                    System.out.println("Entered comm loop");
                    if (scanner.hasNextLine()) {
                        String message = scanner.nextLine();
                        if (message.equalsIgnoreCase("SEND BYTES")) {
                            int length = Integer.parseInt(scanner.nextLine());
                            byte[] bytes = new byte[length];
                            for (int i = 0; i < length; i++) {
                                bytes[i] = Byte.parseByte(scanner.nextLine());
                            }
                            roomba.send(bytes);
                            RoombaJSSCServerSerial serverSerial = (RoombaJSSCServerSerial) roomba;
                            if (serverSerial.hasSensorData()) {
                                byte[] sensorData = serverSerial.getSensorData();
                                printWriter.println("SEND BYTES");
                                printWriter.println(sensorData.length);
                                for (byte b : sensorData) {
                                    printWriter.println(b);
                                }
                                System.out.println("Sent bytes");
                            }
                            System.out.println("Received bytes");
                            printWriter.println("DONE");
                        } else if (message.equalsIgnoreCase("SEND INT")) {
                            int value = Integer.parseInt(scanner.nextLine());
                            roomba.send(value);
                            System.out.println("Received int");
                            printWriter.println("DONE");
                        } else if (message.equalsIgnoreCase("DISCONNECT")) {
                            System.out.println("Disconnecting");
                            printWriter.println("OKAY");
                            socket.close();
                            break;
                        } else {
                            System.out.println("Error!");
                        }
                    }
                }

                System.out.println("Starting new main loop iteration");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
