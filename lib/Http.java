import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

abstract public class Http {
    protected static enum Codici {
        HTTP_OK(200, "OK"),

        HTTP_BAD_REQUEST(400, "Bad Request"),
        HTTP_NOT_FOUND(404, "Not Found"),
        HTTP_UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

        HTTP_INTERNAL_ERROR(500, "Internal Server Error"),
        HTTP_NOT_IMPLEMENTED(501, "Not Implemented");

        private final int codice;
        private final String messaggio;

        private Codici(int codice, String messaggio) {
            this.codice = codice;
            this.messaggio = messaggio;
        }

        public int ottieniCodice() {
            return this.codice;
        }

        public String ottieniMessaggio() {
            return this.messaggio;
        }
    }

    protected final String VERSIONE = "HTTP/1.1";

    protected static final String CONTENT_TYPE_HTML = "text/html";

    protected static final String CRLF = "\r\n";

    protected String server; // Nome del server
    protected String richiesta;
    protected Socket connessione;
    protected int id; // Id da mostare durante il log
    protected int log = 0; // Livello di log
    protected Map<String, File> hostMapping;

    public Http(String richiesta, Socket connessione, String server, Map<String, File> hostMapping, int id, int log) {
        init(richiesta, connessione, server, hostMapping);

        this.id = id;
        this.log = log;

        log("ricevuta richiesta da " + connessione.getRemoteSocketAddress());
        log(richiesta, 2);
    }

    private void init(String richiesta, Socket connessione, String server, Map<String, File> hostMapping) {
        this.richiesta = richiesta;
        this.connessione = connessione;
        this.server = server;
        this.hostMapping = hostMapping;
    }

    protected final void log(String messaggio) {
        log(messaggio, 1);
    }

    protected final void log(String messaggio, int lv) {
        if (this.log >= lv) {
            System.out.println("[" + id + "]: " + messaggio);
        }
    }

    protected final void mandaRisposta(String messaggio) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(connessione.getOutputStream());
        } catch (IOException e) {
            log("errore durante l'invio del messaggio");
            chiudi();
            return;
        }

        pw.println(messaggio);
        pw.flush();
        pw.close();
    }

    protected void mandaRisposta(byte[] stato, byte[] header, byte[] body) {
        final byte[] NEW_LINE_BYTE = "\n".getBytes();
        final byte[] CRLF_BYTE = CRLF.getBytes();

        try {
            OutputStream out = connessione.getOutputStream();
            out.write(stato);
            out.write(NEW_LINE_BYTE);
            out.write(header);
            out.write(CRLF_BYTE);

            if (body != null)
                out.write(body);

            out.flush();
            out.close();
        } catch (IOException e) {
            log("Errore durante l'invio del messaggio:\n");
            chiudi();
            return;
        }
    }

    protected final void chiudi() {
        try {
            connessione.close();
            log("chiusura connessione");
        } catch (IOException e) {
            log("errore durante la chiusura della connessione");
            return;
        }
    }

    public void eseguiRichiesta() {
        log("esecuzione richiesta");

        // controlla che il campo host sia presente e che questo sia effettivamente
        // previsto
        HashMap<String, String> headerRichiesta = estraiHeader(richiesta);
        String host = headerRichiesta.get("host");
        if (host == null || (!hostMapping.containsKey(host))) {
            badRequest();
            return;
        }

        String metodo = estraiMetodo(richiesta);

        switch (metodo) {
            case "GET":
                get();
                break;
            case "POST":
                post();
                break;
            case "HEAD":
                head();
                break;
            case "PUT":
                put();
            case "DELETE":
                delete();
            case "PATCH":
                patch();
            case "TRACE":
                trace();
            case "OPTIONS":
                options();
            case "CONNECT":
                connect();
                break;
            default:
                badRequest();
                break;
        }

        log("richiesta eseguita");
    }

    protected void get() {
        notImplemented();
    }

    protected void post() {
        notImplemented();
    }

    protected void head() {
        notImplemented();
    }

    protected void put() {
        notImplemented();
    }

    protected void delete() {
        notImplemented();
    }

    protected void patch() {
        notImplemented();
    }

    protected void trace() {
        notImplemented();
    }

    protected void options() {
        notImplemented();
    }

    protected void connect() {
        notImplemented();
    }

    abstract protected void badRequest();

    abstract protected void internalError();

    abstract protected void notImplemented();

    protected Map<String, String> headerBase() {
        return new HashMap<String, String>() {
            {
                put("server", server);
            }
        };

    }

    protected final static String estraiMetodo(String richiesta) {
        String[] righe = richiesta.split("\n");
        String metodo = righe[0].split(" ")[0];

        return metodo;
    }

    protected final static String estraiRisorsaRichiesta(String richiesta) {
        String[] righe = richiesta.split("\n");
        String metodo = righe[0].split(" ")[1];

        return metodo;
    }

    protected final static String estraiVersioneHttp(String richiesta) {
        String[] righe = richiesta.split("\n");
        String metodo = righe[0].split(" ")[2];

        return metodo;
    }

    protected final static HashMap<String, String> estraiHeader(String richiesta) {
        HashMap<String, String> header = new HashMap<String, String>();

        String[] righe = richiesta.split("\n");
        boolean fineHeader = false;
        for (int i = 1; i < righe.length && (!fineHeader); i++) {
            String riga = righe[i];
            String[] splitRiga = riga.split(": ", 2); // Split sulla riga su ': ' con limite 2 per evitare di includere
                                                      // eventuali caratteri : presenti nel valore

            String nome = splitRiga[0];
            String valore = splitRiga[1];

            header.put(nome.toLowerCase(), valore);
            // Controllo di essere arrivati alla fine dell'header
            // Primo if controlla se non siamo già arrivati alla fine dell'array, altrimenti
            // righe[i + 1] andrebbe fuori dall'array
            if ((i + 1) != righe.length) {
                if (righe[i + 1] == CRLF) {
                    fineHeader = true;
                }
            }
        }

        return header;
    }

    protected final static String estraiBody(String richiesta) {
        String[] righe = richiesta.split(CRLF, 2);

        String body = righe[1];

        return body;
    }

    protected final static String costruisciStato(String versione, Codici stato) {
        return versione + " " + stato.ottieniCodice() + " " + stato.ottieniMessaggio();
    }

    protected final static String costruisciHeader(HashMap<String, String> campi) {
        String header = "";

        for (String campo : campi.keySet()) {
            String valore = campi.get(campo);
            header += campo + ": " + valore + "\n";
        }

        return header;
    }

    protected final static String costruisciRisposta(String stato, HashMap<String, String> campiHeader, String body) {
        return stato + "\n" + costruisciHeader(campiHeader) + CRLF + body;
    }
}