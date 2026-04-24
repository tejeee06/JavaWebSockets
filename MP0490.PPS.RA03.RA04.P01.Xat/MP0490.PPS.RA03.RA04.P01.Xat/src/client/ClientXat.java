package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * CLIENT DE CHAT
 * ===============
 * Aquest programa es connecta al servidor de chat.
 * Usa dos fils:
 *   - El fil principal llegeix el que l'usuari escriu per teclat i ho envia
 *   - Un fil secundari escolta els missatges que arriben del servidor
 *
 * Per provar: arrenca primer el servidor, després diversos clients.
 */
public class ClientXat {

    // Adreça i port del servidor (canvia "localhost" si el servidor és en una altra màquina)
    static final String ADRECA_SERVIDOR = "localhost";
    static final int PORT = 12345;

    public static void main(String[] args) {

        System.out.println("=== CLIENT DE CHAT ===");
        System.out.println("Connectant a " + ADRECA_SERVIDOR + ":" + PORT + " ...");

        try (Socket socket = new Socket(ADRECA_SERVIDOR, PORT)) {

            System.out.println("Connexió establerta!");

            // Fluxos de comunicació
            BufferedReader entradaServidor = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter sortidaServidor = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader teclat = new BufferedReader(new InputStreamReader(System.in));

            // Fil receptor: escolta el servidor en paral·lel
            // Usem un fil "daemon" per que s'aturi quan acabi el main
            Thread receptor = new Thread(() -> {
                try {
                    String missatgeServidor;
                    // Llegim fins que el servidor tanqui la connexió (readLine retorna null)
                    while ((missatgeServidor = entradaServidor.readLine()) != null) {
                        System.out.println(missatgeServidor);
                    }
                } catch (IOException e) {
                    System.out.println("[INFO] Connexió amb el servidor tancada.");
                }
            });
            receptor.setDaemon(true); // s'atura sol quan el main acabi
            receptor.start();

            // Fil principal: llegim del teclat i enviem al servidor
            String linia;
            while ((linia = teclat.readLine()) != null) {
                sortidaServidor.println(linia);

                if (linia.equalsIgnoreCase("sortir")) {
                    break;
                }
            }

            System.out.println("Desconnectant...");

        } catch (UnknownHostException e) {
            System.err.println("[ERROR] No es troba el servidor: " + ADRECA_SERVIDOR);
        } catch (IOException e) {
            System.err.println("[ERROR] Connexió fallida: " + e.getMessage());
            System.err.println("Comprova que el servidor està en marxa.");
        }
    }
}
