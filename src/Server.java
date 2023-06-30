import java.net.Socket;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;

public class Server implements Runnable {
    private Socket connessioneClient;
    private int id;
    private Map<String, File> hostMap;
    private Map<String, String> configurazione;

    public Server(Socket connessioneClient, Map<String, String> configurazione, Map<String, File> hostMap, int id) {
        this.connessioneClient = connessioneClient;
        this.configurazione = configurazione;
        this.hostMap = hostMap;
        this.id = id;
    }

    private static String leggiMessaggio(InputStream in) throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(in));

        String messaggio = "";
        String linea = bf.readLine();
        while (!linea.isEmpty()) {
            messaggio += linea + "\n";
            linea = bf.readLine();
        }

        return messaggio;
    }

    public void run() {
        String nomeServer = configurazione.get("server");
        int logLv = Integer.parseInt(configurazione.get("log"));

        try {
            String messaggio = null;
            try {
                messaggio = leggiMessaggio(connessioneClient.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                connessioneClient.close();
                return;
            }

            HttpI h = new HttpI(messaggio, connessioneClient, nomeServer, this.id, logLv, hostMap);
            h.eseguiRichiesta();

            connessioneClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}