import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import java.io.File;
import java.io.IOException;

public class HttpI extends Http {
    public HttpI(String richiesta, Socket connessione, String server, int id, int log, Map<String, File> hostMapping) {
        super(richiesta, connessione, server, hostMapping, id, log);
    }

    private void erroreDefault(Codici codice) {
        String stato = costruisciStato(VERSIONE, codice);
        byte[] statoByte = stato.getBytes();

        HashMap<String, String> campiHeader = (HashMap<String, String>) headerBase();
        campiHeader.put("content-type", CONTENT_TYPE_HTML);
        String header = costruisciHeader(campiHeader);
        byte[] headerByte = header.getBytes();

        String body = "<h1 align='center'>" + codice.ottieniMessaggio().toUpperCase() + "</h1>";
        byte[] bodyByte = body.getBytes();

        mandaRisposta(statoByte, headerByte, bodyByte);
        chiudi();
    }

    protected void notFound() {
        erroreDefault(Codici.HTTP_NOT_FOUND);
    }

    protected void internalError() {
        erroreDefault(Codici.HTTP_INTERNAL_ERROR);
    }

    protected void badRequest() {
        erroreDefault(Codici.HTTP_BAD_REQUEST);
    }

    protected void notImplemented() {
        erroreDefault(Codici.HTTP_NOT_IMPLEMENTED);
    }

    protected void unsupportedMediaType() {
        erroreDefault(Codici.HTTP_UNSUPPORTED_MEDIA_TYPE);
    }

    private static boolean mimeCompatibile(String m1, String m2) throws MimeTypeParseException {
        MimeType mime1 = new MimeType(m1);
        MimeType mime2 = new MimeType(m2);

        return mime1.match(mime2);
    }

    // Ritorna true se il mime m1 è presente in listaMime
    private static boolean mimeCompatibili(String m1, String[] listaMime) throws MimeTypeParseException {
        for (String mime : listaMime) {
            if (mimeCompatibile(m1, mime)) {
                return true;
            }
        }

        return false;
    }

    // Calcola la risorsa da mandare
    // ritorna null in caso di errore
    private File calcolaRisorsa() {
        // Calcolo percorso finale risorsa e controllo che essa esista
        String risorsaRichiesta = estraiRisorsaRichiesta(richiesta);

        HashMap<String, String> headerRichiesta = estraiHeader(richiesta);
        String host = headerRichiesta.get("host");
        if (host == null) {
            log("campo host non presente nell'header della richiesta");
            badRequest();
            return null;
        }

        File rootDir = hostMapping.get(host);
        File risorsa = new File(rootDir.getAbsolutePath() + risorsaRichiesta);

        log("Richiesta risorsa " + risorsa.getAbsolutePath(), 2);

        if (!risorsa.exists()) {
            log("risorsa richiesta non esistente");
            notFound();
            return null;
        }

        return risorsa;
    }

    // Determina MIME type risorsa e se esso coincide con quello richiesto
    // ritorna null se si sono verificati degli errori
    private String calcolaMime(File risorsa) {
        String mimeTypeRisposta = URLConnection.guessContentTypeFromName(risorsa.getName());
        HashMap<String, String> headerRichiesta = estraiHeader(richiesta);
        String mimeTypeRichiesti = headerRichiesta.get("accept");

        if (!mimeTypeRichiesti.contains("*/*")) {
            String[] mimeTypeRichiestiArray = mimeTypeRichiesti.split(",");

            boolean mimeCompatibile = false;
            try {
                mimeCompatibile = mimeCompatibili(mimeTypeRisposta, mimeTypeRichiestiArray);
            } catch (MimeTypeParseException e) {
                log("Errore durante controllo mime:\n" + e.getMessage());
                internalError();
                return null;
            }

            if (!mimeCompatibile) {
                log("Mime type non compatibile");
                unsupportedMediaType();
                return null;
            }
        }

        return mimeTypeRisposta;
    }

    private byte[] leggiRisorsa(File risorsa) {
        byte[] rawBody = null;
        try {
            rawBody = Files.readAllBytes(risorsa.toPath());
        } catch (IOException e) {
            log("errore durante la lettura della risorsa richiesta:\n" + e.toString());
            internalError();
            return null;
        }

        return rawBody;
    }

    private void get(boolean head) {
        // Calcolo risorsa
        File risorsa = null;
        if ((risorsa = calcolaRisorsa()) == null)
            return;

        // Calcolo mime type
        String mimeTypeRisposta = null;
        if ((mimeTypeRisposta = calcolaMime(risorsa)) == null)
            return;

        // Leggi la risorsa come byte
        byte[] rawBody = null;
        if ((rawBody = leggiRisorsa(risorsa)) == null)
            return;

        // Creazione riga di stato
        String statoRisposta = costruisciStato(VERSIONE, Codici.HTTP_OK);
        byte[] statoRispostaByte = statoRisposta.getBytes();

        // Creazione header
        HashMap<String, String> headerRisposta = (HashMap<String, String>) headerBase();
        headerRisposta.put("content-type", mimeTypeRisposta);
        headerRisposta.put("content-length", String.valueOf(rawBody.length));
        String headerRispostaString = costruisciHeader(headerRisposta);
        byte[] headerRispostaByte = headerRispostaString.getBytes();

        if (head)
            rawBody = null;

        mandaRisposta(statoRispostaByte, headerRispostaByte, rawBody);
        chiudi();
    }

    protected void get() {
        get(false);
    }

    protected void head() {
        get(true);
    }
}