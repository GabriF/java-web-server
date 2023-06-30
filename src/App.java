import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class App {
    private static HashMap<String, String> configurazioneBase() {
        return new HashMap<String, String>() {
            {
                put("server", "jwb");
                put("log", "2");
            }
        };
    }

    private static HashMap<String, File> hostMapBase() {
        return new HashMap<String, File>() {
            {
                put("localhost:8080", new File(""));
                put("127.0.0.1:8080", new File("/home/gabrielef/Desktop/prova"));
            }
        };
    }
    
    public static void main(String[] args) {
        HashMap<String, String> configurazione = configurazioneBase();
        HashMap<String, File> hostMap = hostMapBase();

        ServerSocket server = null;

        try {
            int i = 0; // id dei thread

            server = new ServerSocket(8080, 0, InetAddress.getByName("127.0.0.1"));

            while (true) {
                i++;
                Socket client = server.accept();

                Server richiesta = new Server(client, configurazione, hostMap, i);
                Thread t = new Thread(richiesta, String.valueOf(i));
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
